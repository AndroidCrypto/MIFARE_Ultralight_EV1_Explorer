package de.androidcrypto.mifare_ultralight_ev1_explorer;

import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.hexStringToByteArray;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.intFrom3ByteArrayInversed;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.printData;

import android.nfc.tech.NfcA;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

/**
 * This class holds all methods to work with a MIFARE Ultralight EV1 NFC tag.
 * Note: the class is using the NfcA class to communicate with the tag and
 * not the Ultralight class that is not available on all Android devices.
 * Please do not use this class to work with MIFARE Ultralight C tags as
 * this class uses specific and predefined data for Ultralight EV1 tags.
 */

public class MIFARE_Ultralight_EV1 {
    private static final String TAG = "MFULEV1";
    public static final String version = "1.00";
    private static final byte[] atqaUltralight = hexStringToByteArray("4400");
    private static final short sakUltralight = 0;
    public static int pagesToRead = 41; // MFOUL21 tag, can be changed to 20 in case of a MF0UL11 tag
    // acknowledge bytes
    public static final byte ack = 0x0A;
    // Remark: Any 4-bit response different from Ah shall be interpreted as NAK

    public static final byte[] defaultPassword = hexStringToByteArray("FFFFFFFF");
    public static final byte[] defaultPack = hexStringToByteArray("0000");
    public static final byte[] customPassword = hexStringToByteArray("12345678");
    public static final byte[] customPack = hexStringToByteArray("9876");

