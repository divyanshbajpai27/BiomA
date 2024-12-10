package com.example.bioma;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;

public class Home extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;
    LinearLayout scan;
    Button go, logout;
    EditText code;
    SessionManager sessionManager;
    ImageView pfp;
    TextView name;
    private Uri photu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        scan = findViewById(R.id.linearLayout);
        go = findViewById(R.id.button);
        code = findViewById(R.id.code);
        logout = findViewById(R.id.logout);
        pfp = findViewById(R.id.person);
        name = findViewById(R.id.name);

        String photoStr = sessionManager.getPhotoStr();
        if (photoStr != null) {
            photu = Uri.parse(photoStr);
        }

        name.setText(sessionManager.getUserName());

        if (photu != null) {
            Log.d("photu", photu.toString());
            Picasso.get()
                    .load(photu)
                    .into(pfp);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set onClickListener for the scan button
        scan.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
            } else {
                openScannerActivity();
            }
        });

        // Set onClickListener for the go button
        go.setOnClickListener(view -> {
            String key = code.getText().toString();
            Intent intent = new Intent(this, Fingerprint.class);
            intent.putExtra("key", key);
            startActivity(intent);
        });

        logout.setOnClickListener(view -> {
            sessionManager.logout(); // Clear session data
            Intent intent = new Intent(Home.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
            startActivity(intent);
        });
    }

    // Request camera permission
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "Camera permission is needed to scan.", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    // Open the Scanner activity with location permission check
    private void openScannerActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else if (!isLocationEnabled()) {
            promptEnableLocation();
        } else {
            Intent intent = new Intent(this, Scanner.class);
            startActivity(intent);
        }
    }

    // Request location permission
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Location permission is required to scan.", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
    }

    // Check if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Prompt user to enable location
    private void promptEnableLocation() {
        Toast.makeText(this, "Please enable location services.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    // Handle the result of permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openScannerActivity();
            } else {
                Toast.makeText(this, "Camera permission denied. Cannot scan.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openScannerActivity();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot scan.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
