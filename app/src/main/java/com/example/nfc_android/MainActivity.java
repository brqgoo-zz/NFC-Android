package com.example.nfc_android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.app.Activity;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.Web3j;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.jar.Attributes.Name;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText descriptionTextBox;
    Button writeButton;
    Context context;
    Credentials credentials;
    String tagDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Write to NFC tag");
        context = this;

        descriptionTextBox = findViewById(R.id.descriptionTextBox);
        writeButton = findViewById(R.id.writeButton);
        writeButton.setOnClickListener(MainActivity.this);

        Web3j web3 = Web3j.build(new HttpService());


        tagDescription = "";

        String pk = "dc5a5cb29e7761943b98ca663084c07ad27407fb7dd23cc32a5789e1dc8c8621";

        credentials = Credentials.create(pk);



        System.out.println(" getAddress " + credentials.getAddress());






    }

    @Override
    public void onClick(View v) {

        hideKeyboard(MainActivity.this);

        tagDescription = descriptionTextBox.getText().toString();

        if(tagDescription.length()>=1){

        byte[] tagUID = fromHexString("04c80e62ed4c84");
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
                hashedPrefixedMessage, credentials.getEcKeyPair(), false);


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

            Toast.makeText(MainActivity.this,
                    "Writing successful", Toast.LENGTH_SHORT).show();

    }
        else {
            Toast.makeText(MainActivity.this,
                    "Please type something first.", Toast.LENGTH_SHORT).show();
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
}