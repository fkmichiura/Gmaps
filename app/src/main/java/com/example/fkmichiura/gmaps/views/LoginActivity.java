package com.example.fkmichiura.gmaps.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.fkmichiura.gmaps.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;

    private EditText mEmailField;
    private EditText mPasswordField;

    private ProgressBar progressBar;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailField = findViewById(R.id.tv_email);
        mPasswordField = findViewById(R.id.tv_password);

        Button signInBtn = findViewById(R.id.signin_btn);
        Button registerBtn = findViewById(R.id.register_btn);

        signInBtn.setOnClickListener(this);
        registerBtn.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.signin_btn:
                signIn(
                    mEmailField.getText().toString().trim(),
                    mPasswordField.getText().toString().trim());
                break;

            case R.id.register_btn:
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);

        if (!validateForm()) {
            return;
        }
        showProgressBar();

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");

                            Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Falha de autenticação",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            Log.i("LoginActivity", "Error: " + task.getException());
                            hideProgressBar();
                        }
                        // [END_EXCLUDE]

                        hideProgressBar();
                    }
                });
        // [END sign_in_with_email]
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Preencher este campo");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Preencher este campo");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }
        return valid;
    }

    private void showProgressBar(){
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }
}
