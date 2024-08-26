package de.androidcrypto.mifare_ultralight_c_examples;

import static de.androidcrypto.mifare_ultralight_c_examples.Utils.bytesToHexNpe;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.doVibrate;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.hexStringToByteArray;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadCounterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadCounterFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ReadCounterFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReadCounterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReadCounterFragment newInstance(String param1, String param2) {
        ReadCounterFragment fragment = new ReadCounterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    TextView readResult;
    Button checkAccessBytes;
    private View loadingLayout;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;
    private TableLayout mLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        contextSave = getActivity().getApplicationContext();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        readResult = getView().findViewById(R.id.tvReadResult);
        checkAccessBytes = getView().findViewById(R.id.btnCheckAccessBytes);
        loadingLayout = getView().findViewById(R.id.loading_layout);
        mLayout = getView().findViewById(
                R.id.tableLayoutValueBlocksToInt);

        checkAccessBytes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // we do have these access bytes found: 787788 + 7f0788
                byte[] accBytes1 = hexStringToByteArray("787788");
                byte[] accBytes2 = hexStringToByteArray("7f0788");

                byte[][] accBytes1Matrix = ACBytesToACMatrix(accBytes1);
                byte[][] accBytes2Matrix = ACBytesToACMatrix(accBytes2);
                Context context = getContext();
                String desc11 = GetAccessConditionsDescription(context, accBytes1Matrix, 0, false);
                String desc12 = GetAccessConditionsDescription(context, accBytes1Matrix, 1, false);
                String desc13 = GetAccessConditionsDescription(context, accBytes1Matrix, 2, false);
                String desc14 = GetAccessConditionsDescription(context, accBytes1Matrix, 3, true);
                String desc21 = GetAccessConditionsDescription(context, accBytes2Matrix, 0, false);
                String desc22 = GetAccessConditionsDescription(context, accBytes2Matrix, 1, false);
                String desc23 = GetAccessConditionsDescription(context, accBytes2Matrix, 2, false);
                String desc24 = GetAccessConditionsDescription(context, accBytes2Matrix, 3, true);
                StringBuilder sb = new StringBuilder();
                sb.append("accBytesMatrix description").append("\n");
                sb.append("desc11: ").append(desc11).append("\n");
                sb.append("desc12: ").append(desc12).append("\n");
                sb.append("desc13: ").append(desc13).append("\n");
                sb.append("desc14: ").append(desc14).append("\n");
                sb.append("desc21: ").append(desc21).append("\n");
                sb.append("desc22: ").append(desc22).append("\n");
                sb.append("desc23: ").append(desc23).append("\n");
                sb.append("desc24: ").append(desc24).append("\n");

                // a human readable form


                writeToUiAppend(sb.toString());
                writeToUiFinal(readResult);
            }
        });

    }

    /**
     * Create a full colored string (representing one block).
     *
     * @param data   Block data as hex string (16 Byte, 32 Chars.).
     * @param hasUID True if the block is the first block of the entire tag
     *               (Sector 0, Block 0).
     * @return A full colored string.
     */
    private SpannableString colorDataBlock(String data, boolean hasUID) {
        SpannableString ret;
        if (hasUID) {
            // First block (UID, manuf. data).
            ret = new SpannableString(TextUtils.concat(
                    colorString(data,
                            ContextCompat.getColor(getContext(), R.color.purple))));
        } else {
            if (isValueBlock(data)) {
                // Value block.
                ret = colorString(data,
                        ContextCompat.getColor(getContext(), R.color.yellow));
            } else {
                // Just data.
                //ret = new SpannableString(data);
                ret = colorString(data,
                        ContextCompat.getColor(getContext(), R.color.white));
            }
        }
        return ret;
    }

    /**
     * Create a full colored sector trailer (representing the last block of
     * every sector).
     *
     * @param data Block data as hex string (16 Byte, 32 Chars.).
     * @return A full colored string.
     */
    private SpannableString colorSectorTrailer(String data) {
        // Get sector trailer colors.
        int colorKeyA = ContextCompat.getColor(getContext(), R.color.light_green);
        int colorKeyB = ContextCompat.getColor(getContext(), R.color.dark_green);
        int colorAC = ContextCompat.getColor(getContext(), R.color.orange);
        int colorDatByte = ContextCompat.getColor(getContext(), R.color.white);
        try {
            SpannableString keyA = colorString(
                    data.substring(0, 12), colorKeyA);
            SpannableString keyB = colorString(
                    data.substring(20), colorKeyB);
            SpannableString ac = colorString(
                    data.substring(12, 18), colorAC);
            SpannableString datByte = colorString(
                    data.substring(18, 20), colorDatByte);
            return new SpannableString(
                    TextUtils.concat(keyA, ac, datByte, keyB));
        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "Error while coloring " +
                    "sector trailer");
        }
        return new SpannableString(data);
    }

    /**
     * Create a colored string.
     *
     * @param data  The text to be colored.
     * @param color The color for the text.
     * @return A colored string.
     */
    public static SpannableString colorString(String data, int color) {
        SpannableString ret = new SpannableString(data);
        ret.setSpan(new ForegroundColorSpan(color),
                0, data.length(), 0);
        return ret;
    }

    /**
     * Check if the given block (hex string) is a value block.
     * NXP has PDFs describing what value blocks are. Google something
     * like "nxp MIFARE classic value block" if you want to have a
     * closer look.
     *
     * @param hexString Block data as hex string.
     * @return True if it is a value block. False otherwise.
     */
    public static boolean isValueBlock(String hexString) {
        byte[] b = hexStringToByteArray(hexString);
        if (b != null && b.length == 16) {
            // Google some NXP info PDFs about MIFARE Classic to see how
            // Value Blocks are formatted.
            // For better reading (~ = invert operator):
            // if (b0=b8 and b0=~b4) and (b1=b9 and b9=~b5) ...
            // ... and (b12=b14 and b13=b15 and b12=~b13) then
            return (b[0] == b[8] && (byte) (b[0] ^ 0xFF) == b[4]) &&
                    (b[1] == b[9] && (byte) (b[1] ^ 0xFF) == b[5]) &&
                    (b[2] == b[10] && (byte) (b[2] ^ 0xFF) == b[6]) &&
                    (b[3] == b[11] && (byte) (b[3] ^ 0xFF) == b[7]) &&
                    (b[12] == b[14] && b[13] == b[15] &&
                            (byte) (b[12] ^ 0xFF) == b[13]);
        }
        return false;
    }

    private String getAccessConditionHR(Context context, byte[] accessBytes, int blockIndex, boolean isSectorTrailer) {
        byte[][] accBytesMatrix = ACBytesToACMatrix(accessBytes);
        String desc = GetAccessConditionsDescription(context, accBytesMatrix, blockIndex, isSectorTrailer);
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read_counter, container, false);
    }

    //public static String GetAccessConditionsDescription(byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
    public static String GetAccessConditionsDescription(Context context, byte[][] sectorAccessBits, int blockIndex, boolean isSectorTrailer) {
        if (sectorAccessBits == null || blockIndex < 0 || blockIndex >= sectorAccessBits[0].length) {
            return "";
        }
        int accessBitsNumber = (sectorAccessBits[0][blockIndex] << 2) | (sectorAccessBits[1][blockIndex] << 1) | sectorAccessBits[0][blockIndex];
        String resAccessCondsPrefix = isSectorTrailer ? "ac_sector_trailer_" : "ac_data_block_";
        String accessCondsResIdStr = resAccessCondsPrefix + accessBitsNumber;
        //Context appContext = localMFCDataIface.GetApplicationContext();
        Log.d(TAG, "resAccessCondsPrefix: " + resAccessCondsPrefix);
        Log.d(TAG, "accessCondsResIdStr: " + accessCondsResIdStr);
        try {
            int accessCondsResId = R.string.class.getField(accessCondsResIdStr).getInt(null);
            Log.d(TAG, "appContext.getResources().getString(accessCondsResId): " + context.getResources().getString(accessCondsResId));
            return context.getResources().getString(accessCondsResId);
        } catch (Exception nsfe) {
            Log.e(TAG, "Exception in GetAccessConditionsDescription: " + nsfe.getMessage());
            return "";
        }
    }

    /**
     * Convert the Access Condition bytes to a matrix containing the
     * resolved C1, C2 and C3 for each block.
     *
     * @param acBytes The Access Condition bytes (3 byte).
     * @return Matrix of access conditions bits (C1-C3) where the first
     * dimension is the "C" parameter (C1-C3, Index 0-2) and the second
     * dimension is the block number (Index 0-3). If the ACs are incorrect
     * null will be returned.
     */
    private static byte[][] ACBytesToACMatrix(byte acBytes[]) {
        // ACs correct?
        // C1 (Byte 7, 4-7) == ~C1 (Byte 6, 0-3) and
        // C2 (Byte 8, 0-3) == ~C2 (Byte 6, 4-7) and
        // C3 (Byte 8, 4-7) == ~C3 (Byte 7, 0-3)
        byte[][] acMatrix = new byte[3][4];
        if (acBytes.length > 2 &&
                (byte) ((acBytes[1] >>> 4) & 0x0F) ==
                        (byte) ((acBytes[0] ^ 0xFF) & 0x0F) &&
                (byte) (acBytes[2] & 0x0F) ==
                        (byte) (((acBytes[0] ^ 0xFF) >>> 4) & 0x0F) &&
                (byte) ((acBytes[2] >>> 4) & 0x0F) ==
                        (byte) ((acBytes[1] ^ 0xFF) & 0x0F)) {
            // C1, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[0][i] = (byte) ((acBytes[1] >>> 4 + i) & 0x01);
            }
            // C2, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[1][i] = (byte) ((acBytes[2] >>> i) & 0x01);
            }
            // C3, Block 0-3
            for (int i = 0; i < 4; i++) {
                acMatrix[2][i] = (byte) ((acBytes[2] >>> 4 + i) & 0x01);
            }
            return acMatrix;
        }
        return null;
    }


    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        playSinglePing();
        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            readResult.setBackgroundColor(getResources().getColor(R.color.white));
            readResult.setText("");
        });

        // you should have checked that this device is capable of working with Mifare Classic tags, otherwise you receive an exception

        MifareClassic mfc = MifareClassic.get(tag);
        if (mfc == null) {
            writeToUiAppend("The tag is not readable with Mifare Classic classes, sorry");
            writeToUiFinal(readResult);
            setLoadingLayoutVisibility(false);
            return;
        }

        // get card details
        int ttype = mfc.getType();
        StringBuilder sb = new StringBuilder();
        sb.append("MifareClassic type: ").append(ttype).append("\n");
        int tagSize = mfc.getSize();
        sb.append("MifareClassic size: ").append(tagSize).append("\n");
        int sectorCount = mfc.getSectorCount();
        sb.append("MifareClassic sector count: ").append(sectorCount).append("\n");
        int blockCount = mfc.getBlockCount();
        sb.append("MifareClassic block count: ").append(blockCount).append("\n");

        sb.append("APP_DIRECTORY: ").append(bytesToHexNpe(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)).append("\n");
        sb.append("KEY_DEFAULT  : ").append(bytesToHexNpe(MifareClassic.KEY_DEFAULT)).append("\n");
        sb.append("KEY_NFC_FORUM: ").append(bytesToHexNpe(MifareClassic.KEY_NFC_FORUM)).append("\n");

        writeToUiAppend(sb.toString());

