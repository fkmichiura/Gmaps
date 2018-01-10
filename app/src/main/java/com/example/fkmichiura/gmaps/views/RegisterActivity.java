package com.example.fkmichiura.gmaps.views;

import android.content.Intent;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    private EditText mRegEmail;
    private EditText mRegPassword;

    private ProgressBar progressBar;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mRegEmail = findViewById(R.id.reg_email);
        mRegPassword = findViewById(R.id.reg_password);

        Button registerBtn = findViewById(R.id.reg_btn);
        registerBtn.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.reg_btn){
            signOut();
            createAccount(
                    mRegEmail.getText().toString().trim(),
                    mRegPassword.getText().toString().trim());
        }
    }

    private boolean validateForm() {
        boolean validated = true;

        String email = mRegEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mRegEmail.setError("Preencher este campo");
            validated = false;
        } else {
            mRegEmail.setError(null);
        }

        String password = mRegPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mRegPassword.setError("Preencher este campo");
            validated = false;
        } else {
            mRegPassword.setError(null);
        }
        return validated;
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }
        showProgressBar();

        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            //Envia e-mail de verificação
                            sendEmailVerification();

                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Falha de autenticação",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        hideProgressBar();
                        // [END_EXCLUDE]
                    }
                });
        // [END create_user_with_email]
    }

    private void sendEmailVerification() {
        // Disable button
        user = mAuth.getCurrentUser();
        // Send verification email
        if(user != null){
            // [START send_email_verification]
            user.sendEmailVerification()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // [START_EXCLUDE]

                            if (task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this,
                                        "E-mail de verificação enviado para " + user.getEmail(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "sendEmailVerification", task.getException());
                                Toast.makeText(RegisterActivity.this,
                                        "Falha ao enviar e-mail de verificação",
                                        Toast.LENGTH_SHORT).show();
                            }
                            // [END_EXCLUDE]
                        }
                    });
            // [END send_email_verification]
        }
    }

    private void signOut() {
        mAuth.signOut();
    }

    private void showProgressBar(){
        progressBar = findViewById(R.id.reg_progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar(){
        progressBar = findViewById(R.id.reg_progressBar);
        progressBar.setVisibility(View.GONE);
    }
}
