package de.androidcrypto.mifare_ultralight_c_examples;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {

    final String APP_TAG = "CryptoManager";
    private static final int PBKDF2_ITERATIONS = 10000;

    private static final String TRANSFORMATION_GCM = "AES/GCM/NoPadding";


    // https://www.techiedelight.com/concatenate-byte-arrays-in-java/
    public static byte[] concat(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (arrays != null) {
            Arrays.stream(arrays).filter(Objects::nonNull)
                    .forEach(array -> out.write(array, 0, array.length));
        }
        return out.toByteArray();
    }

    // generated ciphertext is 32 byte salt 12 byte iv xx byte ciphertext
    public static byte[] aes256GcmPbkdf2Sha256Encryption(byte[] plaintext, char[] passphrase) {
        byte[] ciphertext = new byte[0];
        // generate 32 byte random salt
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        SecretKeyFactory secretKeyFactory = null;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 32 * 8);
            byte[] secretKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            ciphertext = cipher.doFinal(plaintext);
            return concat(salt, cipher.getIV(), ciphertext);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    // return a byte[][], 0 = salt, 1 = nonce and 2 = ciphertext
    public static byte[][] aes256GcmPbkdf2Sha256Encryption2(byte[] plaintext, char[] passphrase) {
        byte[][] output = new byte[3][];
        byte[] ciphertext = new byte[0];
        // generate 32 byte random salt
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        // generate 12 byte random nonce
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        SecretKeyFactory secretKeyFactory = null;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 32 * 8);
            byte[] secretKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            ciphertext = cipher.doFinal(plaintext);
            //System.out.println("nonce length: " + cipher.getIV().length);
            output[0] = salt;
            output[1] = cipher.getIV();
            output[2] = ciphertext;
            return output;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return new byte[0][];
        }
    }

    public static byte[] aes256GcmPbkdf2Sha256Decryption2(byte[] salt, byte[] nonce, byte[] ciphertext, char[] passphrase) {
        byte[] plaintext = new byte[0];
        // todo sanity check for correct lengths of salt, nonce and minimum length ciphertext ?
        //System.out.println("salt l: " + salt.length + "d: " + bytesToHex(salt));
        //System.out.println("nonc l: " + nonce.length + "d: " + bytesToHex(nonce));
        //System.out.println("cite l: " + ciphertext.length + "d: " + bytesToHex(ciphertext));
        SecretKeyFactory secretKeyFactory = null;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 32 * 8);
            byte[] secretKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            return cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static byte[] aes256GcmPbkdf2Sha256Decryption(byte[] completeCiphertext, char[] passphrase) {
        byte[] plaintext = new byte[0];
        // get 32 bytes salt, 12 bytes IV and xx bytes from the completeCiphertext
        byte[] salt = new byte[32];
        byte[] nonce = new byte[12];
        int ciphertextLength = completeCiphertext.length - 32 - 12;
        byte[] ciphertext = new byte[(ciphertextLength)];
        salt = Arrays.copyOfRange(completeCiphertext, 0, 32);
        nonce = Arrays.copyOfRange(completeCiphertext, 32, 44);
        ciphertext = Arrays.copyOfRange(completeCiphertext, 44, completeCiphertext.length);
        //System.out.println("*** completeCiphertext length: " + completeCiphertext.length);
        //System.out.println("complete:" + bytesToHex(completeCiphertext));
        //System.out.println("salt l: " + salt.length + "d: " + bytesToHex(salt));
        //System.out.println("iv l: " + nonce.length + "d: " + bytesToHex(nonce));
        //System.out.println("cite l: " + ciphertext.length + "d: " + bytesToHex(ciphertext));
        SecretKeyFactory secretKeyFactory = null;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, 32 * 8);
            byte[] secretKey = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_GCM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            return cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static String base64Encoding(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }

    public static byte[] base64Decoding(String input) {
        return Base64.decode(input, Base64.NO_WRAP);
    }

    // https://stackoverflow.com/a/9670279/8166854
    byte[] charArrayToByteArray(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
                byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }
    /*
    Solution is inspired from Swing recommendation to store passwords in char[].
    usage:
    char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    byte[] bytes = toBytes(chars);
    // do something with chars/bytes
    Arrays.fill(chars, '\u0000'); // clear sensitive data
    Arrays.fill(bytes, (byte) 0); // clear sensitive data
     */

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}


