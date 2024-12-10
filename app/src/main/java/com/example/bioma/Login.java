package com.example.bioma;

import static com.example.bioma.MainActivity.WEB_URL;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Login extends AppCompatActivity {

    private RelativeLayout internetPopup, dimScreen;
    private Button login_btn;
    private static final String TAG = "MainActivity";
    private ImageView togglePasswordVisibility;
    private boolean isPasswordVisible = false;
    SessionManager sessionManager;
    EditText userID, Pass;
    static String photoStr, Name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        sessionManager = new SessionManager(this);

        Button retryButton, cancelButton;

        Animation popupAnimation = AnimationUtils.loadAnimation(this, R.anim.popup);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        login_btn = findViewById(R.id.login_btn);
        retryButton = findViewById(R.id.retry_button);
        cancelButton = findViewById(R.id.cancel_button);
        internetPopup = findViewById(R.id.internet_popup);
        dimScreen = findViewById(R.id.internet_popup_container);
        userID = findViewById(R.id.user_id);
        Pass = findViewById(R.id.password);
        togglePasswordVisibility = findViewById(R.id.password_toggle);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        togglePasswordVisibility.setOnClickListener(v -> {
            if (isPasswordVisible) {
                Pass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.eye_open); // Show open eye icon
            } else {
                Pass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.eye_close); // Show close eye icon
            }
            isPasswordVisible = !isPasswordVisible;
            Pass.setSelection(Pass.getText().length()); // Move cursor to end
        });

        retryButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                internetPopup.setVisibility(View.GONE);
                dimScreen.setVisibility(View.GONE);
                dimScreen.startAnimation(fadeOut);
                login_btn.setEnabled(true);
                login_btn.setText("Log In");
            } else {
                // Retry failed, show pop-up again with animation if desired
                internetPopup.setVisibility(View.VISIBLE);
                dimScreen.setVisibility(View.VISIBLE);
                internetPopup.startAnimation(popupAnimation);
                dimScreen.startAnimation(fadeIn);
            }
        });

        cancelButton.setOnClickListener(v -> {
            internetPopup.setVisibility(View.GONE);
            dimScreen.setVisibility(View.GONE);
            dimScreen.startAnimation(fadeOut);
            login_btn.setEnabled(true);
            login_btn.setText("Log In");
        });

        login_btn.setOnClickListener(v -> {
            login_btn.setEnabled(false); // Disable the button to prevent multiple clicks
            login_btn.setText("Processing...");

            if (!isNetworkAvailable()) {
                internetPopup.setVisibility(View.VISIBLE);
                dimScreen.setVisibility(View.VISIBLE);
                internetPopup.startAnimation(popupAnimation);
                dimScreen.startAnimation(fadeIn);
                login_btn.setText("Login");
                return;
            }

            String IDString = userID.getText().toString();
            String PassString = Pass.getText().toString();

            if (IDString.isEmpty() || PassString.isEmpty()) {
                Toast.makeText(this, "Enter both User ID and Password", Toast.LENGTH_SHORT).show();
                login_btn.setEnabled(true);
                login_btn.setText("Login");
                return;
            }

            CredsVerification(IDString, PassString, verified -> {
                if (verified) {
                    Toast.makeText(this, "user verified", Toast.LENGTH_SHORT).show();
                    sessionManager.setLogin(true, IDString, photoStr, Name);

                    Intent intent = new Intent(Login.this, Home.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "not verified", Toast.LENGTH_SHORT).show();
                }
                login_btn.setEnabled(true);
                login_btn.setText("Login");
            });
        });
    }

    public interface VerificationCallback {
        void VerificationCompleted(boolean verified);
    }

    private void CredsVerification(String userID, String password, VerificationCallback callback) {
        String url = WEB_URL + "?action=getCreds";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonArray = response.getJSONArray("creds");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        String id = object.getString("UserID");
                        String pass = object.getString("Password");
                        String uid = object.getString("Photo");
                        photoStr = "https://erp.psitche.ac.in/assets/img/Simages/" + uid + ".jpg";
                        Name = object.getString("Name");
                        if (id.equals(userID) && pass.equals(password)) {
                            callback.VerificationCompleted(true);
                            return;
                        } else {
                            Log.d(TAG, "lere id: " + id);
                        }
                    }
                    callback.VerificationCompleted(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(Login.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    callback.VerificationCompleted(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                volleyError.printStackTrace();
                Toast.makeText(Login.this, "Network error: " + volleyError.getMessage(), Toast.LENGTH_SHORT).show();
                login_btn.setText("Login"); // Moved inside
                callback.VerificationCompleted(false);
            }
        });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
