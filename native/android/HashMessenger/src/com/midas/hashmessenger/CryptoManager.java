package com.midas.hashmessenger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.util.Base64;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class CryptoManager {
	private static byte[] m_passwordBytes = null;

	public static String encrypt(String publicKeyStr, String cleartxt)
			throws InvalidKeyException, Exception {

		X509EncodedKeySpec spec = new X509EncodedKeySpec(
				Base64.decode(publicKeyStr));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey pubKey = kf.generatePublic(spec);

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		byte[] inputData = cleartxt.getBytes("utf-8");
		byte[] cipherData = cipher.doFinal(inputData);
		String encres = Base64.encodeBytes(cipherData);
		return encres;
	}

	public static String decrypt(String privateKeyStr, String base64str)
			throws InvalidKeyException, Exception {

		if (privateKeyStr == null) {
			throw new Exception("Private Key is null");
		}

		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(
				Base64.decode(privateKeyStr));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = kf.generatePrivate(spec);
		Cipher cipher = Cipher.getInstance("RSA");

		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] utf8 = cipher.doFinal(Base64.decode(base64str));
		String decstr = new String(utf8, "utf-8");
		return decstr;
	}

	private static byte[] getPasswordBytes() {
		return HistoryManager.getHistoryPassword();
	}

	private static byte[] getPasswordBytes(String password) {
		if (CryptoManager.m_passwordBytes == null) {
			try {
				byte[] keyBytes;
				keyBytes = password.getBytes("UTF-8");
				MessageDigest sha = MessageDigest.getInstance("SHA-1");

				keyBytes = sha.digest(keyBytes);
				keyBytes = Arrays.copyOf(keyBytes, 16); // 128 bit
				CryptoManager.m_passwordBytes = keyBytes;
			} catch (Exception e) {
				e.printStackTrace();
				CryptoManager.m_passwordBytes = null;
			}
		}
		return m_passwordBytes;
	}

	public static String passwordEncrypt(String cleartxt)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException,
			UnsupportedEncodingException, IllegalBlockSizeException,
			BadPaddingException {

		SecretKeySpec key = new SecretKeySpec(CryptoManager.getPasswordBytes(),
				"AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] inputData = cleartxt.getBytes("utf-8");
		byte[] cipherData = cipher.doFinal(inputData);
		String encres = Base64.encodeBytes(cipherData);
		return encres;
	}

	public static String passwordEncrypt(String password, String cleartxt)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException,
			UnsupportedEncodingException, IllegalBlockSizeException,
			BadPaddingException {

		SecretKeySpec key = new SecretKeySpec(
				CryptoManager.getPasswordBytes(password), "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] inputData = cleartxt.getBytes("utf-8");
		byte[] cipherData = cipher.doFinal(inputData);
		String encres = Base64.encodeBytes(cipherData);
		return encres;
	}

	public static String passwordEncrypt(byte[] password, String cleartxt)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException,
			UnsupportedEncodingException, IllegalBlockSizeException,
			BadPaddingException {

		SecretKeySpec key = new SecretKeySpec(password, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] inputData = cleartxt.getBytes("utf-8");
		byte[] cipherData = cipher.doFinal(inputData);
		String encres = Base64.encodeBytes(cipherData);
		return encres;
	}

	public static String passwordDecrypt(String password, String base64str)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException {

		SecretKeySpec key = new SecretKeySpec(
				CryptoManager.getPasswordBytes(password), "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		cipher.init(Cipher.DECRYPT_MODE, key);

		byte[] utf8 = cipher.doFinal(Base64.decode(base64str));
		String decstr = new String(utf8, "utf-8");
		return decstr;
	}

	public static String passwordDecrypt(byte[] password, String base64str)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException {

		SecretKeySpec key = new SecretKeySpec(password, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		cipher.init(Cipher.DECRYPT_MODE, key);

		byte[] utf8 = cipher.doFinal(Base64.decode(base64str));
		String decstr = new String(utf8, "utf-8");
		return decstr;
	}

	public static String passwordDecrypt(String base64str)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException,
			UnsupportedEncodingException {

		SecretKeySpec key = new SecretKeySpec(CryptoManager.getPasswordBytes(),
				"AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		cipher.init(Cipher.DECRYPT_MODE, key);

		byte[] utf8 = cipher.doFinal(Base64.decode(base64str));
		String decstr = new String(utf8, "utf-8");
		return decstr;
	}

	/*
	 * Generate a random string to be used as a password
	 */
	public static String randomPassword() {
		// generate a random password
		SecureRandom sr = new SecureRandom();
		return new BigInteger(130, sr).toString(32);
	}

	/**
	 * Base64 encode the SHA1 sum of the input string
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] sha1Base64(String input) {
		byte[] stringBytes;
		try {
			stringBytes = input.getBytes("UTF-8");
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			stringBytes = sha.digest(stringBytes);
			stringBytes = Arrays.copyOf(stringBytes, 16); // 128 bit
			return stringBytes;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return new byte[] {};
	}

	/**
	 * Base64 encode the SHA1 sum of the input string
	 * 
	 * @param input
	 * @return
	 */
	public static String sha1Base64String(String input) {
		byte[] stringBytes = CryptoManager.sha1Base64(input);
		if (stringBytes.length == 0) {
			return null;
		}
		return Base64.encodeBytes(stringBytes);
	}
}
