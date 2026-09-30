package com.example.labapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText editUsername, editPassword, editNote;
    private TextView textStatus;
    private SQLiteDatabase db;
    private SharedPreferences notePrefs;

    // VULN  (Securing Components) lives in AndroidManifest.xml, not here —
    // see AndroidManifest_Insecure.xml (WelcomeActivity android:exported="true").

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editNote = findViewById(R.id.editNote);
        textStatus = findViewById(R.id.textStatus);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);
        Button btnShowData = findViewById(R.id.btnShowData);

        db = openOrCreateDatabase("app_data.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS users (username TEXT, password TEXT)");
        db.execSQL("INSERT INTO users (username, password) SELECT 'admin@example.com', 'supersecret' WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin@example.com')");

        notePrefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);

        btnLogin.setOnClickListener(v -> processLogin());
        btnSaveNote.setOnClickListener(v -> saveNote());
        btnShowData.setOnClickListener(v -> showNote());
    }

    private void processLogin() {
        String user = editUsername.getText().toString();
        String pass = editPassword.getText().toString();

        // EXTRA (Secure Logging, bonus — not one of the 6): logs the raw password.
        Log.d("LOGIN_ATTEMPT", "Attempting login with password: " + pass);

        // ===== VULN : INPUT VALIDATION — MISSING =====
        // (nothing here — any text in either field is accepted as-is)

        // ===== VULN : SQL INJECTION =====
        String query = "SELECT * FROM users WHERE username = '" + user + "' AND password = '" + pass + "'";
        textStatus.setText("Executed Query:\n" + query + "\n");
        Cursor cursor = db.rawQuery(query, null);
        boolean loginMatched = cursor.getCount() > 0;
        cursor.close();

        // ===== VULN : SECURE STORAGE — MISSING (plain SharedPreferences) =====
        SharedPreferences prefs = getSharedPreferences("standard_prefs", MODE_PRIVATE);
        prefs.edit().putString("saved_user", user).putString("saved_pass", pass).apply();

        if (loginMatched) {
            textStatus.append("\nStatus: Login Bypassed/Successful!");
            startActivity(new Intent(MainActivity.this, WelcomeActivity.class));
        } else {
            textStatus.append("\nStatus: Login Failed");
        }
    }

    private void saveNote() {
        String note = editNote.getText().toString();

        // ===== VULN : ERROR HANDLING — MISSING =====
        // No try/catch anywhere in this app: an empty note makes charAt(0)
        // throw StringIndexOutOfBoundsException, which is never caught, so
        // the app crashes and closes.
        char firstChar = note.charAt(0);

        // ===== VULN : DATA ENCRYPTION — MISSING (stored as plain text) =====
        notePrefs.edit().putString("note_value", note).apply();

        textStatus.setText("Saved Plaintext Note: " + note + "\n");
    }

    private void showNote() {
        String stored = notePrefs.getString("note_value", null);
        if (stored == null) {
            textStatus.setText("No note saved yet.");
        } else {
            textStatus.setText("Stored Note (Plaintext): \n" + stored);
        }
    }
}