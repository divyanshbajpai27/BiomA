package com.example.bioma;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout internetPopup, dimScreen;
    private Button login_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button retryButton, cancelButton;

        Animation popupAnimation = AnimationUtils.loadAnimation(this, R.anim.popup);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        login_btn = findViewById(R.id.login_btn);
        retryButton = findViewById(R.id.retry_button);
        cancelButton = findViewById(R.id.cancel_button);
        internetPopup = findViewById(R.id.internet_popup);
        dimScreen = findViewById(R.id.internet_popup_container);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}