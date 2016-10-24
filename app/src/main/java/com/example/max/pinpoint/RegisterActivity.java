package com.example.max.pinpoint;

/**
 * Created by Max on 10/23/2016.
 */

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class RegisterActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set View to register.xml
        setContentView(R.layout.register);

        TextView loginScreen = (TextView) findViewById(R.id.link_to_login);

        // Listening to button to login screen
        loginScreen.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // Closing registration screen
                // Switching to Login Screen/closing Register screen
                finish();
            }
        });

        TextView loginScreenComplete = (TextView) findViewById(R.id.btnRegister);

        // Listening to register button
        loginScreenComplete.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // Closing registration screen
                // Switching to Login Screen/closing Register Screen
                finish();
            }
        });

    }
}
