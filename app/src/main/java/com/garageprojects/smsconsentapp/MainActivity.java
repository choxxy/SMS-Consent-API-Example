package com.garageprojects.smsconsentapp;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity {

    private final String TAG =  MainActivity.class.getSimpleName();
    private static final int SMS_CONSENT_REQUEST = 2;

    EditText verificationCode;
    Button startTask;

    // Set to an unused request code
    private final BroadcastReceiver smsVerificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                switch (smsRetrieverStatus.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        // Get consent intent
                        Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                        try {
                            /*Start activity to show consent dialog to user within
                             *5 minutes, otherwise you'll receive another TIMEOUT intent
                             */
                            startActivityForResult(consentIntent, SMS_CONSENT_REQUEST);
                        } catch (ActivityNotFoundException e) {
                            // Handle the exception
                        }
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        // Time out occurred, handle the error.
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        registerReceiver(smsVerificationReceiver, intentFilter);

        verificationCode = findViewById(R.id.verification_code);
        startTask = findViewById(R.id.start_task);

        //In this demo we will use a button click event to trigger listening for SMS User Consent broadcasts
        startTask.setOnClickListener(v -> smsTask());
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SMS_CONSENT_REQUEST:
                if (resultCode == RESULT_OK) {
                    // Get SMS message content
                    String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                    // Extract one-time code from the message and complete verification
                    String oneTimeCode = parseOneTimeCode(message);
                    //for this demo we will display it instead
                    verificationCode.setText(oneTimeCode);

                } else {
                    // Consent canceled, handle the error
                }
                break;
        }
    }


    private String parseOneTimeCode(String message) {
        //simple number extractor
        return message.replaceAll("[^0-9]", "");
    }


    private void smsTask() {
        //Start listening for SMS User Consent broadcasts from senderPhoneNumber
        //The sender number being used was configured in my emulator, you can use your own number
        Task<Void> task = SmsRetriever.getClient(this).startSmsUserConsent("+254700123123");

        task.addOnCompleteListener(listener -> {
            if (listener.isSuccessful()) {
                // Task completed successfully
                Log.d(TAG, "Success");

            } else {
                // Task failed with an exception
                Exception exception = listener.getException();
                exception.printStackTrace();
            }
        });

    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //to prevent IntentReceiver leakage unregister
        unregisterReceiver(smsVerificationReceiver);
    }

}
