package com.qrilt.roadrunner;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    @BindView(R.id.activity_main_username_edit_text)
    EditText usernameEditText;
    @BindView(R.id.activity_main_password_edit_text)
    EditText passwordEditText;
    @BindView(R.id.activity_main_auth_button)
    Button authButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setup();
    }

    private void setup() {
        //setup auth button
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString(), password = passwordEditText.getText().toString();

                if(!username.isEmpty() && !password.isEmpty()) {
                    ParseUser newUser = new ParseUser();
                    newUser.setEmail(username);
                    newUser.setUsername(username);
                    newUser.setPassword(password);

                    newUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e == null) {
                                Toast.makeText(MainActivity.this, "Authentication Successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                                startActivity(intent);
                            }
                        }
                    });
                }
            }
        });
    }

    //override SensorEventListener methods
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("DebugK", "Accuracy Changed " + accuracy);
    }

    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {
        float[] gravity = new float[3], linear_acceleration = new float[3];
        final float alpha = 0.8f;

        //Isolate force of gravity with the low-pass filter
        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

        //Remove gravity contribution with high-pass filter
        linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
        linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
        linear_acceleration[2] = sensorEvent.values[2] - gravity[2];

        Log.d("DebugK", "Linear Acceleration " + linear_acceleration[0] + "," + linear_acceleration[1] + "," + linear_acceleration[2]);
    }
}
