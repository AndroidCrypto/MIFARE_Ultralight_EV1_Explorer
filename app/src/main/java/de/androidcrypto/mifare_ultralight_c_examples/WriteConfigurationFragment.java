package de.androidcrypto.mifare_ultralight_c_examples;

import static de.androidcrypto.mifare_ultralight_c_examples.MIFARE_Ultralight_C.authenticateUltralightC;
import static de.androidcrypto.mifare_ultralight_c_examples.MIFARE_Ultralight_C.customAuthKey;
import static de.androidcrypto.mifare_ultralight_c_examples.MIFARE_Ultralight_C.doAuthenticateUltralightCDefault;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.bytesToHexNpe;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.doVibrate;
import static de.androidcrypto.mifare_ultralight_c_examples.Utils.hexStringToByteArray;

import android.content.Context;
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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteConfigurationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteConfigurationFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ReadCounterFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public WriteConfigurationFragment() {
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
    public static WriteConfigurationFragment newInstance(String param1, String param2) {
        WriteConfigurationFragment fragment = new WriteConfigurationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }
    private TextView readResult;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth;
    private View loadingLayout;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private boolean isTagUltralight = false;
    private boolean[] isPageReadable;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;
    private byte[][] pagesComplete;
    private int pagesToRead;
    byte[] versionData;
    private boolean isUltralightC = false;
    private boolean isUltralightEv1 = false;
    private int counter0 = 0;

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
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        loadingLayout = getView().findViewById(R.id.loading_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write_configuration, container, false);
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
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
            readResult.setText("");
            readResult.setBackgroundColor(getResources().getColor(R.color.white));
        });

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception
        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(readResult);
            setLoadingLayoutVisibility(false);
            return;
        }

        try {
            nfcA.connect();

            if (nfcA.isConnected()) {

                // get card details
                byte[] atqa = nfcA.getAtqa();
                int sak = nfcA.getSak();
                int maxTransceiveLength = nfcA.getMaxTransceiveLength();
                byte[] tagId = nfcA.getTag().getId();
                String[] techList = nfcA.getTag().getTechList();
                StringBuilder sb = new StringBuilder();
                sb.append("Technical Data of the Tag").append("\n");
                sb.append("ATQA: ").append(bytesToHexNpe(atqa)).append("\n");
                sb.append("SAK:  ").append(sak).append("\n");
                sb.append("Tag ID: ").append(bytesToHexNpe(tagId)).append("\n");
                sb.append("Tech-List:").append("\n");
                sb.append("Tag TechList: ").append(Arrays.toString(techList)).append("\n");

                // sanity check on atqa and sak
                final byte[] atqaUltralight = hexStringToByteArray("4400");
                final int sakUltralight = 0;
                if ((Arrays.equals(atqa, atqaUltralight)) && (sak == sakUltralight)) {
                    sb.append("The Tag seems to be a MIFARE Ultralight tag").append("\n");
                    isTagUltralight = true;
                } else {
                    sb.append("The Tag IS NOT a MIFARE Ultralight tag").append("\n");
                    sb.append("** End of Processing **").append("\n");
                }
                sb.append("maxTransceiveLength: ").append(maxTransceiveLength).append("\n");

                writeToUiAppend(sb.toString());
                // stop processing if not an Ultralight tag
                if (!isTagUltralight) return;

                pagesToRead = 48;
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
        } catch (Exception e) {
            writeToUiAppend("Exception on connection: " + e.getMessage());
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