package com.vkmu.datadestination;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.vkmu.datadestination.debug.DebugLogger;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.vkmu.datadestination.vpn.VpnServiceImpl;

public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView debugBox = findViewById(R.id.txtDebug);
        DebugLogger.attach(debugBox);
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
                    if (item.getItemId() == R.id.nav_about) {
                        startActivity(new Intent(this, AboutActivity.class));
                        drawerLayout.closeDrawers();
                        return true;
                    }
            return false;
        });

        // image button toggle logic
        ImageButton btn = findViewById(R.id.btnToggle);
        btn.setOnClickListener(v -> {

            boolean newState = !v.isSelected();
            v.setSelected(newState);

            if (newState) {
                prepareVpn();
                Toast.makeText(this, "VPN ACTIVE", Toast.LENGTH_SHORT).show();
            } else {
                stopVpn();
                Toast.makeText(this, "VPN INACTIVE", Toast.LENGTH_SHORT).show();
            }
        });

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