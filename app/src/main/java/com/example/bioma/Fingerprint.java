package com.example.bioma;

import static com.example.bioma.MainActivity.WEB_URL;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class Fingerprint extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;
    private TextView fingerprintStatus;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String TAG = "MainActivity";
    SessionManager sessionManager;

    private String scannedKey;
    private String classroom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        fingerprintStatus = findViewById(R.id.fingerprint_status);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get data from Intent
        Intent intent = getIntent();
        scannedKey = intent.getStringExtra("key");
        classroom = intent.getStringExtra("classroom");

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocationAndCheckProximity();
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                fingerprintStatus.setText("Authentication error: " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                fingerprintStatus.setText("Authentication succeeded!");
                // Handle successful authentication
                getLocationAndCheckProximity();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                fingerprintStatus.setText("Authentication failed. Try again.");
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Authentication")
                .setSubtitle("Verify your identity")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndCheckProximity();
            } else {
                Toast.makeText(this, "Location permission is required to check proximity",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocationAndCheckProximity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                checkProximity(location);
                            } else {
                                requestNewLocationData();
                            }
                        });
            } catch (SecurityException e) {
                Toast.makeText(this, "Location permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(Fingerprint.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        checkProximity(location);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkProximity(Location location) {
        double targetLatitude = 26.429278;
        double targetLongitude = 80.334551;

        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                targetLatitude, targetLongitude, results);
        float distanceInMeters = results[0];

        if (distanceInMeters < 1000) {
            Toast.makeText(this, "Within the specified area", Toast.LENGTH_LONG).show();
            // Both verifications completed successfully
            verifyAttend(verified -> {
                if(verified){
                    Toast.makeText(this, "Attendance Marked", Toast.LENGTH_SHORT).show();
                    markPresent();
                }
                else{
                    Toast.makeText(this, "Key match Failure", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Outside the specified area", Toast.LENGTH_LONG).show();
        }
    }

    private void markPresent() {
        sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();

        String url = WEB_URL + "?action=markAttendance&userId=" + userId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String result = response.getString("result");
                    if (result.equals("Attendance marked")) {
                        Toast.makeText(Fingerprint.this, "Attendance marked successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Fingerprint.this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(Fingerprint.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(Fingerprint.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }


    public interface VerificationCallback {
        void VerificationCompleted(boolean verified);
    }

    private void verifyAttend(VerificationCallback callback) {
        String url = WEB_URL + "?action=fetchKeysForToday";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("keys");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        String key = object.getString("Key");
                        String clas = object.getString("Class");
                        if (key.equals(scannedKey) && clas.equals(classroom)) {
                            callback.VerificationCompleted(true);
                            return;
                        } else {
                            Log.d(TAG, "lere id: " + key);
                            Log.d(TAG, "lere class: " + classroom);
                        }
                    }
                    callback.VerificationCompleted(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(Fingerprint.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    callback.VerificationCompleted(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                volleyError.printStackTrace();
                Toast.makeText(Fingerprint.this, "Network error: " + volleyError.getMessage(), Toast.LENGTH_SHORT).show();
                callback.VerificationCompleted(false);
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }
}
