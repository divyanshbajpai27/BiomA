package com.example.bioma;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    // Shared preferences file name
    private static final String PREF_NAME = "LoginSession";
    private static final String IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";

    // Constructor
    public SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Method to set login status and user ID
    public void setLogin(boolean isLoggedIn, String userId) {
        editor.putBoolean(IS_LOGGED_IN, isLoggedIn); // Save the login status
        editor.putString(KEY_USER_ID, userId); // Save the user ID
        editor.commit(); // Commit changes
    }

    // Method to get login status
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(IS_LOGGED_IN, false); // Return the login status
    }

    // Method to get user ID
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null); // Return the user ID
    }

    // Method to clear login status and user ID
    public void logout() {
        editor.clear(); // Clear all data from SharedPreferences
        editor.commit(); // Commit changes
    }
}