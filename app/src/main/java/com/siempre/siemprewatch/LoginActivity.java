package com.siempre.siemprewatch;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.HashMap;

public class LoginActivity extends WearableActivity {
    private static final String TAG = "LoginActivity";
    private static final String AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";

    Context context;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor prefEdit;

    EditText emailText;
    EditText passwordText;
    Button loginButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        context = getApplicationContext();
        sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        prefEdit = sharedPreferences.edit();

        if (!sharedPreferences.getString("userId", "").equals("")
            && !sharedPreferences.getString("refreshToken", "").equals("")) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }

        emailText = findViewById(R.id.emailText);
        passwordText = findViewById(R.id.passwordText);
        loginButton = findViewById(R.id.loginButton);

        if (!sharedPreferences.getString("email", "").equals("")) {
            emailText.setText(sharedPreferences.getString("email", ""));
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = emailText.getText().toString().trim();
                final String password = passwordText.getText().toString().trim();

                JsonObject json = new JsonObject();
                json.addProperty("email", email);
                json.addProperty("password", password);
                json.addProperty("returnSecureToken", true);

                Ion.with(context)
                    .load(AUTH_URL + Constants.API_KEY)
                    .setJsonObjectBody(json)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            if (e != null) {
                                Log.d(TAG, e.toString());
                                Log.d(TAG, e.getMessage());
                                Toast.makeText(LoginActivity.this, "An error occurred", Toast.LENGTH_LONG).show();
                            } else {
                                if (result.has("error")) {
                                    String errorMessage = Utilities.removeQuotes(result.getAsJsonObject("error").get("message").toString());
                                    if (errorMessage.equals("EMAIL_NOT_FOUND")) {
                                        Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_LONG).show();
                                    } else if (errorMessage.equals("INVALID_PASSWORD")) {
                                        Toast.makeText(LoginActivity.this, "User/password does not match", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "An error occurred", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Log.d(TAG, result.toString());
                                    String resultId = Utilities.removeQuotes(result.get("localId").toString());
                                    String refreshToken = Utilities.removeQuotes(result.get("refreshToken").toString());
                                    prefEdit.putString("email", email);
                                    prefEdit.putString("userId", resultId);
                                    prefEdit.putString("refreshToken", refreshToken);
                                    prefEdit.commit();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                }
                            }
                        }
                    });
                }
            });
        }
}