/*
default keys
KEY_MIFARE_
APPLICATION_
DIRECTORY:     a0a1a2a3a4a5
KEY_DEFAULT:   ffffffffffff
KEY_NFC_FORUM: d3f7d3f7d3f7
 */

/*
keys for red crowne card
4D57414C5648
FFFFFFFFFFFF
A0A1A2A3A4A5
4D48414C5648
 */
/*
promark keys
[ 0] ffffffffffff
[ 1] 000000000000
[ 2] a0a1a2a3a4a5
[ 3] b0b1b2b3b4b5
[ 4] c0c1c2c3c4c5
[ 5] d0d1d2d3d4d5
[ 6] aabbccddeeff
[ 7] 1a2b3c4d5e6f
[ 8] 123456789abc
[ 9] 010203040506
[10] 123456abcdef
[11] abcdef123456
[12] 4d3a99c351dd
[13] 1a982c7e459a
[14] d3f7d3f7d3f7
[15] 714c5c886e97
[16] 587ee5f9350f
[17] a0478cc39091
[18] 533cb6c723f6
[19] 8fd0a4f256e9

 */

        // go through all sectors
        try {
            mfc.connect();

            if (mfc.isConnected()) {
                for (int secCnt = 0; secCnt < sectorCount; secCnt++) {
                    writeToUiAppend("");
                    // this is the loop for all sectors of a Mifare Classic card
                    int sectorNumber;
                    boolean isSector0 = false;
                    boolean isReadableSector;
                    byte[] sectorData = new byte[64]; // sector 0 first 16 bytes a re UID & manufacture info, last 16 bytes is access block
                    byte[] uidData = null; // contains the UID & manufacture info, only if isSector0 = true
                    byte[] blockData; // contains 2 (if sector 0) or 3 blocks of 16 bytes of data
                    byte[] accessBlock = new byte[16]; // complete block, sections see below. key A and B are nulled as they can't read out
                    byte[] keyA = new byte[6]; // access key A
                    byte[] accessBits = new byte[3]; // 3 access bytes for access to the data elements
                    byte[] unused = new byte[1]; // unused byte, can be used for data
                    byte[] keyB = new byte[6]; // access key B
                    byte[][] readResult = readMifareSector(mfc, secCnt);
                    if (readResult == null) {
                        writeToUiAppend("ERROR - sector " + secCnt + " could not get read");
                        // write a zero data to SectorMc1kModel
                        sectorNumber = secCnt;
                        if (secCnt == 0) isSector0 = true;
                        isReadableSector = false;
                        sectorData = null;
                        uidData = null;
                        blockData = null;
                        accessBlock = null;
                        keyA = null;
                        accessBits = null;
                        unused = null;
                        keyB = null;

                    } else {
                        // analyze content data and write to SectorMc1kModel
                        sectorNumber = secCnt;
                        if (secCnt == 0) isSector0 = true;
                        isReadableSector = true;
                        sectorData = readResult[0];

                        // length depends on sector0 or not
                        if (isSector0) {
                            uidData = Arrays.copyOfRange(sectorData, 0, 16);
                            blockData = Arrays.copyOfRange(sectorData, 16, 48);
                        } else {
                            blockData = Arrays.copyOfRange(sectorData, 0, 48);
                        }
                        accessBlock = Arrays.copyOfRange(sectorData, 48, 64);
                        keyA = readResult[1];
                        accessBits = Arrays.copyOfRange(accessBlock, 6, 9);
                        unused = Arrays.copyOfRange(accessBlock, 9, 10);
                        keyB = readResult[2];

                    }


                } // for (int secCnt = 0; secCnt < sectorCount; secCnt++) {
                // at his point we check all blocks for ValueBlocks

                // convert all blockData to hexString
                // isValueBlock on String
                // collect an Array of lines
                // output to table
                // see DumpEditor - decodeValueBlocks()
                // see ValueBlocksToInt - onCreate


                requireActivity().runOnUiThread(() -> {
                    mLayout.setBackgroundColor(getResources().getColor(R.color.dark_gray));
                });

            }

            mfc.close();

        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }


        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
    }

    private String generateListEntry(int sector, int block, String line) {
        StringBuilder sb = new StringBuilder();
        sb.append(sector).append(":").append(block).append(":").append(line);
        return sb.toString();
    }

    /**
     * Add a row with position information to the layout table.
     * This row shows the user where the value block is located (sector, block).
     *
     * @param value The position information (e.g. "Sector: 1, Block: 2").
     */
    private void addPosInfoRow(String value) {
        requireActivity().runOnUiThread(() -> {
            TextView header = new TextView(getContext());
            header.setText(colorString(value,
                            ContextCompat.getColor(getContext(), R.color.blue)),
                    TextView.BufferType.SPANNABLE);
            TableRow tr = new TableRow(getContext());
            tr.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            tr.addView(header);
            mLayout.addView(tr, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
        });
    }

    /**
     * Add full value block information (original
     * and integer format) to the layout table (two rows).
     *
     * @param hexValueBlock The value block as hex string (32 chars.).
     */
    private void addValueBlock(String hexValueBlock) {
        requireActivity().runOnUiThread(() -> {
            TableRow tr = new TableRow(getContext());
            TextView what = new TextView(getContext());
            TextView value = new TextView(getContext());

            // Original.
            tr.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            what.setText(colorString("Original:",
                    ContextCompat.getColor(getContext(), R.color.white)));
            //what.setText("Original:");
            value.setText(colorString(hexValueBlock.substring(0, 8),
                    ContextCompat.getColor(getContext(), R.color.yellow)));
            tr.addView(what);
            tr.addView(value);
            mLayout.addView(tr, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            // Resolved to int.
            tr = new TableRow(getContext());
            tr.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            what = new TextView(getContext());
            what.setText(colorString("As Integer:",
                    ContextCompat.getColor(getContext(), R.color.white)));
            //what.setText("As Integer:");
            value = new TextView(getContext());
            byte[] asBytes = hexStringToByteArray(
                    hexValueBlock.substring(0, 8));
            reverseByteArrayInPlace(asBytes);
            ByteBuffer bb = ByteBuffer.wrap(asBytes);
            int i = bb.getInt();
            String asInt = "" + i;
            value.setText(colorString(asInt,
                    ContextCompat.getColor(getContext(), R.color.light_green)));
            tr.addView(what);
            tr.addView(value);
            mLayout.addView(tr, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
        });
    }

    /**
     * Reverse a byte Array (e.g. Little Endian -> Big Endian).
     * Hmpf! Java has no Array.reverse(). And I don't want to use
     * Commons.Lang (ArrayUtils) from Apache....
     *
     * @param array The array to reverse (in-place).
     */
    public static void reverseByteArrayInPlace(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }


    /**
     * read mifare classic card sector by sector
     *
     * @param mif
     * @param secCnt 0 to 15 (for Mifare Classic 1K) or in general sectorCount
     * @return a double byte array
     * [0] returns the complete data of one sector
     * [1] returns key A if used for authentication
     * [2] returns key B if used for authentication
     * returns NULL if no key was found
     */
    private byte[][] readMifareSector(MifareClassic mif, int secCnt) {
        byte[][] returnBytes = new byte[3][64];
        byte[] keyABytes = null;
        byte[] keyBBytes = null;
        byte[] dataBytes = new byte[64];
        boolean isAuthenticated = false;
        // try to authenticate with known keys - no brute force
        Log.d(TAG, "");
        Log.d(TAG, "readMifareSector " + secCnt);
        try {
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                keyABytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                Log.d(TAG, "Auth success with A KEY_MIFARE_APPLICATION_DIRECTORY");
                isAuthenticated = true;
                // there are 3 default keys available
                // MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY: a0a1a2a3a4a5
                // MifareClassic.KEY_DEFAULT:                      ffffffffffff
                // MifareClassic.KEY_NFC_FORUM:                    d3f7d3f7d3f7
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyABytes = MifareClassic.KEY_DEFAULT.clone();
                Log.d(TAG, "Auth success with A KEY_DEFAULT");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                keyABytes = MifareClassic.KEY_NFC_FORUM.clone();
                Log.d(TAG, "Auth success with A KEY_NFC_FORUM");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                keyBBytes = MifareClassic.KEY_DEFAULT.clone();
                Log.d(TAG, "Auth success with B KEY_DEFAULT");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                keyBBytes = MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY.clone();
                isAuthenticated = true;
                Log.d(TAG, "Auth success with B KEY_MIFARE_APPLICATION_DIRECTORY");
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                keyBBytes = MifareClassic.KEY_NFC_FORUM;
                Log.d(TAG, "Auth success with B KEY_NFC_FORUM");
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, hexStringToByteArray("4D57414C5648"))) {
                keyABytes = hexStringToByteArray("4D57414C5648");
                Log.d(TAG, "Auth success with A Crowne Plaza key");
                isAuthenticated = true;
                //4D57414C5648
            } else {
                //return null;
                Log.d(TAG, "NO Auth success");
            }

            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            int blocksInSector = mif.getBlockCountInSector(secCnt);
            // get the data of each block
            dataBytes = new byte[(16 * blocksInSector)];
            for (int blockInSectorCount = 0; blockInSectorCount < blocksInSector; blockInSectorCount++) {
                // get following data
                byte[] block = mif.readBlock((block_index + blockInSectorCount));
                System.arraycopy(block, 0, dataBytes, (blockInSectorCount * 16), 16);
            }
        } catch (IOException e) {
            Log.e(TAG, "Sector " + secCnt + " IOException: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        System.out.println("*** dataBytes for sector " + secCnt + " length: " + dataBytes.length + " data: " + bytesToHexNpe(dataBytes));
        returnBytes[0] = dataBytes;
        returnBytes[1] = keyABytes;
        returnBytes[2] = keyBBytes;
        return returnBytes;
    }

/*
    private byte[] readMifareSector(MifareClassic mif, int secCnt) {
        byte[] returnBytes = new byte[0];
        boolean isAuthenticated = false;
        // try to authenticate with known keys - no brute force
        try {
            if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_DEFAULT)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyA(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_DEFAULT)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)) {
                isAuthenticated = true;
            } else if (mif.authenticateSectorWithKeyB(secCnt, MifareClassic.KEY_NFC_FORUM)) {
                isAuthenticated = true;
                writeToUiAppend(readResult, "auth success in sector: " + secCnt);
            } else {
                writeToUiAppend(readResult, "auth denied in sector: " + secCnt);
                // gives an error, no access to textview from background thread
                return null;
            }
            // get the blockindex
            int block_index = mif.sectorToBlock(secCnt);
            // get block in sector
            int blocksInSector = mif.getBlockCountInSector(secCnt);
            // get the data of each block
            returnBytes = new byte[(16 * blocksInSector)];
            for (int blockInSectorCount = 0; blockInSectorCount < blocksInSector; blockInSectorCount++) {
                // get following data
                byte[] block = mif.readBlock((block_index + blockInSectorCount));
                System.arraycopy(block, 0, returnBytes, (blockInSectorCount * 16), 16);
            }
        } catch (IOException e) {
            writeToUiAppend(readResult, "IOException: " + e);
            e.printStackTrace();
            return null;
        }
        return returnBytes;
    }

 */

    /**
     * Sound files downloaded from Material Design Sounds
     * https://m2.material.io/design/sound/sound-resources.html
     */
    private void playSinglePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_02);
        mp.start();
    }

    private void playDoublePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_01);
        mp.start();
    }

    private void writeToUiAppend(String message) {
        //System.out.println(message);
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) readResult) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(outputString);
                    System.out.println(outputString); // print the data to console
                }
            });
        }
    }

    /**
     * shows a progress bar as long as the reading lasts
     *
     * @param isVisible
     */

    private void setLoadingLayoutVisibility(boolean isVisible) {
        getActivity().runOnUiThread(() -> {
            if (isVisible) {
                loadingLayout.setVisibility(View.VISIBLE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this.getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this.getActivity());
    }

}