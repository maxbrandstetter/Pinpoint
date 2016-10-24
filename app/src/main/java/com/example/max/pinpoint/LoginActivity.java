package com.example.max.pinpoint;

/**
 * Created by Max on 10/23/2016.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class LoginActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setting default screen to login.xml
        setContentView(R.layout.login);

        TextView registerScreen = (TextView) findViewById(R.id.link_to_register);

        // Listening to register new account link
        registerScreen.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // Switching to Register screen
                Intent i = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(i);
            }
        });

        TextView loginSuccess = (TextView) findViewById(R.id.btnLogin);

        // Listening to login button
        loginSuccess.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // Switching to Home screen
                Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(i);
            }
        });
    }
}
