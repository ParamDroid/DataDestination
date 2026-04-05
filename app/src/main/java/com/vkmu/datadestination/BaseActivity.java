package com.vkmu.datadestination;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.ActionBarDrawerToggle;
public class BaseActivity extends AppCompatActivity {

    protected void setupDrawer() {
        android.util.Log.d("DrawerDebug", "setupDrawer() called in " + getClass().getSimpleName());

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            android.util.Log.d("DrawerDebug", "Drawer item clicked: " + item.getItemId());
            
            Intent intent = null;
            if (item.getItemId() == R.id.nav_geoip) {
                if (!(this instanceof GeoIpActivity)) intent = new Intent(this, GeoIpActivity.class);
            }
            else if (item.getItemId() == R.id.nav_about) {
                if (!(this instanceof AboutActivity)) intent = new Intent(this, AboutActivity.class);
            }
            else if (item.getItemId() == R.id.nav_pie) {
                if (!(this instanceof PieChartActivity)) intent = new Intent(this, PieChartActivity.class);
            }
            else if (item.getItemId() == R.id.nav_settings) {
                if (!(this instanceof SettingsActivity)) intent = new Intent(this, SettingsActivity.class);
            }
            else if (item.getItemId() == R.id.nav_main) {
                if (!(this instanceof MainActivity)) intent = new Intent(this, MainActivity.class);
            }
            else if (item.getItemId() == R.id.nav_abuse) {
                if (!(this instanceof AbuseActivity)) intent = new Intent(this, AbuseActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }
}