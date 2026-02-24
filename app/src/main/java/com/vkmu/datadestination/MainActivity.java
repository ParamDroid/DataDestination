package com.vkmu.datadestination;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vkmu.datadestination.vpn.VpnServiceImpl;

public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> prepareVpn());
        btnStop.setOnClickListener(v -> stopVpn());
    }

    private void prepareVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            startVpn();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startVpn();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startVpn() {
        Intent intent = new Intent(this, VpnServiceImpl.class);
        intent.setAction(VpnServiceImpl.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
    }

    private void stopVpn() {
        Intent intent = new Intent(this, VpnServiceImpl.class);
        intent.setAction(VpnServiceImpl.ACTION_STOP);
        startService(intent);
    }
}