package de.androidcrypto.mifare_ultralight_c_examples;

import static de.androidcrypto.mifare_ultralight_c_examples.Utils.bytesToHexNpe;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.combineByteArrays;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.hexStringToByteArray;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.intFrom2ByteArrayInversed;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.printData;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.reverseByteArray;

import android.nfc.tech.NfcA;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * This class holds all methods to work with a MIFARE Ultralight C NFC tag.
 * Note: the class is using the NfcA class to communicate with the tag and
 * not the Ultralight class that is not available on all Android devices.
 * Please do not use this class to work with MIFARE Ultralight EV1 tags as
 * this class uses specific and predefined data for Ultralight C tags.
 */

public class MIFARE_Ultralight_C {
    private static final String TAG = "MFULC";
    public static final String version = "1.00";
    private static final int pagesToRead = 48;
    // acknoledge bytes
    public static final byte ack = 0x0A;
    public static final byte nack_eeprom_write = 0x02;
    public static final byte nack_parity_crc = 0x01;
    public static final byte nack_other = 0x00;
    // Remark: Any 4-bit response different from Ah shall be interpreted as NAK

    public static final byte[] mifareULCDefaultKey = {(byte) 0x49, (byte) 0x45,
            (byte) 0x4D, (byte) 0x4B, (byte) 0x41, (byte) 0x45,
            (byte) 0x52, (byte) 0x42, (byte) 0x21, (byte) 0x4E,
            (byte) 0x41, (byte) 0x43, (byte) 0x55, (byte) 0x4F,
            (byte) 0x59, (byte) 0x46}; // IEMKAERB!NACUOYF

