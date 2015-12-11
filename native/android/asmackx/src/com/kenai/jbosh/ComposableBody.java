/*
 * Copyright 2009 Mike Cumings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kenai.jbosh;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;

/**
 * Implementation of the {@code AbstractBody} class which allows for the
 * definition of  messages from individual elements of a body.
 * <p/>
 * A message is constructed by creating a builder, manipulating the
 * configuration of the builder, and then building it into a class instance,
 * as in the following example:
 * <pre>
 * ComposableBody body = ComposableBody.builder()
 *     .setNamespaceDefinition("foo", "http://foo.com/bar")
 *     .setPayloadXML("<foo:data>Data to send to remote server</foo:data>")
 *     .build();
 * </pre>
 * Class instances can also be "rebuilt", allowing them to be used as templates
 * when building many similar messages:
 * <pre>
 * ComposableBody body2 = body.rebuild()
 *     .setPayloadXML("<foo:data>More data to send</foo:data>")
 *     .build();
 * </pre>
 * This class does only minimal syntactic and semantic checking with respect
 * to what the generated XML will look like.  It is up to the developer to
 * protect against the definition of malformed XML messages when building
 * instances of this class.
 * <p/>
 * Instances of this class are immutable and thread-safe.
 */
public final class ComposableBody extends AbstractBody {

    private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory factory = TransformerFactory.newInstance();

    /**
     * Map of all attributes to their values.
     */
    private final Map<BodyQName, String> attrs;

    /**
     * Payload XML.
     */
    private final String payload;

    /**
     * Computed raw XML.
     */
    private final AtomicReference<String> computed =
            new AtomicReference<String>();

    /**
     * Class instance builder, after the builder pattern.  This allows each
     * message instance to be immutable while providing flexibility when
     * building new messages.
     * <p/>
     * Instances of this class are <b>not</b> thread-safe.
     */
    public static final class Builder {
        private Map<BodyQName, String> map;
        private boolean doMapCopy;
        private String payloadXML;

        /**
         * Prevent direct construction.
         */
        private Builder() {
            // Empty
        }

        /**
         * Creates a builder which is initialized to the values of the
         * provided {@code ComposableBody} instance.  This allows an
         * existing {@code ComposableBody} to be used as a
         * template/starting point.
         *
         * @param source body template
         * @return builder instance
         */
        private static Builder fromBody(final ComposableBody source) {
            Builder result = new Builder();
            result.map = source.getAttributes();
            result.doMapCopy = true;
            result.payloadXML = source.payload;
            return result;
        }

        /**
         * Set the body message's wrapped payload content.  Any previous
         * content will be replaced.
         *
         * @param xml payload XML content
         * @return builder instance
         */
        public Builder setPayloadXML(final String xml) {
            if (xml == null) {
                throw(new IllegalArgumentException(
                        "payload XML argument cannot be null"));
            }
            payloadXML = xml;
            return this;
        }

        /**
         * Set an attribute on the message body / wrapper element.
         *
         * @param name qualified name of the attribute
         * @param value value of the attribute
         * @return builder instance
         */
        public Builder setAttribute(
                final BodyQName name, final String value) {
            if (map == null) {
                map = new HashMap<BodyQName, String>();
            } else if (doMapCopy) {
                map = new HashMap<BodyQName, String>(map);
                doMapCopy = false;
            }
            if (value == null) {
                map.remove(name);
            } else {
                map.put(name, value);
            }
            return this;
        }

        /**
         * Convenience method to set a namespace definition. This would result
         * in a namespace prefix definition similar to:
         * {@code <body xmlns:prefix="uri"/>}
         *
         * @param prefix prefix to define
         * @param uri namespace URI to associate with the prefix
         * @return builder instance
         */
        public Builder setNamespaceDefinition(
                final String prefix, final String uri) {
            BodyQName qname = BodyQName.createWithPrefix(
                    XMLConstants.XML_NS_URI, prefix,
                    XMLConstants.XMLNS_ATTRIBUTE);
            return setAttribute(qname, uri);
        }

