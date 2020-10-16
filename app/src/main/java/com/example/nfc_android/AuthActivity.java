package com.example.nfc_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.app.Activity;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.Web3j;

import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.util.Random;
import android.view.WindowManager;
import org.web3j.crypto.Sign.SignatureData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.web3j.utils.Numeric;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import java.util.Properties;
import java.math.BigInteger;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import android.nfc.NfcAdapter;
import android.app.PendingIntent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;



public class AuthActivity extends AppCompatActivity implements View.OnClickListener {

    Context context;
    BottomNavigationView bottomNavigationView;
    EditText expectedPubKeyBox;
    Button authButton;

    NfcAdapter mAdapter;
    PendingIntent mPendingIntent;

    Boolean nfcEnabled;
    TextView scanningLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);


        setTitle("Write to NFC tag");
        context = this;







        bottomNavigationView = findViewById(R.id.bottomnavigation);
        bottomNavigationView.setSelectedItemId(R.id.auth);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.write:
                        startActivity(new Intent(context, MainActivity.class));
                        overridePendingTransition(0,0);
                    case R.id.auth:
                        return true;
                    case R.id.info:
                        startActivity(new Intent(context, InfoActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });

        expectedPubKeyBox = findViewById(R.id.expectedPubKeyBox);
        authButton = findViewById(R.id.authButton);
        authButton.setOnClickListener(AuthActivity.this);
        scanningLabel = findViewById(R.id.scanningLabel);

        scanningLabel.setVisibility(View.GONE);
        nfcEnabled = false;
        authButton.setText("AUTH");

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            //nfc not support your device.
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

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
            System.out.println("tagfndns");
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            System.out.println("tagid" + bytesToHex(tag.getId()));
            GetDataFromTag(tag, intent);
        }

    }

    private void GetDataFromTag(Tag tag, Intent intent) {
        Ndef ndef = Ndef.get(tag);
        try {
            ndef.connect();
//            txtType.setText(ndef.getType().toString());
//            txtSize.setText(String.valueOf(ndef.getMaxSize()));
//            txtWrite.setText(ndef.isWritable() ? "True" : "False");
            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (messages != null) {
                NdefMessage[] ndefMessages = new NdefMessage[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    ndefMessages[i] = (NdefMessage) messages[i];
                }
                NdefRecord record = ndefMessages[0].getRecords()[0];

                byte[] payload = record.getPayload();
                String text = new String(payload);
                System.out.println("vahid" + bytesToHex(record.getPayload()));
                onTag(bytesToHex(tag.getId()),bytesToHex(record.getPayload()));
                ndef.close();

            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Cannot Read From Tag.", Toast.LENGTH_LONG).show();
            nfcEnabled = false;
            scanningLabel.setVisibility(View.GONE);
            authButton.setText("AUTH");
        }
    }

    @Override
    public void onClick(View v) {

        hideKeyboard(AuthActivity.this);

        System.out.println("dssdc");
        if(expectedPubKeyBox.getText().toString().length() >= 124) {
            System.out.println("dssdc2");
            //onTag();
            if (nfcEnabled == true) {
                nfcEnabled = false;
                scanningLabel.setVisibility(v.GONE);
                authButton.setText("AUTH");
            } else {
                nfcEnabled = true;
                scanningLabel.setVisibility(v.VISIBLE);
                authButton.setText("CANCEL");
            }
        }
        else {
            Toast.makeText(AuthActivity.this,
                    "Please type a valid public key.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onTag(String uid, String pyld) {

        hideKeyboard(AuthActivity.this);

        if(expectedPubKeyBox.getText().toString().length() >= 124){

            String tagUID = uid;
            String tagPayLoadHexStr = pyld;

            String tagPayLoadDescriptionHexStr = tagPayLoadHexStr.split("FFFFFFFF")[1].split("FFFFFFFF")[0];
            String tagPayLoadSigHexStr = tagPayLoadHexStr.split("FFFFFFFF")[2];

            System.out.println(tagPayLoadDescriptionHexStr);
            System.out.println(tagPayLoadSigHexStr);

            byte[] message = fromHexString(tagUID.toLowerCase() + tagPayLoadDescriptionHexStr.toLowerCase());
            byte[] messageHash = Hash.sha256(message);

            byte[] signatureBytes = fromHexString(tagPayLoadSigHexStr);
            byte vVal = signatureBytes[64];

            System.out.println("message");
            System.out.println(bytesToHex(message));

            System.out.println("messageHash");
            System.out.println(bytesToHex(messageHash));

            System.out.println("vVal");
            System.out.println(vVal);

            Boolean authSuc = false;

            SignatureData sd =
                    new SignatureData(
                            vVal,
                            (byte[]) Arrays.copyOfRange(signatureBytes, 0, 32),
                            (byte[]) Arrays.copyOfRange(signatureBytes, 32, 64));

            BigInteger publicKey = null;
            for (int i = 0; i < 4; i++) {

                System.out.println(i);

                publicKey =
                        Sign.recoverFromSignature(
                                (byte) i,
                                new ECDSASignature(
                                        new BigInteger(1, sd.getR()), new BigInteger(1, sd.getS())),
                                messageHash);

                if (publicKey != null) {

                    System.out.println("puubkeyis");
                    System.out.println(encodeRawPubKey(bytesToHex(publicKey.toByteArray())));


                    if(encodeRawPubKey(bytesToHex(publicKey.toByteArray())).toLowerCase().compareTo(expectedPubKeyBox.getText().toString().toLowerCase()) == 0){
                        authSuc = true;
                    }
                }


            }
                if(authSuc == false){
                    Toast.makeText(AuthActivity.this,
                            "Authentication failed.", Toast.LENGTH_SHORT).show();

                    nfcEnabled = false;
                    scanningLabel.setVisibility(View.GONE);
                    authButton.setText("AUTH");
                }
                else {
                    Toast.makeText(AuthActivity.this,
                            "Authentication success: " + new String(fromHexString(tagPayLoadDescriptionHexStr), StandardCharsets.UTF_8), Toast.LENGTH_LONG).show();

                    nfcEnabled = false;
                    scanningLabel.setVisibility(View.GONE);
                    authButton.setText("AUTH");
                }




        }
        else {
            Toast.makeText(AuthActivity.this,
                    "Please type a valid public key.", Toast.LENGTH_SHORT).show();
        }

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
            Toast.makeText(AuthActivity.this,
                    "Error derivedPubKey: " + derivedPubKey, Toast.LENGTH_SHORT).show();
        }
        return returnPubKey;
    }
    private String encodeRawPubKey(String pubKey){

        String pubKeyParam = pubKey;
        String returnPubKey = "";

        if(pubKeyParam.length() == 128){
            returnPubKey = "04" + pubKey;
        }
        else if(pubKeyParam.length() == 130){
            returnPubKey = "04" + pubKey.substring(2);
        }
        else {
            Toast.makeText(AuthActivity.this,
                    "Error derivedPubKey: " + pubKey, Toast.LENGTH_SHORT).show();
        }
        return returnPubKey;
    }
}