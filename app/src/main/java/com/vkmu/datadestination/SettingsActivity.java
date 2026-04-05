package com.vkmu.datadestination;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Switch;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS = "app_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupDrawer();

        EditText apiInput = findViewById(R.id.editApiKey);
        Switch debugSwitch = findViewById(R.id.switchDebug);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // load saved values
        apiInput.setText(prefs.getString("abuse_api_key", ""));
        debugSwitch.setChecked(prefs.getBoolean("debug_mode", true));

        // save API key
        apiInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String key = s.toString().trim();
                Log.d("SettingsActivity", "Saving API Key: " + key);
                prefs.edit()
                        .putString("abuse_api_key", key)
                        .apply();
            }
        });
        EditText ipInput = findViewById(R.id.editDesktopIp);
        EditText portInput = findViewById(R.id.editDesktopPort);
        Switch desktopSwitch = findViewById(R.id.switchDesktop);



// load saved values
        ipInput.setText(prefs.getString("desktop_ip", ""));
        portInput.setText(String.valueOf(prefs.getInt("desktop_port", 9000)));
        desktopSwitch.setChecked(prefs.getBoolean("desktop_enabled", false));

// save IP
        ipInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                prefs.edit()
                        .putString("desktop_ip", ipInput.getText().toString().trim())
                        .apply();
            }
        });

// save port
        portInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int port = Integer.parseInt(portInput.getText().toString());
                    prefs.edit().putInt("desktop_port", port).apply();
                } catch (Exception ignored) {}
            }
        });

// save toggle
        desktopSwitch.setOnCheckedChangeListener((btn, val) -> {
            prefs.edit().putBoolean("desktop_enabled", val).apply();
        });
        // save debug mode
        debugSwitch.setOnCheckedChangeListener((btn, val) -> {
            prefs.edit().putBoolean("debug_mode", val).apply();
        });
    }
}