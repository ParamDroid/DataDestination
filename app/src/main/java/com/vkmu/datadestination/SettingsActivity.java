package com.vkmu.datadestination;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Switch;

import com.vkmu.datadestination.debug.DebugLogger;

public class SettingsActivity extends BaseActivity {

    private static final String PREFS = "app_settings";
    private SharedPreferences prefs;

    private Switch switchCustomDns;
    private EditText editCustomDns;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupDrawer();

        //api key prefs
        EditText apiInput = findViewById(R.id.editApiKey);
        Switch debugSwitch = findViewById(R.id.switchDebug);

        //dns prefs
        switchCustomDns = findViewById(R.id.switchCustomDns);
        editCustomDns = findViewById(R.id.editCustomDns);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);


        switchCustomDns.setOnCheckedChangeListener((buttonView, checked) -> {
            editCustomDns.setEnabled(checked);
            prefs.edit()
                    .putBoolean(
                            "custom_dns_enabled",
                            checked
                    )
                    .apply();
        });

        //To hide the text box of dns
        editCustomDns.setEnabled(
                switchCustomDns.isChecked()
        );

        // load saved values
        apiInput.setText(prefs.getString("abuse_api_key", ""));
        debugSwitch.setChecked(prefs.getBoolean("debug_mode", true));

        //For dns pref loading
        switchCustomDns.setChecked(
                prefs.getBoolean("custom_dns_enabled", false)
        );

        editCustomDns.setText(
                prefs.getString(
                        "custom_dns",
                        "1.1.1.1"
                )
        );

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
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch desktopSwitch = findViewById(R.id.switchDesktop);

        //to save the dns settings
        editCustomDns.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {

                prefs.edit()
                        .putString(
                                "custom_dns",
                                s.toString().trim()
                        )
                        .apply();
            }
        });
// load saved values
        ipInput.setText(prefs.getString("desktop_ip", ""));
        portInput.setText(String.valueOf(prefs.getInt("desktop_port", 9000)));
        desktopSwitch.setChecked(prefs.getBoolean("desktop_enabled", false));

// save IP
        ipInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit()
                        .putString("desktop_ip", s.toString().trim())
                        .apply();
            }
        });

// save port
        portInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int port = Integer.parseInt(s.toString());
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
            DebugLogger.setDebugEnabled(val);
            prefs.edit().putBoolean("debug_mode", val).apply();
        });
    }
}
