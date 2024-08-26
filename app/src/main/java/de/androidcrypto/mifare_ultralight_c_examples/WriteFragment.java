package de.androidcrypto.mifare_ultralight_c_examples;

import static de.androidcrypto.mifare_ultralight_c_examples.Utils.doVibrate;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.getTimestampShort;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.printData;

import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    com.google.android.material.textfield.TextInputEditText dataToSend, resultNfcWriting;
    SwitchMaterial addTimestampToData;
    AutoCompleteTextView autoCompleteTextView;
    com.google.android.material.textfield.TextInputLayout dataToSendLayout;

    private NfcAdapter mNfcAdapter;
    private int pageToWrite;
    private String outputString = ""; // used for the UI output

    public WriteFragment() {
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
    public static WriteFragment newInstance(String param1, String param2) {
        WriteFragment fragment = new WriteFragment();
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
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        dataToSend = getView().findViewById(R.id.etWriteData);
        dataToSendLayout = getView().findViewById(R.id.etWriteDataLayout);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        addTimestampToData = getView().findViewById(R.id.swMainAddTimestampSwitch);
        addTimestampToData.setChecked(false);

        // The minimum number of pages to write is 12 (= 48 bytes user memory)
        // as we are writing a 16 bytes long data we do need 4 pages to write the data and
        // therefore when writing to page 9 we will write to pages 9, 10, 11 and 12
        String[] type = new String[]{
                "4", "5", "6", "7", "8",
                "9"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                type);

        autoCompleteTextView = getView().findViewById(R.id.writePage);
        autoCompleteTextView.setText(type[0]);
        autoCompleteTextView.setAdapter(arrayAdapter);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        // todo work with pages > 12 depending on tag type (12 is common minimum for all Ultralight tag types)
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "pageToWrite: " + pageToWrite);
            }
        });

        addTimestampToData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // ts is 15 chars long + 1 trailing " " = 20 characters
                if (b) {
                    dataToSendLayout.setEnabled(false);

                } else {
                    dataToSendLayout.setEnabled(true);
                    dataToSendLayout.setCounterMaxLength(16);
                    setEditTextMaxLength(dataToSend, 16);
                }
            }
        });

    }

    private void setEditTextMaxLength(EditText et, int maxLength) {
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
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
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            resultNfcWriting.setText("");
        });

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception

        String sendData = dataToSend.getText().toString();
        if (addTimestampToData.isChecked()) sendData = getTimestampShort() + " ";
        if (TextUtils.isEmpty(sendData)) {
            writeToUiAppend("Please enter some data to write on tag. Aborted");
            writeToUiFinal(resultNfcWriting);
            return;
        }
        if (sendData.length() > 16) sendData = sendData.substring(0, 16);

        // identify the tag
        TagIdentification ti = new TagIdentification(tag);
        if (ti != null) {
            writeToUiAppend(ti.dumpMifareUltralight());
        }

        MifareUltralight mfu = MifareUltralight.get(tag);

        if (mfu == null) {
            writeToUiAppend("The tag is not readable with Mifare Ultralight classes, sorry");
            writeToUiFinal(resultNfcWriting);
            //setLoadingLayoutVisibility(false);
            return;
        }

        try {
            mfu.connect();

            if (mfu.isConnected()) {

                // lets read some data
                int pagesToRead = 48;
                byte[][] pagesComplete = new byte[pagesToRead][];
                for (int i = 0; i < pagesToRead; i++) {
                    pagesComplete[i] = readPageMifareUltralight(mfu, i);
                    writeToUiAppend(printData("page " + i, pagesComplete[i]));
                }

                // get page to write
                String choiceString = autoCompleteTextView.getText().toString();
                pageToWrite = Integer.parseInt(choiceString);

                byte[] dtw = new byte[16];
                System.arraycopy(sendData.getBytes(StandardCharsets.UTF_8), 0, dtw, 0, sendData.getBytes(StandardCharsets.UTF_8).length); // this is an array filled up with 0x00
                writeToUiAppend(printData("data to write", dtw));
                // split dtw (16 bytes long) into 4 byte arrays page 1 to 4
                byte[] page1 = Arrays.copyOfRange(dtw, 0, 4);
                byte[] page2 = Arrays.copyOfRange(dtw, 4, 8);
                byte[] page3 = Arrays.copyOfRange(dtw, 8, 12);
                byte[] page4 = Arrays.copyOfRange(dtw, 12, 16);

                // write to tag
                boolean writeSuccess = writePageMifareUltralight(mfu, pageToWrite, page1);
                writeToUiAppend("Tried to write data to tag on page " + pageToWrite + ", success ? : " + writeSuccess);
                writeSuccess = writePageMifareUltralight(mfu, pageToWrite + 1, page2);
                writeToUiAppend("Tried to write data to tag on page " + pageToWrite + 1 + ", success ? : " + writeSuccess);
                writeSuccess = writePageMifareUltralight(mfu, pageToWrite + 2, page3);
                writeToUiAppend("Tried to write data to tag on page " + pageToWrite + 1 + ", success ? : " + writeSuccess);
                writeSuccess = writePageMifareUltralight(mfu, pageToWrite + 3, page4);
                writeToUiAppend("Tried to write data to tag on page " + pageToWrite + 1 + ", success ? : " + writeSuccess);
                mfu.close();
            }
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }

        doVibrate(getActivity());
        playDoublePing();
        writeToUiFinal(resultNfcWriting);

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
            mfu.writePage(page, data4Byte);
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