package com.vkmu.datadestination;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setupDrawer();
    }
}