    public static final byte[] defaultAuthKey = hexStringToByteArray("49454D4B41455242214E4143554F5946"); // "IEMKAERB!NACUOYF" => "BREAKMEIFYOUCAN!", 16 bytes long
    public static final byte[] customAuthKey = "1234567890123456".getBytes(StandardCharsets.UTF_8);
    /**
     * auth code taken from  https://stackoverflow.com/a/44640515/8166854
     * Note: this is a modified code without Apache Commons Lang.
     */
    public static boolean authenticateUltralightC(NfcA nfcA, byte[] key) {
        Log.d(TAG, "AUTHENTICATE Ultralight C");
        try {
            byte[] encRndB = null;
            encRndB = nfcA.transceive((new byte[]{0x1A, 0x00}));
            if ((encRndB.length != 9) || (encRndB[0] != (byte) 0xAF)) {
                Log.e(TAG, "RuntimeException(Invalid response!)");
                return false;
            }
            encRndB = Arrays.copyOfRange(encRndB, 1, 9);
            Log.d(TAG, " - EncRndB: " + bytesToHexNpe(encRndB));
            byte[] rndB = desDecrypt(key, encRndB);
            Log.d(TAG, " - RndB: " + bytesToHexNpe(rndB));
            byte[] rndBrot = rotateLeft(rndB);
            Log.d(TAG, " - RndBrot: " + bytesToHexNpe(rndBrot));
            byte[] rndA = new byte[8];
            generateRandom(rndA);
            Log.d(TAG, " - RndA: " + bytesToHexNpe(rndA));
            //byte[] encRndArotPrime = transmitRaw(ArrayUtils.addAll(new byte[] {(byte) 0xAF}, desEncrypt(key, ArrayUtils.addAll(rndA, rndBrot))));
            //byte[] rndARndBrot = ArrayUtils.addAll(rndA, rndBrot);
            byte[] rndARndBrot = combineByteArrays(rndA, rndBrot);
            Log.d(TAG, printData("rndARndBrot", rndARndBrot));
            byte[] encRndARndBrot = desEncrypt(key, rndARndBrot);
            Log.d(TAG, printData("encRndARndBrot", encRndARndBrot));
            byte[] dataToSend = combineByteArrays(new byte[]{(byte) 0xAF}, encRndARndBrot);
            Log.d(TAG, printData("dataToSend", dataToSend));
            // AF0A638559FC7737F9F15D7862EBBE967A
            byte[] encRndArotPrime;
            try {
                encRndArotPrime = nfcA.transceive(dataToSend);
            } catch (IOException e) {
                Log.e(TAG, "IOEx on second auth round");
                return false;
            }
            Log.d(TAG, "encRndArotPrime: " + bytesToHexNpe(encRndArotPrime));
            if ((encRndArotPrime.length != 9) || (encRndArotPrime[0] != 0x00)) {
                Log.d(TAG, "RuntimeException (Invalid response!)");
                return false;
            }
            encRndArotPrime = Arrays.copyOfRange(encRndArotPrime, 1, 9);
            Log.d(TAG, " - EncRndArot': " + bytesToHexNpe(encRndArotPrime));
            byte[] rndArotPrime = desDecrypt(key, encRndArotPrime);
            Log.d(TAG, " - RndArot': " + bytesToHexNpe(rndArotPrime));
            if (!Arrays.equals(rotateLeft(rndA), rndArotPrime)) {
                Log.d(TAG, "RuntimeException (Card authentication failed)");
                return false;
            } else {
                Log.d(TAG, "Card authentication success");
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // runs a complete authentication session with the default 3DES key
    public static boolean doAuthenticateUltralightCDefault(NfcA nfcA) {
        Log.d(TAG, printData("mifareULCDefaultKey", defaultAuthKey));
        Log.d(TAG, "mifareULCDefaultKey:" + new String(defaultAuthKey, StandardCharsets.UTF_8) + "###");
        boolean authSuccess = false;
        try {
            authSuccess = authenticateUltralightC(nfcA, defaultAuthKey);
            return authSuccess;
        } catch (Exception e) {
            Log.e(TAG, "doAuthenticateUltralightCDefault Exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * This allows to read the complete memory = all pages of the tag. If a page is not readable the
     * method returns NULL.
     * Note: some pages are not readable by design (e.g. password).
     *
     * @param nfcA
     * @param page
     * @return
     */
    public static byte[] readPageMifareUltralight(NfcA nfcA, int page) {
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x30,           // READ a page is 4 bytes long
                    (byte) (page & 0x0ff)  // page address
            });
            return response;
        } catch (IOException e) {
            Log.d(TAG, "on page " + page + " readPage failed with IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        /*
        try {
            nfcA.close();
        } catch (Exception e) {
        }
        try {
            nfcA.connect();
        } catch (Exception e) {
        }

         */
        return null;
    }

    public static boolean writePageMifareUltralightC(NfcA nfcA, int page, byte[] data4Byte) {
        return writePageMifareUltralightC(nfcA, page, data4Byte, false);
    }

    /**
     * This allows to write just to the free user memory of the tag but not to any internal or configuration data
     * to prevent any damage or writing of OTP data.
     * If you need to write to these pages you need to use the dedicated methods instead.
     *
     * @param nfcA
     * @param page
     * @param data4Byte
     * @param noPageCheck if true all pages can be written
     * @return
     */
    public static boolean writePageMifareUltralightC(NfcA nfcA, int page, byte[] data4Byte, boolean noPageCheck) {
        if (data4Byte == null) {
            Log.d(TAG, "writePage data is NULL, aborted");
            return false;
        }
        if (data4Byte.length != 4) {
            Log.d(TAG, "writePage data is not exact 4 bytes long, aborted");
            return false;
        }
        if (page < 2) {
            // serial number
            Log.d(TAG, "writePage page is < 2, aborted");
            return false;
        }
        // this is to prevent for writing to OTP area
        if ((page >= 2) && (page < 4) && (noPageCheck == false)) {
            // Lock & OTP area
            Log.d(TAG, "writePage page is < 2, aborted");
            return false;
        }
        // the following data depends on the tag type
        if (page > pagesToRead) {
            Log.d(TAG, "writePage page is > " + (pagesToRead - 1) + ", aborted");
            return false;
        }
        // this is to prevent for writing to configuration or counter pages (and additional lock bytes on Ultralight C
        if ((page >= 40) && (noPageCheck == false)) {
            Log.d(TAG, "writePage page is >= 40 = configuration area, aborted");
            return false;
        }

        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0xA2,           // WRITE to a page is 4 bytes long
                    (byte) (page & 0x0ff),  // page address
                    (byte) (data4Byte[0]),
                    (byte) (data4Byte[1]),
                    (byte) (data4Byte[2]),
                    (byte) (data4Byte[3])
            });
            return true;
        } catch (IOException e) {
            Log.e(TAG, "on page " + page + " readPage failed with IOException: " + e.getMessage());
        }
        return false;
    }

    /**
     * AUTH0 defines the page address from which the authentication is required. Valid address values
     * for byte AUTH0 are from 03h to 30h.
     * Setting AUTH0 to 30h effectively disables memory protection
     *
     * @param auth0
     * @return true on success and fals on failure
     */
    public static boolean writeAuth0UltralightC(NfcA nfcA, byte auth0) {
        if ((int) auth0 < 3) {
            Log.d(TAG, "writeAuth0UltralightC auth0 is < 3, aborted");
            return false;
        }
        if ((int) auth0 > 48) {
            Log.d(TAG, "writeAuth0UltralightC auth0 is > 30, aborted");
            return false;
        }
        byte[] data4Byte = new byte[4];
        data4Byte[0] = auth0;
        return writePageMifareUltralightC(nfcA, 42, data4Byte, true);
    }

    /**
     * AUTH1 defines if the authentication restriction is for write only or read and write access
     *
     * @param writeAccessRestrictedOnly
     * @return true on success and fals on failure
     */
    public static boolean writeAuth1UltralightC(NfcA nfcA, boolean writeAccessRestrictedOnly) {
        // if writeAccessRestrictedOnly == false the tag gets write and read restricted
        // if writeAccessRestrictedOnly == true the tag is only write restricted
        byte auth1 = 0; // default means write and read restricted
        if (writeAccessRestrictedOnly) auth1 = 1;
        byte[] data4Byte = new byte[4];
        data4Byte[0] = auth1;
        return writePageMifareUltralightC(nfcA, 43, data4Byte, true);
    }

    /**
     * Converts a password to the format demanded by the tag
     * @param password16bytes
     * @return
     */
    public static byte[] convertPassword(byte[] password16bytes) {
        if (password16bytes == null) {
            Log.d(TAG, "password16bytes is NULL, aborted");
            return null;
        }
        if (password16bytes.length != 16) {
            Log.d(TAG, "password16bytes is not 16 bytes long, aborted");
            return null;
        }
        // change the direction and position of elements
        // e.g. byte[] defaultAuthKey = hexStringToByteArray("49454D4B41455242214E4143554F5946"); // "IEMKAERB!NACUOYF" => "BREAKMEIFYOUCAN!", 16 bytes long
        // step 1: inverse the bytes
        byte[] passwordInversed = reverseByteArray(password16bytes);
        // step 2: reorg of the data
        byte[] passwordFinal = new byte[passwordInversed.length];
        System.arraycopy(passwordInversed, 8, passwordFinal, 0, 8);
        System.arraycopy(passwordInversed, 0, passwordFinal, 8, 8);
        return passwordFinal;
    }

    public static boolean writePasswordUltralightC(NfcA nfcA, byte[] password16bytes) {
        if (password16bytes == null) {
            Log.d(TAG, "password16bytes is NULL, aborted");
            return false;
        }
        if (password16bytes.length != 16) {
            Log.d(TAG, "password16bytes is not 16 bytes long, aborted");
            return false;
        }
        // change the direction and position of elements
        // e.g. byte[] defaultAuthKey = hexStringToByteArray("49454D4B41455242214E4143554F5946"); // "IEMKAERB!NACUOYF" => "BREAKMEIFYOUCAN!", 16 bytes long
        // step 1: inverse the bytes
        byte[] passwordInversed = reverseByteArray(password16bytes);
        // step 2: reorg of the data
        byte[] passwordFinal = new byte[passwordInversed.length];
        System.arraycopy(passwordInversed, 8, passwordFinal, 0, 8);
        System.arraycopy(passwordInversed, 0, passwordFinal, 8, 8);
        // step 3: write the data in 4 byte chunks
        boolean success;
        //success = false;
        byte[] dataPage44 = Arrays.copyOfRange(passwordFinal, 0, 4);
        byte[] dataPage45 = Arrays.copyOfRange(passwordFinal, 4, 8);
        byte[] dataPage46 = Arrays.copyOfRange(passwordFinal, 8, 12);
        byte[] dataPage47 = Arrays.copyOfRange(passwordFinal, 12, 16);
        Log.e(TAG, printData("passwordFinal", passwordFinal));
        Log.e(TAG, printData("dataPage44", dataPage44));
        Log.e(TAG, printData("dataPage45", dataPage45));
        Log.e(TAG, printData("dataPage46", dataPage46));
        Log.e(TAG, printData("dataPage47", dataPage47));
        success = writePageMifareUltralightC(nfcA, 44, dataPage44, true);
        if (!success) {
            Log.e(TAG, "Error writing password step 1 page 44");
            return false;
        }
        success = writePageMifareUltralightC(nfcA, 45, dataPage45, true);
        if (!success) {
            Log.e(TAG, "Error writing password step 2 page 45");
            return false;
        }
        success = writePageMifareUltralightC(nfcA, 46, dataPage46, true);
        if (!success) {
            Log.e(TAG, "Error writing password step 3 page 46");
            return false;
        }
        success = writePageMifareUltralightC(nfcA, 47, dataPage47, true);
        if (!success) {
            Log.e(TAG, "Error writing password step 4 page 47");
            return false;
        }
        Log.e(TAG, "passwordOriginal: " + new String(password16bytes));
        Log.e(TAG, printData("passwordInversed", passwordInversed));
        Log.e(TAG, "passwordInversed: " + new String(passwordInversed));
        Log.e(TAG, "passwordInvFinal: " + "IEMKAERB!NACUOYF");
        Log.e(TAG, "passwordFinal   : " + new String(passwordFinal));
        return true;
    }

    public static int getCounterValue(NfcA nfcA) {
        // the counter is located in the first two bytes of page 29h, representing a 16 bit counter.
        // The counter starts with 0h and ends with 65535.
        byte[] pageData = readPageMifareUltralight(nfcA, 41);
        if (pageData == null) {
            Log.d(TAG, "getCounterValue returned NULL - the counter page is not readable");
            return -1;
        }
        // Although 2 last 2 bytes are set to 0 I'm using just 2 bytes for conversion
        return intFrom2ByteArrayInversed(Arrays.copyOf(pageData, 2));
    }

    public static boolean increaseCounterValueByOne(NfcA nfcA) {
        // the counter is located in the first two bytes of page 29h, representing a 16 bit counter.
        byte[] data4Byte = hexStringToByteArray("01000000");
        return writePageMifareUltralightC(nfcA, 41, data4Byte, true);
    }

    /**
     * Reads the complete content of the tag and returns an byte array with all pages
     * Pages that are not readable (e.g. read protected or by design) are filled with
     * 00h data.
     *
     * @param nfcA
     * @return
     */
    public static byte[] readCompleteContent(NfcA nfcA) {
        byte[][] pagesComplete = new byte[pagesToRead][]; // clear all data
        //boolean[] isPageReadable = new boolean[pagesToRead];
        byte[] memoryComplete = new byte[(pagesToRead) * 4];
        for (int i = 0; i < (pagesToRead - 4); i++) { // the last 4 pages are not readable
            pagesComplete[i] = readPageMifareUltralight(nfcA, i);
            if (pagesComplete[i] != null) {
                System.arraycopy(pagesComplete[i], 0, memoryComplete, i * 4, 4);
                //isPageReadable[i] = true;
            } else {
                System.arraycopy(new byte[16], 0, memoryComplete, i * 4, 4);
                //isPageReadable[i] = false;
            }
        }
        return memoryComplete;
    }

    /**
     * Reads the complete content of the tag and returns an array of pages
     * Pages that are not readable (e.g. read protected or by design) are NULL.
     *
     * @param nfcA
     * @return
     */
    public static byte[][] readCompleteContentPages(NfcA nfcA) {
        byte[][] pagesComplete = new byte[pagesToRead][]; // clear all data
        for (int i = 0; i < (pagesToRead - 4); i++) { // the last 4 pages are not readable
            pagesComplete[i] = readPageMifareUltralight(nfcA, i);
        }
        return pagesComplete;
    }

    // internal methods

    protected static SecureRandom rnd = new SecureRandom();

    protected static void generateRandom(byte[] rndA) {
        rnd.nextBytes(rndA);
    }

    protected static byte[] desEncrypt(byte[] key, byte[] data) {
        return performDes(Cipher.ENCRYPT_MODE, key, data);
    }

    protected static byte[] desDecrypt(byte[] key, byte[] data) {
        return performDes(Cipher.DECRYPT_MODE, key, data);
    }

    private static byte[] iv = new byte[8];

    protected static byte[] performDes(int opMode, byte[] key, byte[] data) {
        try {
            Cipher des = Cipher.getInstance("DESede/CBC/NoPadding");
            SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance("DESede");
            Key desKey = desKeyFactory.generateSecret(new DESedeKeySpec(combineByteArrays(key, Arrays.copyOf(key, 8))));
            des.init(opMode, desKey, new IvParameterSpec(iv));
            byte[] ret = des.doFinal(data);
            if (opMode == Cipher.ENCRYPT_MODE) {
                iv = Arrays.copyOfRange(ret, ret.length - 8, ret.length);
            } else {
                iv = Arrays.copyOfRange(data, data.length - 8, data.length);
            }
            return ret;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidKeySpecException |
                 IllegalBlockSizeException |
                 BadPaddingException |
                 InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] rotateLeft(byte[] in) {
        byte[] rotated = new byte[in.length];
        rotated[in.length - 1] = in[0];
        for (int i = 0; i < in.length - 1; i++) {
            rotated[i] = in[i + 1];
        }
        return rotated;
    }

}
