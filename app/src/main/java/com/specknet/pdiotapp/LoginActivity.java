package com.specknet.pdiotapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextTextPassword);
        Button buttonLogin = findViewById(R.id.login_button);
        // You've already declared this button, make sure you do not redeclare it inside onCreate()
        Button buttonRegister = findViewById(R.id.register_button); // Reuse the field instead of declaring a new local variable


        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Set the OnClickListener for the register button
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // TODO: Implement your authentication logic here.
        // If login is successful, start a new Activity, else show an error
        // Validate the input first
        if (!isValidEmail(email)) {
            // Show an error or alert
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            // Show an error or alert
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use UserManager to validate the credentials
        RegisterActivity.UserManager userManager = new RegisterActivity.UserManager(this);
        boolean isValidUser = userManager.validateUser(email, password);

        if (isValidUser) {
            // Credentials are valid, start the new Activity
            startHomeActivity();
        } else {
            // Credentials are invalid, show an error
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        // Implement your own password validation logic here
        return password.length() >= 6;
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, MainActivity.class); // Replace with your target Activity
        startActivity(intent);
        finish(); // Close the LoginActivity once the HomeActivity is started
    }
}