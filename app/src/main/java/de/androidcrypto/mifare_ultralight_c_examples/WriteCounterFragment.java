package de.androidcrypto.mifare_ultralight_c_examples;

import static de.androidcrypto.mifare_ultralight_c_examples.Utils.doVibrate;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.printData;

import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteCounterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteCounterFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteCounterFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    com.google.android.material.textfield.TextInputLayout counter0Layout, counter1Layout, counter2Layout;
    com.google.android.material.textfield.TextInputEditText incrementCounter, counter0, counter1, counter2, resultNfcWriting;
    RadioButton incrementNoCounter, incrementCounter0, incrementCounter1, incrementCounter2;
    private View loadingLayout;

    private NfcAdapter mNfcAdapter;
    private String outputString = ""; // used for the UI output

    public WriteCounterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WriteCounterFragment newInstance(String param1, String param2) {
        WriteCounterFragment fragment = new WriteCounterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write_counter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        incrementCounter = getView().findViewById(R.id.etCounterIncreaseValue);
        counter0 = getView().findViewById(R.id.etCounter0);
        counter1 = getView().findViewById(R.id.etCounter1);
        counter2 = getView().findViewById(R.id.etCounter2);
        incrementNoCounter = getView().findViewById(R.id.rbCounterNoIncrease);
        incrementCounter0 = getView().findViewById(R.id.rbIncreaseCounter0);
        incrementCounter1 = getView().findViewById(R.id.rbIncreraseCounter1);
        incrementCounter2 = getView().findViewById(R.id.rbIncreaseCounter2);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        loadingLayout = getView().findViewById(R.id.loading_layout);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {

        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        playSinglePing();
        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            resultNfcWriting.setText("");
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.white));
        });

        //System.out.println("*** TL: " + tag.getTechList().toString());

        // identify the tag
        TagIdentification ti = new TagIdentification(tag);
        if (ti != null) {
            writeToUiAppend(ti.dumpMifareUltralight());
        }

        MifareUltralight mfu = MifareUltralight.get(tag);

        if (mfu == null) {
            writeToUiAppend("The tag is not readable with Mifare Ultralight classes, sorry");
            writeToUiFinal(resultNfcWriting);
            setLoadingLayoutVisibility(false);
            return;
        }

        try {
            mfu.connect();

            // todo distinguish between Ultralight (no counter), Ultralight-C (3 counter in non user memory) and Ultralight EV1 (1 counter in page)
            // todo enable counter0 on C and EV-1 only
            // todo enable counter1 and counter2 on EV1 only

            // as the incrementCounter EditText is disabled and fixed to 1 the exception should never thrown
            try {
                int increaseValue = Integer.parseInt(incrementCounter.getText().toString());
            } catch (NumberFormatException e) {
                Log.e(TAG, "wrong value in increaseValue: " + e.getMessage());
                writeToUiAppend("wrong value in increaseValue: " + e.getMessage());
                writeToUiFinal(resultNfcWriting);
                setLoadingLayoutVisibility(false);
                return;
            }

            if (ti.getTagTypeSubName().equals(TagIdentification.techUltralight.MifareUltralightFirst.toString())) {
                Log.d(TAG, "As the tag type is of Ultralight (first) there are no counter available");
                writeToUiAppend("As the tag type is of Ultralight (first) there are no counter available");
                writeToUiFinal(resultNfcWriting);
                setLoadingLayoutVisibility(false);
                return;
            }

            if (ti.getTagTypeSubName().equals(TagIdentification.techUltralight.MifareUltralightC.toString())) {
                Log.d(TAG, "Running the code for Ultralight-C");
                // this is for Mifare Ultralight-C only
                // the counter is located in page 41d bytes 0 + 1 (16 bit counter)
                int counterNumber = -1;
                if (incrementNoCounter.isChecked()) {
                    counterNumber = -1;
                    Log.d(TAG, "no counter should get increased");
                    writeToUiAppend("no counter should get increased");
                }
                if (incrementCounter0.isChecked()) {
                    byte[] increaseValue = new byte[]{(byte) 0x01, (byte) 0x0, (byte) 0x0, (byte) 0x0,};
                    boolean increaseCounterResult = writePageMifareUltralight(mfu, 41, increaseValue);
                    writeToUiAppend("increaseCounterResult is " + increaseCounterResult);
                }
                if (incrementCounter1.isChecked()) {
                    Log.d(TAG, "only counter 0 is available on Ultralight-C tags");
                    writeToUiAppend("only counter 0 is available on Ultralight-C tags");
                }
                if (incrementCounter2.isChecked()) {
                    Log.d(TAG, "only counter 0 is available on Ultralight-C tags");
                    writeToUiAppend("only counter 0 is available on Ultralight-C tags");
                }
                byte[] counter0B = readPageMifareUltralight(mfu, 41);
                writeToUiAppend(printData("Counter in page 41d", counter0B));
                int counter0I = 0, counter1I = 0, counter2I = 0;
                if (counter0B != null) counter0I = byteArrayToInt2Byte(counter0B);
                writeCounterToUi(counter0I, counter1I, counter2I);

            }

            if (ti.getTagTypeSubName().equals(TagIdentification.techUltralight.MifareUltralightEv1.toString())) {
                Log.d(TAG, "Running the code for Ultralight EV1");

                // this is for Mifare Ultralight EV-1 only
                // write the counter
                int counterNumber = -1;

                if (incrementNoCounter.isChecked()) {
                    counterNumber = -1;
                    Log.d(TAG, "no counter should get increased");
                    writeToUiAppend("no counter should get increased");
                }

                if (incrementCounter0.isChecked()) counterNumber = 0;
                if (incrementCounter1.isChecked()) counterNumber = 1;
                if (incrementCounter2.isChecked()) counterNumber = 2;
                if (counterNumber > -1) {
                    byte[] increaseResponse = increaseCounterByOneMifareUltralightEv1(mfu, counterNumber);
                    writeToUiAppend(printData("increase counter" + counterNumber + " response", increaseResponse));
                }
                // read the counters
                byte[] counter0B = readCounterMifareUltralightEv1(mfu, 0);
                byte[] counter1B = readCounterMifareUltralightEv1(mfu, 1);
                byte[] counter2B = readCounterMifareUltralightEv1(mfu, 2);
                writeToUiAppend(printData("counter0", counter0B));
                writeToUiAppend(printData("counter1", counter1B));
                writeToUiAppend(printData("counter2", counter2B));
                int counter0I = 0, counter1I = 0, counter2I = 0;
                if (counter0B != null) counter0I = byteArrayToInt3Byte(counter0B);
                if (counter1B != null) counter1I = byteArrayToInt3Byte(counter1B);
                if (counter2B != null) counter2I = byteArrayToInt3Byte(counter2B);
                writeCounterToUi(counter0I, counter1I, counter2I);
            }

        } catch (IOException e) {
            //throw new RuntimeException(e);
            Log.e(TAG, "Error on connection to NFC tag: " + e.getMessage());
            writeToUiAppend("Error on connection to NFC tag: " + e.getMessage());
        }

        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());

    }

    public static int byteArrayToInt2Byte(byte[] b) {
        if (b == null) return 0;
        if (b.length != 2) return 0;
        return b[0] & 0xFF |
                (b[1] & 0xFF) << 8;
    }

    public static int byteArrayToInt3Byte(byte[] b) {
        if (b == null) return 0;
        if (b.length != 3) return 0;
        return b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16;
    }

    public static int byteArrayToInt4Byte(byte[] b) {
        if (b == null) return 0;
        if (b.length != 4) return 0;
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    private byte[] readPageMifareUltralight(MifareUltralight mfu, int page) {
        byte[] response = null;
        try {
            response = mfu.transceive(new byte[]{
                    (byte) 0x30,           // READ a page is 4 bytes long
                    (byte) (page & 0x0ff)  // page address
            });
            return response;
        } catch (IOException e) {
            Log.d(TAG, "on page " + page + " readPage failed with IOException: " + e.getMessage());
            //writeToUiAppend("on page " + page + " readPage failed with IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }

    private boolean writePageMifareUltralight(MifareUltralight mfu, int page, byte[] data4Byte) {
        if (data4Byte == null) {
            Log.d(TAG, "writePage data is NULL, aborted");
            return false;
        }
        if (data4Byte.length != 4) {
            Log.d(TAG, "writePage data is not exact 4 bytes long, aborted");
            return false;
        }
        byte[] response = null;
        try {
            mfu.writePage(41, data4Byte);
            return true;
        } catch (IOException e) {
            Log.d(TAG, "on page " + page + " readPage failed with IOException: " + e.getMessage());
            //writeToUiAppend("on page " + page + " readPage failed with IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return false;
    }


    /**
     * read the counter value for Mifare Ultralight EV1
     *
     * @param mfu
     * @param counterNumber 0, 1 or 2
     * @return the value for the counter, null is any error occurs
     */
    private byte[] readCounterMifareUltralightEv1(MifareUltralight mfu, int counterNumber) {
        if ((counterNumber < 0) | (counterNumber > 2)) {
            return null;
        }
        byte[] response = null;
        try {
            byte[] getCounterCommand = new byte[]{(byte) 0x39, (byte) (counterNumber & 0x0ff)};
            response = mfu.transceive(getCounterCommand);
            return response;
        } catch (IOException e) {
            Log.d(TAG, "readCounter IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connenction and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * increase the counter value by 1 for Mifare Ultralight EV1
     *
     * @param mfu
     * @param counterNumber
     * @return ACK/NAK
     */
    private byte[] increaseCounterByOneMifareUltralightEv1(MifareUltralight mfu, int counterNumber) {
        if ((counterNumber < 0) | (counterNumber > 2)) {
            return null;
        }
        byte[] response = null;
        try {
            byte[] getCounterCommand = new byte[]{(
                    byte) 0xA5, // increase counter command
                    (byte) (counterNumber & 0x0ff), // counter number
                    (byte) 0x01, // increase by 1
                    (byte) 0x00,
                    (byte) 0x00,
                    (byte) 0x00
            };
            response = mfu.transceive(getCounterCommand);
            return response;
        } catch (IOException e) {
            Log.d(TAG, "readCounter IOException: " + e.getMessage());
        }
        // this is just an advice - if an error occurs - close the connenction and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            mfu.close();
        } catch (Exception e) {
        }
        try {
            mfu.connect();
        } catch (Exception e) {
        }
        return null;
    }

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

    private void writeCounterToUi(final int counter0I, final int counter1I, final int counter2I) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                counter0.setText(String.valueOf(counter0I));
                counter1.setText(String.valueOf(counter1I));
                counter2.setText(String.valueOf(counter2I));
            }
        });
    }

    private void writeToUiAppend(String message) {
        //System.out.println(message);
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) resultNfcWriting) {
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

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            resultNfcWriting.setText(message);
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
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
            mNfcAdapter.enableReaderMode(getActivity(),
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
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}