        /**
         * Build the immutable object instance with the current configuration.
         *
         * @return composable body instance
         */
        public ComposableBody build() {
            if (map == null) {
                map = new HashMap<BodyQName, String>();
            }
            if (payloadXML == null) {
                payloadXML = "";
            }
            return new ComposableBody(map, payloadXML);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Constructors:

    /**
     * Prevent direct construction.  This constructor is for body messages
     * which are dynamically assembled.
     */
    private ComposableBody(
            final Map<BodyQName, String> attrMap,
            final String payloadXML) {
        super();
        attrs = attrMap;
        payload = payloadXML;
    }

    /**
     * Parse a static body instance into a composable instance.  This is an
     * expensive operation and should not be used lightly.
     * <p/>
     * The current implementation does not obtain the payload XML by means of
     * a proper XML parser.  It uses some string pattern searching to find the
     * first @{code body} element and the last element's closing tag.  It is
     * assumed that the static body's XML is well formed, etc..  This
     * implementation may change in the future.
     *
     * @param body static body instance to convert
     * @return composable bosy instance
     * @throws BOSHException
     */
    static ComposableBody fromStaticBody(final StaticBody body)
    throws BOSHException {
        String raw = body.toXML();

        Document doc = null;
        Node bodyNode = null;

        DocumentBuilder db = null;
        try {
            db = docBuilderFactory.newDocumentBuilder();

            InputSource is =  new InputSource();
            is.setCharacterStream(new StringReader(raw));

            doc = db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new BOSHException("Invalid xml", e);
        } catch (SAXException e) {
            throw new BOSHException("Invalid xml", e);
        } catch (IOException e) {
            throw new BOSHException("Invalid xml", e);
        }

        if (doc == null) {
            throw new BOSHException("Invalid xml");
        }

        NodeList rootNodes = doc.getChildNodes();
        if (rootNodes.getLength() == 1) {
            bodyNode = doc.getFirstChild();
        } else {
            boolean foundElement = false;
            for (int i = 0; i < rootNodes.getLength(); i++) {

                Node node = rootNodes.item(i);
                int nodeType = node.getNodeType();
                if (nodeType == Node.ELEMENT_NODE) {
                    if (foundElement) {
                        throw new BOSHException("Expected exactly one content node");
                    }

                    bodyNode = node;
                    foundElement = true;
                } else if (nodeType == Node.COMMENT_NODE) {
                    // ok, we'll skip comments
                } else {
                    throw new BOSHException("Invalid xml, expected exactly one content node");
                }
            }
        }

        if (bodyNode == null) {
            throw new BOSHException("Invalid xml, could not get body node");
        }

        // getLocalName is returning null
        String nodeName = bodyNode.getNodeName();
        if (nodeName.contains(":")) {
            nodeName = nodeName.substring(nodeName.indexOf(":") + 1);
        }

        if (!nodeName.equals("body")) {
            throw new BOSHException("Expected body element, found: " + bodyNode.getNodeName());
        }

        // get the dom representation of body

        String payload = "";

        try {
            Transformer transformer = factory.newTransformer();

            Properties properties = new Properties();
            properties.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            transformer.setOutputProperties(properties);

            final StringBuilder sb = new StringBuilder();


            NodeList childNodes = bodyNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);

                int nodeType = child.getNodeType();

                javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(child);
                javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(new OutputStream() {

                    @Override
                    public void write(int i) throws IOException {
                        sb.append((char)i);
                    }
                });

                transformer.transform(domSource, result);
            }


            payload = sb.toString();

        } catch (TransformerConfigurationException e) {
            throw new BOSHException("Invalid xml", e);
        } catch (TransformerException e) {
            throw new BOSHException("Invalid xml", e);
        }

        return new ComposableBody(body.getAttributes(), payload);
    }

    /**
     * Create a builder instance to build new instances of this class.
     *
     * @return AbstractBody instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * If this {@code ComposableBody} instance is a dynamic instance, uses this
     * {@code ComposableBody} instance as a starting point, create a builder
     * which can be used to create another {@code ComposableBody} instance
     * based on this one. This allows a {@code ComposableBody} instance to be
     * used as a template.  Note that the use of the returned builder in no
     * way modifies or manipulates the current {@code ComposableBody} instance.
     *
     * @return builder instance which can be used to build similar
     *  {@code ComposableBody} instances
     */
    public Builder rebuild() {
        return Builder.fromBody(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Accessors:
    
    /**
     * {@inheritDoc}
     */
    public Map<BodyQName, String> getAttributes() {
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * {@inheritDoc}
     */
    public String toXML() {
        String comp = computed.get();
        if (comp == null) {
            comp = computeXML();
            computed.set(comp);
        }
        return comp;
    }

    /**
     * Get the paylaod XML in String form.
     *
     * @return payload XML
     */
    public String getPayloadXML() {
        return payload;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods:

    /**
     * Escape the value of an attribute to ensure we maintain valid
     * XML syntax.
     *
     * @param value value to escape
     * @return escaped value
     */
    private String escape(final String value) {
        return value.replace("'", "&apos;");
    }

    /**
     * Generate a String representation of the message body.
     *
     * @return XML string representation of the body
     */
    private String computeXML() {
        BodyQName bodyName = getBodyQName();
        StringBuilder builder = new StringBuilder();
        builder.append("<");
        builder.append(bodyName.getLocalPart());
        for (Map.Entry<BodyQName, String> entry : attrs.entrySet()) {
            builder.append(" ");
            BodyQName name = entry.getKey();
            String prefix = name.getPrefix();
            if (prefix != null && prefix.length() > 0) {
                builder.append(prefix);
                builder.append(":");
            }
            builder.append(name.getLocalPart());
            builder.append("='");
            builder.append(escape(entry.getValue()));
            builder.append("'");
        }
        builder.append(" ");
        builder.append(XMLConstants.XMLNS_ATTRIBUTE);
        builder.append("='");
        builder.append(bodyName.getNamespaceURI());
        builder.append("'>");
        if (payload != null) {
            builder.append(payload);
        }
        builder.append("</body>");
        return builder.toString();
    }

}
