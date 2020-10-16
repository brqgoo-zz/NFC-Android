package com.example.nfc_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.app.Activity;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.Web3j;

import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;
import android.view.WindowManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import java.util.Properties;
import java.math.BigInteger;
import android.nfc.NfcAdapter;
import android.app.PendingIntent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    BottomNavigationView bottomNavigationView;
    String privateKey;
    EditText descriptionTextBox;
    Button writeButton;
    Context context;
    Credentials credentials;
    String tagDescription;
    ECKeyPair ecKeyPair;
    Wallet wallet;
    Keys keys;
    FileOutputStream fOut;
    FileInputStream fin;

    EditText priKeyBox;
    EditText pubKeyBox;
    Boolean nfcEnabled;
    TextView scanningLabel;

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Write to NFC tag");
        context = this;





        bottomNavigationView = findViewById(R.id.bottomnavigation);
        bottomNavigationView.setSelectedItemId(R.id.write);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.write:
                        return true;
                    case R.id.auth:
                        startActivity(new Intent(context, AuthActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.info:
                        startActivity(new Intent(context, InfoActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });




        descriptionTextBox = findViewById(R.id.descriptionTextBox);
        writeButton = findViewById(R.id.writeButton);
        writeButton.setOnClickListener(MainActivity.this);

        priKeyBox = findViewById(R.id.priKeyBox);
        pubKeyBox = findViewById(R.id.pubKeyBox);
        scanningLabel = findViewById(R.id.scanningLabel);

        scanningLabel.setVisibility(View.GONE);
        nfcEnabled = false;
        writeButton.setText("WRITE");

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            //nfc not support your device.
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        try {
            fin = openFileInput("userPrivateKey");
            int i = 0;
            char c;
            byte[] bs = new byte[0];
            try {
                if(fin.available() > 0){
                    bs = new byte[fin.available()];
                    fin.read(bs);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            privateKey = bytesToHex(bs);
            System.out.println(" 2nd ime ");
            } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }

        if(privateKey == null){
            System.out.println(" first ime ");

            privateKey = getRandomHexString(64);

            try {
                fOut = openFileOutput("userPrivateKey",Context.MODE_PRIVATE);
                fOut.write(fromHexString(privateKey));
                fOut.close();
                System.out.println(" openFileOutput succ ");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println(" userPrivateKey " + privateKey);
        System.out.println(" userPublicKey " + getPublicKeyFromPrivateKey(privateKey));

        priKeyBox.setText(privateKey);
        pubKeyBox.setText(getPublicKeyFromPrivateKey(privateKey));

        Web3j web3 = Web3j.build(new HttpService());

    }


    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);

    }


    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        if (nfcEnabled == true) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            System.out.println("tagid" + bytesToHex(tag.getId()));
            //onTagWrite(bytesToHex(tag.getId()));
            Ndef ndefTag = Ndef.get(tag);

            if (ndefTag != null) {

                System.out.println("muha");
                try {
                    ndefTag.connect();
                    NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], fromHexString(onTagWrite(bytesToHex(tag.getId()))));

                    if (record.getPayload().length >= 50) {
                        ndefTag.writeNdefMessage(new NdefMessage(new NdefRecord[]{record}));
                        ndefTag.makeReadOnly();
                        Toast.makeText(MainActivity.this,
                                "Writing successful", Toast.LENGTH_SHORT).show();

                        nfcEnabled = false;
                        scanningLabel.setVisibility(View.GONE);
                        writeButton.setText("WRITE");
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Writing failed", Toast.LENGTH_SHORT).show();

                        nfcEnabled = false;
                        scanningLabel.setVisibility(View.GONE);
                        writeButton.setText("WRITE");
                    }


                } catch (IOException | FormatException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,
                            "Writing failed", Toast.LENGTH_SHORT).show();
                    nfcEnabled = false;
                    scanningLabel.setVisibility(View.GONE);
                    writeButton.setText("WRITE");
                }
            }

        }

    }


    @Override
    public void onClick(View v) {

        hideKeyboard(MainActivity.this);

        if (getPublicKeyFromPrivateKey(priKeyBox.getText().toString()).toLowerCase().compareTo(pubKeyBox.getText().toString().toLowerCase()) == 0){

            tagDescription = descriptionTextBox.getText().toString();

            if (tagDescription.length() >= 1) {
                if (nfcEnabled == true) {
                    nfcEnabled = false;
                    scanningLabel.setVisibility(v.GONE);
                    writeButton.setText("WRITE");
                } else {
                    nfcEnabled = true;
                    scanningLabel.setVisibility(v.VISIBLE);
                    writeButton.setText("CANCEL");
                }
            }
            else {
                Toast.makeText(MainActivity.this,
                        "Please type something first.", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(MainActivity.this,
                    "Given private key and public key does not match.", Toast.LENGTH_SHORT).show();
        }


    }

    private String onTagWrite(String uid) {

        String returnstr = "";

        tagDescription = descriptionTextBox.getText().toString();

            byte[] tagUID = fromHexString(uid);
            byte[] messageToSign = tagDescription.getBytes();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                outputStream.write(tagUID);
                outputStream.write(messageToSign);
            } catch (IOException e) {
                throw new RuntimeException("Error when generating signature", e);
            }

            byte[] hashedPrefixedMessage = Hash.sha256(outputStream.toByteArray());
            System.out.println("hashedPrefixedMessage: " + bytesToHex(hashedPrefixedMessage));

            Sign.SignatureData signedMessage = Sign.signMessage(
                    hashedPrefixedMessage, Credentials.create(privateKey).getEcKeyPair(), false);

            ByteArrayOutputStream sigStream = new ByteArrayOutputStream();

            try {
                sigStream.write(signedMessage.getR());
                sigStream.write(signedMessage.getS());
                sigStream.write(signedMessage.getV());
            } catch (IOException e) {
                throw new RuntimeException("Error when generating signature", e);
            }

            System.out.println("signedMessage is " + bytesToHex(sigStream.toByteArray()));

            ByteArrayOutputStream tagPayloadStream = new ByteArrayOutputStream();

            try {
                tagPayloadStream.write(fromHexString("FFFFFFFF"));
                tagPayloadStream.write(tagDescription.getBytes());
                tagPayloadStream.write(fromHexString("FFFFFFFF"));
                tagPayloadStream.write(sigStream.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Error when generating signature", e);
            }

            System.out.println("tagPayloadStream is " + bytesToHex(tagPayloadStream.toByteArray()));

            returnstr = bytesToHex(tagPayloadStream.toByteArray());

            return returnstr;

        }






    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] fromHexString(final String encoded) {
        if ((encoded.length() % 2) != 0)
            throw new IllegalArgumentException("Input string must contain an even number of characters");

        final byte result[] = new byte[encoded.length()/2];
        final char enc[] = encoded.toCharArray();
        for (int i = 0; i < enc.length; i += 2) {
            StringBuilder curr = new StringBuilder(2);
            curr.append(enc[i]).append(enc[i + 1]);
            result[i/2] = (byte) Integer.parseInt(curr.toString(), 16);
        }
        return result;
    }

    public static void hideKeyboard( Activity activity ) {
        InputMethodManager imm = (InputMethodManager)activity.getSystemService( Context.INPUT_METHOD_SERVICE );
        View f = activity.getCurrentFocus();
        if( null != f && null != f.getWindowToken() && EditText.class.isAssignableFrom( f.getClass() ) )
            imm.hideSoftInputFromWindow( f.getWindowToken(), 0 );
        else
            activity.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN );
    }

    private String getRandomHexString(int numchars){
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while(sb.length() < numchars){
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars);
    }

    private String getPublicKeyFromPrivateKey(String priKey){
        String derivedPubKey = bytesToHex(Credentials.create(priKey).getEcKeyPair().getPublicKey().toByteArray());

        String returnPubKey = "";

        if(derivedPubKey.length() == 128){
            returnPubKey = "04" + derivedPubKey;
        }
        else if(derivedPubKey.length() == 130){
            returnPubKey = "04" + derivedPubKey.substring(2);
        }
        else {
            Toast.makeText(MainActivity.this,
                    "Error derivedPubKey: " + derivedPubKey, Toast.LENGTH_SHORT).show();
        }
        return returnPubKey;
    }
}