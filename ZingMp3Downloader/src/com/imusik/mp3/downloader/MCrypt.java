package com.imusik.mp3.downloader;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

public class MCrypt {
	private IvParameterSpec mIvspec;
	private String mSecretKey;
	private SecretKeySpec mKeyspec;
	private Cipher cipher;
	
	public MCrypt(String iv, String secretKey) {
		mIvspec = new IvParameterSpec(iv.getBytes());
		mSecretKey = secretKey;
		mKeyspec = new SecretKeySpec(mSecretKey.getBytes(), "AES");
		
		try {
			cipher = Cipher.getInstance("AES/CBC/NoPadding");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public MCrypt(byte[] ivBytes, String secretKey) {
		mIvspec = new IvParameterSpec(ivBytes);
		mSecretKey = secretKey;
		mKeyspec = new SecretKeySpec(mSecretKey.getBytes(), "AES");
		
		try {
			cipher = Cipher.getInstance("AES/CBC/NoPadding");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	/**
	 * Set iv bytes array
	 * @param ivBytes
	 */
	public void setIvBytes(byte[] ivBytes) {
		mIvspec = new IvParameterSpec(ivBytes);
	}

	/**
	 * Encrypt string to byte array
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public byte[] encryptToByteArray(String text) throws Exception {
		if (text == null || text.length() == 0)
			throw new Exception("Empty string");

		byte[] encrypted = null;

		try {
			cipher.init(Cipher.ENCRYPT_MODE, mKeyspec, mIvspec);
			encrypted = cipher.doFinal(padString(text).getBytes());
		} catch (Exception e) {
			throw new Exception("[encrypt] " + e.getMessage());
		}
		return encrypted;
	}

	/**
	 * Encrypt string to base 64 string
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public String encryptToBase64String(String text) throws Exception {
		return Base64.encodeToString(encryptToByteArray(text), Base64.DEFAULT);
	}	
	
	public String encryptToHexString(String text) throws Exception {
		return bytesToHex(encryptToByteArray(text));
	}	
	
	
	/**
	 * Decrypt from byte array
	 * @param encrypted 
	 * @return
	 * @throws Exception
	 */
	public byte[] decryptFromByteArray(byte[] encrypted) throws Exception {
		byte[] decrypted = null;
		try {
			cipher.init(Cipher.DECRYPT_MODE, mKeyspec, mIvspec);
			decrypted = cipher.doFinal(encrypted);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("[decrypt] " + e.getMessage());
		}
		return decrypted;
	}
	
	/**
	 * Decrypt from hex string
	 * @param code
	 * @return
	 * @throws Exception
	 */
	public String decryptFromHexString(String code) throws Exception {
		return new String(decryptFromByteArray(hexToBytes(code))).trim();
	}	
	
	public String decryptFromBase64String(String code) throws Exception {
		return new String(decryptFromByteArray(Base64.decode(code, Base64.DEFAULT))).trim();
	}	

	/**
	 * Convert hex string to byte array
	 * @param str
	 * @return
	 */
	public static byte[] hexToBytes(String str) {
		if (str == null) {
			return null;
		} else if (str.length() < 2) {
			return null;
		} else {
			int len = str.length() / 2;
			byte[] buffer = new byte[len];
			for (int i = 0; i < len; i++) {
				buffer[i] = (byte) Integer.parseInt(str.substring(i * 2, i * 2 + 2), 16);
			}
			return buffer;
		}
	}
	
	/**
	 * Convert byte array to hex string
	 * @param data
	 * @return
	 */
	public static String bytesToHex(byte[] data) {
		if (data == null) {
			return null;
		}

		int len = data.length;
		String str = "";
		for (int i = 0; i < len; i++) {
			if ((data[i] & 0xFF) < 16)
				str = str + "0" + java.lang.Integer.toHexString(data[i] & 0xFF);
			else
				str = str + java.lang.Integer.toHexString(data[i] & 0xFF);
		}
		return str;
	}	

	/**
	 * Add padding to input (must trim decrypted to remove padding)
	 * @param source
	 * @return
	 */
	private static String padString(String source) {
		char paddingChar = ' ';
		int size = 16;
		int x = source.length() % size;
		int padLength = size - x;

		for (int i = 0; i < padLength; i++) {
			source += paddingChar;
		}
		return source;
	}
}
