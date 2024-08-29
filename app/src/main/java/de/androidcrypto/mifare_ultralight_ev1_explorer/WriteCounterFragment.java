package de.androidcrypto.mifare_ultralight_ev1_explorer;

import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_C.authenticateUltralightC;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_C.customAuthKey;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_C.doAuthenticateUltralightCDefault;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_C.getCounterValue;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_C.identifyUltralightFamily;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_C.increaseCounterValueByOne;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.bytesToHexNpe;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.doVibrate;
import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
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

import java.util.Arrays;

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

    private com.google.android.material.textfield.TextInputLayout counter0Layout;
    private com.google.android.material.textfield.TextInputEditText incrementCounter, counter0, resultNfcWriting;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth;
    private RadioButton incrementNoCounter, incrementCounter0;
    private View loadingLayout;
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private String outputString = ""; // used for the UI output
    private boolean isTagUltralight = false;

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

        counter0 = getView().findViewById(R.id.etCounter0);
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        incrementNoCounter = getView().findViewById(R.id.rbCounterNoIncrease);
        incrementCounter0 = getView().findViewById(R.id.rbIncreaseCounter0);
        resultNfcWriting = getView().findViewById(R.id.etReadResult);
        loadingLayout = getView().findViewById(R.id.loading_layout);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {

        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an NcfA Technology Type

        boolean success;
        boolean authSuccess = false;

        Log.d(TAG, "NFC tag discovered");
        playSinglePing();
        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            resultNfcWriting.setText("");
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.white));
        });

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception
        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(resultNfcWriting);
            setLoadingLayoutVisibility(false);
            returnOnNotSuccess();
            return;
        }

        try {
            nfcA.connect();

            if (nfcA.isConnected()) {
                // get card details
                byte[] tagId = nfcA.getTag().getId();
                String[] techList = nfcA.getTag().getTechList();
                StringBuilder sb = new StringBuilder();
                sb.append("Technical Data of the Tag").append("\n");
                sb.append("Tag ID: ").append(bytesToHexNpe(tagId)).append("\n");
                sb.append("Tech-List:").append("\n");
                sb.append("Tag TechList: ").append(Arrays.toString(techList)).append("\n");
                if (identifyUltralightFamily(nfcA)) {
                    sb.append("The Tag seems to be a MIFARE Ultralight Family tag").append("\n");
                    isTagUltralight = true;
                } else {
                    sb.append("The Tag IS NOT a MIFARE Ultralight tag").append("\n");
                    sb.append("** End of Processing **").append("\n");
                    isTagUltralight = false;
                }
                writeToUiAppend(sb.toString());

                // stop processing if not an Ultralight Family tag
                if (!isTagUltralight) {
                    returnOnNotSuccess();
                    return;
                }
                writeToUiAppend("This is an Ultralight C tag with 48 pages = 192 bytes memory");

                if (rbNoAuth.isChecked()) {
                    writeToUiAppend("No Authentication requested");
                    authSuccess = false;
                } else if (rbDefaultAuth.isChecked()) {
                    writeToUiAppend("Authentication with Default Key requested");
                    authSuccess = doAuthenticateUltralightCDefault(nfcA);
                    writeToUiAppend("authenticateUltralightC with defaultAuthKey success: " + authSuccess);
                } else {
                    writeToUiAppend("Authentication with Custom Key requested");
                    authSuccess = authenticateUltralightC(nfcA, customAuthKey);
                    writeToUiAppend("authenticateUltralightC with customAuthKey success: " + authSuccess);
                }
            }


            // this is for Mifare Ultralight-C only
            // the counter is located in page 41d bytes 0 + 1 (16 bit counter)
            if (incrementNoCounter.isChecked()) {
                Log.d(TAG, "No counter should get increased");
                writeToUiAppend("No counter should get increased");
            }
            if (incrementCounter0.isChecked()) {
                if (!authSuccess) {
                    writeToUiAppend("Previous Auth was not successful or not done, skipped");
                } else {
                    success = increaseCounterValueByOne(nfcA);
                    writeToUiAppend("Status of increaseCounterValueByOne command to page 41: " + success);
                }
            }
            int counter0I = getCounterValue(nfcA);
            writeToUiAppend("Counter in page 41d: " + counter0I);
            writeCounterToUi(counter0I, 0, 0);

        } catch (Exception e) {
            writeToUiAppend("Exception on connection: " + e.getMessage());
            e.printStackTrace();
        }

        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        reconnect(nfcA);

    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        mNfcAdapter.disableReaderMode(this.getActivity());
    }

    private void reconnect(NfcA nfcA) {
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            nfcA.close();
            Log.d(TAG, "Close NfcA");
        } catch (Exception e) {
            Log.e(TAG, "Exception on Close NfcA: " + e.getMessage());
        }
        try {
            Log.d(TAG, "Reconnect NfcA");
            nfcA.connect();
        } catch (Exception e) {
            Log.e(TAG, "Exception on Reconnect NfcA: " + e.getMessage());
        }
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

            // Enable ReaderMode for NfcA types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
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