package com.example.bioma;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    public static String WEB_URL="https://script.google.com/macros/s/AKfycbyJdxp0S3QDsFeOPmC3bcqm4Sfiq2fKfreFJgEW_2pMNQQpRNuxdfAwqPWIKHGSzUA/exec";
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (sessionManager.isLoggedIn()) {
            // User is logged in, navigate to HomeActivity
            Intent intent = new Intent(MainActivity.this, Home.class);
            startActivity(intent);
            finish(); // Close MainActivity
        } else {
            // User is not logged in, show login screen
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish(); // Close MainActivity
        }
    }
}