    public static boolean identifyUltralightFamily(NfcA nfcA) {
        // get card details
        byte[] atqa = nfcA.getAtqa();
        short sak = nfcA.getSak();
        if ((Arrays.equals(atqa, atqaUltralight)) && (sak == sakUltralight)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * The Ultralight EV1 tag has a command to read out the specification of the tag.
     * See datasheet for more details: page 20 ff
     * @param nfcA
     * @return Value is an 8 bytes long array, the three most important bytes are:
     * Product Type:          byte 2, 0x03h = MIFARE Ultralight
     * Major Product Version: byte 4, 0x01h = EV1
     * Storage size:          byte 6, 0x0Bh = MF0UL11 = 48 bytes
     *                                0x0Eh = MF0UL21 = 128 bytes
     */
    public static byte[] getVersion(NfcA nfcA) {
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x60  // Get version command
            });
            return response;
        } catch (IOException e) {
            Log.e(TAG, "Get Version command failed with IOException: " + e.getMessage());
        }
        return null;
    }


    public static int identifyUltralightEv1Tag (NfcA nfcA) {
        byte[] response = getVersion(nfcA);
        if (response == null) {
            Log.d(TAG, "Get Version responds with Null");
            return 0;
        }
        // check for bytes 2 (product type), 4 (major product version) and byte 6 (storage size)
        if ((response[2] != 0x03) || (response[4] != 0x01)) {
            Log.d(TAG, "Product Type or Major Product Version is not indicating an Ultralight EV1 tag");
            return 0;
        }
        if (response[6] == 0x0B) {
            pagesToRead = 20;
            return 48;
        } else if (response[6] == 0x0E) {
            pagesToRead = 41;
            return 128;
        } else {
            pagesToRead = 0;
            return 0; // unknown storage size
        }
    }

    /**
     * Authenticates the tag by the password (exact 4 bytes) and pack (exact 2 bytes)
     * @param nfcA
     * @param password4Bytes
     * @param pack2Bytes
     * @return 1 on success, 0..-6 is failure
     */
    public static int authenticateUltralightEv1(NfcA nfcA, byte[] password4Bytes, byte[] pack2Bytes) {
        if (password4Bytes == null) {
            Log.d(TAG, "password16Bytes is Null, aborted");
            return -1;
        }
        if (password4Bytes.length != 4) {
            Log.d(TAG, "password16Bytes length is not 4, aborted");
            return -2;
        }
        if (pack2Bytes == null) {
            Log.d(TAG, "pack2Bytes is Null, aborted");
            return -3;
        }
        if (pack2Bytes.length != 2) {
            Log.d(TAG, "pack2Bytes length is not 2, aborted");
            return -4;
        }
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x1B,
                    password4Bytes[0], password4Bytes[1], password4Bytes[2], password4Bytes[3]
            });
        } catch (IOException e) {
            Log.e(TAG, "authenticateUltralightEv1 command failed with IOException: " + e.getMessage());
            return -5;
        }
        Log.d(TAG, printData("authenticateUltralightEv1 response", response));
        if ((response != null) && (response.length >= 2)) {
            // success
            byte[] packReceived = Arrays.copyOf(response, 2);
            if (Arrays.equals(packReceived, pack2Bytes)) {
                // PACK verified, so tag is authentic (not really, but that whole
                // PWD_AUTH/PACK authentication mechanism was not really meant to
                // bring much security, I hope; same with the NTAG signature btw.)
                return 1;
            } else {
                return 0;
            }
        }
        return -6;
    }

    public static int writePasswordPackUltralightEv1(NfcA nfcA, byte[] password4Bytes, byte[] pack2Bytes) {
        if (password4Bytes == null) {
            Log.d(TAG, "password16Bytes is Null, aborted");
            return -1;
        }
        if (password4Bytes.length != 4) {
            Log.d(TAG, "password16Bytes length is not 4, aborted");
            return -2;
        }
        if (pack2Bytes == null) {
            Log.d(TAG, "pack2Bytes is Null, aborted");
            return -3;
        }
        if (pack2Bytes.length != 2) {
            Log.d(TAG, "pack2Bytes length is not 2, aborted");
            return -4;
        }
        byte[] response = null;
        byte pagePassword;
        if (pagesToRead == 20) {
            pagePassword = 0x12;
        } else if (pagesToRead == 41) {
            pagePassword = 0x27;
        } else {
            return -5;
        }
        // write the password
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0xA2,
                    pagePassword,
                    password4Bytes[0], password4Bytes[1], password4Bytes[2], password4Bytes[3]
            });
        } catch (IOException e) {
            Log.e(TAG, "writePasswordUltralightEv1 command failed with IOException: " + e.getMessage());
            return -6;
        }
        Log.d(TAG, printData("write password response", response));
        // write the pack
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0xA2,
                    (byte) (pagePassword + 1),
                    pack2Bytes[0], pack2Bytes[1], (byte)0x00, (byte)0x00
            });
        } catch (IOException e) {
            Log.e(TAG, "writePackUltralightEv1 command failed with IOException: " + e.getMessage());
            return -7;
        }
        Log.d(TAG, printData("write pack response", response));
        return 1; // success;
    }

    /**
     * This allows to read the complete memory = all pages of the tag. If a page is not readable the
     * method returns NULL.
     * Note: some pages are not readable by design (e.g. password).
     *
     * @param nfcA
     * @param page
     * @return The command returns 16 bytes (4 pages) with one command
     */
    public static byte[] readPageMifareUltralightEv1(NfcA nfcA, int page) {
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x30,           // READ a page is 4 bytes long
                    (byte) (page & 0x0ff)  // page address
            });
            if (response.length < 16) {
                return null;
            } else {
                return response;
            }
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

    /**
     * This allows to read the complete memory = all pages of the tag. If a page is not readable the
     * method returns NULL.
     * Note: some pages are not readable by design (e.g. password).
     *
     * @param nfcA
     * @param startPage
     * @param endPage
     * @return The command returns all bytes from all pages with one command
     */
    public static byte[] fastReadPageMifareUltralightEv1(NfcA nfcA, int startPage, int endPage) {
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x3A,           // FAST READ command
                    (byte) (startPage & 0x0ff),  // start page address
                    (byte) (endPage & 0x0ff)  // end page address
            });
            return response;
        } catch (IOException e) {
            Log.d(TAG, "FastReadPage failed with IOException: " + e.getMessage());
        }
        return null;
    }

    public static boolean writePageMifareUltralightEv1(NfcA nfcA, int page, byte[] data4Byte) {
        return writePageMifareUltralightEv1(nfcA, page, data4Byte, false);
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
    private static boolean writePageMifareUltralightEv1(NfcA nfcA, int page, byte[] data4Byte, boolean noPageCheck) {
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
        if (pagesToRead == 20) {
            if ((page >= 16) && (noPageCheck == false)) {
                Log.d(TAG, "writePage page is >= 16 = configuration area, aborted");
                return false;
            }
        } else if (pagesToRead == 41) {
            if ((page >= 36) && (noPageCheck == false)) {
                Log.d(TAG, "writePage page is >= 36 = configuration area, aborted");
                return false;
            }
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
     * for byte AUTH0 are from 00h to FFh.
     * Setting AUTH0 to FFh effectively disables memory protection
     * Note: as we are writing the configuration 0 page there are more settings involved.
     * Usually we should read this page and change just the necessary bits, but this methods does
     * write the following data:
     * a) Byte 0 = 'MOD', fixed to 0x 00h
     * b) Byte 1 = RFUI, fixed to 00h
     * c) Byte 2 = RFUI, fixed to 00h
     * d) Byte 3 = AUTH0, set with the auth0 parameter
     * In short, the page data is written to the page: 0x 00 00 00 <auth0>
     *
     * @param auth0
     * @return true on success and fals on failure
     */
    public static boolean writeAuth0UltralightEv1(NfcA nfcA, byte auth0) {
        if ((int) auth0 < 0) {
            Log.d(TAG, "writeAuth0UltralightC auth0 is < 0, aborted");
            return false;
        }
        if ((int) auth0 > 255) {
            Log.d(TAG, "writeAuth0UltralightC auth0 is > 255, aborted");
            return false;
        }
        byte[] data4Byte = new byte[4];
        data4Byte[3] = auth0;
        if (pagesToRead == 20) {
            return writePageMifareUltralightEv1(nfcA, 16, data4Byte, true);
        } else if (pagesToRead == 41) {
            return writePageMifareUltralightEv1(nfcA, 17, data4Byte, true);
        } else {
            return false;
        }
    }

    /**
     * The ACCESS byte defines if the authentication restriction is for write only or read and write access
     * Note: as we are writing the configuration 1 page there are more settings involved.
     * Usually we should read this page and change just the necessary bits, but this methods does
     * write the following data:
     * a) Byte 0 = 'Access' with these settings:
     *             Bit 7      = Prot is 0b when writeAccessRestrictedOnly = true and 1b when false (auth required for read and write access)
     *             Bit 6      = CFGLCK: fixed to '0b' Write locking bit for the user configuration, 0b = user configuration open to write access
     *             Bits 5/4/3 = RFUI, fixed to '0b'
     *             Bits 2/1/0 = AUTHLIM, fixed to '000b' = disabled Limitation of negative password verification attempts
     * b) Byte 1 = 'VCTID', fixed to 05h, Virtual Card Type Identifier which represents the response to a VCSL command.
     *             To ensure infrastructure compatibility, it is recommended not to change the default value of 05h.
     * c) Byte 2 = RFUI, fixed to 00h
     * d) Byte 3 = RFUI, fixed to 00h
     *
     * In short, this data is written to the page in case of writeAccessRestrictedOnly:
     *   - true:  0x 00 05 00 00 (just the write access is allowed after authentication)
     *   - false: 0x F0 05 00 00 (read and write acccess is allowed after authentication)
     *
     * @param writeAccessRestrictedOnly
     * @return true on success and false on failure
     */
    public static boolean writeProtUltralightEv1(NfcA nfcA, boolean writeAccessRestrictedOnly) {
        // if writeAccessRestrictedOnly == false the tag gets write and read restricted
        // if writeAccessRestrictedOnly == true the tag is only write restricted
        byte auth1 = 0; // default means write and read restricted
        if (writeAccessRestrictedOnly) auth1 = 1;
        byte[] data4Byte = new byte[4];
        data4Byte[0] = auth1;
        if (pagesToRead == 20) {
            return writePageMifareUltralightEv1(nfcA, 17, data4Byte, true);
        } else if (pagesToRead == 41) {
            return writePageMifareUltralightEv1(nfcA, 38, data4Byte, true);
        } else {
            return false;
        }
    }

    /**
     * Return the value of a 24-bit counter. The access is possible without any authentication.
     * @param nfcA
     * @param counterNumber in range 0..2
     * @return
     */
    public static int readCounterEv1(NfcA nfcA, int counterNumber) {
        if ((counterNumber < 0) || (counterNumber > 2)) {
            Log.e(TAG, "The counterNumber is out of range 0..2, aborted");
            return -3;
        }
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x39,
                    (byte) (counterNumber & 0x0ff)
            });
            if (response.length != 3) {
                return -1;
            } else {
                Log.d(TAG, "The value of counter " + counterNumber + " is: " + printData("response", response));
                return intFrom3ByteArrayInversed(response);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException when reading a counter: " + e.getMessage());
            return -2;
        }
    }

    public static boolean increaseCounterByOneEv1(NfcA nfcA, int counterNumber) {
        if ((counterNumber < 0) || (counterNumber > 2)) {
            Log.e(TAG, "The counterNumber is out of range 0..2, aborted");
            return false;
        }
        byte[] response = null;
        try {
            response = nfcA.transceive(new byte[]{
                    (byte) 0xA5,
                    (byte) (counterNumber & 0x0ff),
                    (byte) 0x01, // LSB order
                    (byte) 0x00,
                    (byte) 0x00,
                    (byte) 0x00, // this byte is ignored
            });
            return true;
        } catch (IOException e) {
            Log.e(TAG, "IOException when reading a counter: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads the complete content of the tag and returns an array of pages
     * Pages that are not readable (e.g. read protected or by design) are NULL.
     * There are 2 memory sizes on market:
     * MF0UL11 variant: 20 pages in total, user memory: 12 pages, readable 18 pages (0..17d)
     * MF0UL21 variant: 41 pages in total, user memory: 32 pages, readable 39 pages (0..38d)
     * The underlying Read Command returns 4 pages = 16 bytes in one command, but when using
     * the  Fast Read Command we can read the complete memory of the tag in one call.
     * Important: your Android device needs a buffer that has a minimum of 158 bytes capacity
     * (see maxTransceiveLength)
     * @param nfcA
     * @return
     */
    public static byte[] readCompleteContentFastReadEv1(NfcA nfcA) {
        System.out.println("pagesToRead: " + pagesToRead);
        byte[] readableContent = fastReadPageMifareUltralightEv1(nfcA, 0, pagesToRead - 3);
        byte[] completeContent;
        if (pagesToRead == 20) {
            completeContent = new byte[pagesToRead *4];
        } else {
            completeContent = new byte[(pagesToRead + 1) *4]; // as the tag has 41 pages I'm adding a "blind" page
        }
        // in case the tag is complete or partial read restricted the fastReadCommand returns a NAK
        // the we have to read the pages one by one to get the content that is readable
        if ((readableContent == null) || (readableContent.length < 10)) {
            Log.d(TAG, "The FastRead command returned a NAK, probably because the tag is read-only in parts. Now reading page wise");
            // read page wise
            byte[] pageContent = new byte[0];
            if (pagesToRead == 20) {
                // MF0UL11 variant
                for (int i = 0; i < pagesToRead; i++) {
                    if (pageContent != null) {
                        // skip further reading as we read a content that is read only and we did not run an authentication
                        pageContent = readPageMifareUltralightEv1(nfcA, i);
                        Log.d(TAG, "content of page: " + i + ": " + printData("cont", pageContent));
                        if (pageContent != null) System.arraycopy(pageContent, 0, completeContent, (i * 4), 4); // we are taking 1 page only to avoid roll-over effects
                    }
                }
            } else if (pagesToRead == 41) {
                // MF0UL21 variant
                for (int i = 0; i < pagesToRead; i++) {
                    if (pageContent != null) {
                        // skip further reading as we read a content that is read only and we did not run an authentication
                        pageContent = readPageMifareUltralightEv1(nfcA, i);
                        if (pageContent != null) System.arraycopy(pageContent, 0, completeContent, 0, 4); // we are taking 1 page only to avoid roll-over effects
                    }
                }
            } else {
                Log.d(TAG, "Unknown pagesToRead, aborted");
                return null;
            }
        } else {
            // we could read the complete memory with FastRead
            System.arraycopy(readableContent, 0, completeContent, 0, readableContent.length);
        }
        return completeContent;
    }

    /**
     * The MF0ULx1 supports the virtual card architecture by replying to a Virtual Card Select Last (VCSL)
     * command with a virtual card type identifier. The VCTID that is replied can be programmed in the
     * configuration pages. It enables infrastructure supporting this feature to process MIFARE
     * product-based cards across different MIFARE families in a common way.
     * @param nfcA
     * @return 0x05h as default
     */
    public static byte readVirtualCardSelectLastEv1(NfcA nfcA) {
        // reading page 17d or 38d depending on variant
        byte[] pageContent = new byte[0];
        if (pagesToRead == 20) {
            // MF0UL11 variant
            pageContent = readPageMifareUltralightEv1(nfcA, 17);
            if ((pageContent != null) && (pageContent.length > 3)){
                return pageContent[1];
            } else {
                return 0x00;
            }
        } else if (pagesToRead == 41) {
            // MF0UL21 variant
            pageContent = readPageMifareUltralightEv1(nfcA, 38);
            if ((pageContent != null) && (pageContent.length > 3)) {
                return pageContent[1];
            } else {
                return 0x00;
            }
        } else {
            return 0x00;
        }
    }

}
