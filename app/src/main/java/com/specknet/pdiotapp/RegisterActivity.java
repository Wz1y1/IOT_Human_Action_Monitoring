package com.specknet.pdiotapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private Button buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.Register_EmailAddress);
        editTextPassword = findViewById(R.id.Register_Password);
        buttonRegister = findViewById(R.id.re_Register_button);
        buttonBack = findViewById(R.id.register_back);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the activity, which will go back to the previous activity (LoginActivity)
                finish();
            }
        });
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Check if the email is valid
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for a valid password (at least some logic, you can make this more complex)
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            Toast.makeText(this, "Password must be more than 5 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        createUser(email, password);
    }

    private void createUser(String email, String password) {
        // This is where you would integrate with your backend or local database
        UserManager userManager = new UserManager(this);
        userManager.createUser(email, password);

        // If the user creation is successful
        boolean isUserCreated = true; // This should be the result of your user creation logic

        if (isUserCreated) {
            Toast.makeText(this, "User created successfully", Toast.LENGTH_SHORT).show();
            // Optionally, you can navigate back to the login screen
            finish(); // Ends this activity and goes back to the previous one (LoginActivity)
        } else {
            Toast.makeText(this, "Failed to create user", Toast.LENGTH_SHORT).show();
        }
    }

    public static class UserManager {

        private static final String PREFERENCES_FILE_KEY = "com.example.android.directboot.USER_CREDENTIALS";
        private SharedPreferences sharedPreferences;

        public UserManager(Context context) {
            sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
        }

        public void createUser(String email, String password) {
            // Never store plain text passwords; this is for demonstration purposes only.
            // You should encrypt the password before storing it.
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(email, password);
            editor.apply();
        }

        public boolean validateUser(String email, String password) {
            // This will return the password if it exists, or null if not.
            String storedPassword = sharedPreferences.getString(email, null);
            return storedPassword != null && storedPassword.equals(password);
        }
    }
}
