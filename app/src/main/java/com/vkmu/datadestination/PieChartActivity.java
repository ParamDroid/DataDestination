package com.vkmu.datadestination;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.GeoIPOffline;
import com.vkmu.datadestination.parser.PacketHub;

import java.util.*;

public class PieChartActivity extends BaseActivity {

    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piechart);
        setupDrawer();

        pieChart = findViewById(R.id.pieChart);

        loadChart();
    }

    private void loadChart() {

        new Thread(() -> {

            Map<String, Integer> countryCount = new HashMap<>();

            for (FlowPacket p : PacketHub.getPackets()) {

                String ip = p.destinationIp;
                if (ip == null) continue;

                // extract destination
                if (ip.contains("->")) {
                    ip = ip.split("->")[1].trim();
                }

                // remove port
                if (ip.contains(":") && ip.contains(".")) {
                    ip = ip.substring(0, ip.indexOf(":"));
                }

                // skip local IPs
                if (ip.startsWith("10.") ||
                        ip.startsWith("192.168.") ||
                        ip.startsWith("172.") ||
                        ip.startsWith("fd") ||
                        ip.startsWith("fe80")) {
                    continue;
                }

                String country = GeoIPOffline.resolve(ip);

                if (country.equals("Unknown") || country.equals("Loading...")) continue;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    countryCount.put(country,
                            countryCount.getOrDefault(country, 0) + 1);
                }
            }

            List<PieEntry> entries = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : countryCount.entrySet()) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }

            PieDataSet dataSet = new PieDataSet(entries, "Traffic by Country");

            dataSet.setColors(
                    ColorTemplate.MATERIAL_COLORS
            );

            dataSet.setValueTextSize(14f);
            dataSet.setValueTextColor(android.graphics.Color.WHITE);

            PieData data = new PieData(dataSet);

            runOnUiThread(() -> {
                pieChart.setData(data);

                pieChart.getDescription().setEnabled(false);
                pieChart.setUsePercentValues(true);
                pieChart.setEntryLabelColor(android.graphics.Color.BLACK);
                pieChart.setEntryLabelTextSize(12f);

                pieChart.animateY(1000);
                pieChart.invalidate();
            });

        }).start();
    }
}