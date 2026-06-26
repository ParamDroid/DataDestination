package com.vkmu.datadestination;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.vkmu.datadestination.parser.FlowPacket;
import com.vkmu.datadestination.parser.GeoIPOffline;
import com.vkmu.datadestination.parser.IpAddressUtils;
import com.vkmu.datadestination.parser.PacketHub;

import java.util.*;

public class PieChartActivity extends BaseActivity {

    private PieChart pieChart;
    private final List<String> otherCountries =
            new ArrayList<>();
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

                String ip = IpAddressUtils.extractDestinationIp(p.destinationIp);
                if (ip == null) continue;

                // skip local IPs
                if (IpAddressUtils.isLocalOrPrivate(ip)) {
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

            List<Map.Entry<String,Integer>> sorted =
                    new ArrayList<>(countryCount.entrySet());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sorted.sort((a,b) ->
                        Integer.compare(
                                b.getValue(),
                                a.getValue()
                        ));
            }

            entries = new ArrayList<>();

            int others = 0;

            int MAX_VISIBLE = 5;

            for (int i = 0; i < sorted.size(); i++) {

                Map.Entry<String,Integer> entry =
                        sorted.get(i);

                if (i < MAX_VISIBLE) {

                    entries.add(
                            new PieEntry(
                                    entry.getValue(),
                                    entry.getKey()
                            )
                    );

                } else {

                    others += entry.getValue();

                    otherCountries.add(
                            entry.getKey()
                                    + " : "
                                    + entry.getValue()
                    );
                }
            }

            if (others > 0) {

                entries.add(
                        new PieEntry(
                                others,
                                "Others"
                        )
                );
            }

            PieDataSet dataSet = new PieDataSet(entries, "Traffic by Country");

            dataSet.setColors(
                    color(R.color.dd_accent),
                    color(R.color.dd_warning),
                    color(R.color.dd_chart_blue),
                    color(R.color.dd_danger),
                    color(R.color.dd_success)
            );

            dataSet.setValueTextSize(14f);
            dataSet.setValueTextColor(color(R.color.dd_text_primary));

            PieData data = new PieData(dataSet);

            runOnUiThread(() -> {
                pieChart.setData(data);
                pieChart.setOnChartValueSelectedListener(
                        new OnChartValueSelectedListener() {

                            @Override
                            public void onValueSelected(
                                    Entry e,
                                    Highlight h
                            ) {

                                PieEntry entry =
                                        (PieEntry) e;

                                if (!"Others".equals(
                                        entry.getLabel()
                                )) {
                                    return;
                                }

                                StringBuilder builder =
                                        new StringBuilder();

                                for (String s : otherCountries) {

                                    builder.append(s)
                                            .append("\n");
                                }

                                new AlertDialog.Builder(
                                        PieChartActivity.this
                                )
                                        .setTitle("Other Countries")
                                        .setMessage(
                                                builder.toString()
                                        )
                                        .setPositiveButton(
                                                "Close",
                                                null
                                        )
                                        .show();
                            }

                            @Override
                            public void onNothingSelected() {

                            }
                        });
                pieChart.getDescription().setEnabled(false);
                pieChart.setNoDataText("No destination data yet");
                pieChart.setNoDataTextColor(color(R.color.dd_text_secondary));
                pieChart.setHoleColor(color(R.color.dd_surface));
                pieChart.setCenterText("Countries");
                pieChart.setCenterTextColor(color(R.color.dd_text_primary));
                pieChart.setCenterTextSize(15f);
                pieChart.setUsePercentValues(true);
                pieChart.setEntryLabelColor(color(R.color.dd_text_primary));
                pieChart.setEntryLabelTextSize(12f);
                pieChart.getLegend().setTextColor(color(R.color.dd_text_secondary));
                pieChart.getLegend().setTextSize(12f);

                pieChart.animateY(1000);
                pieChart.invalidate();
            });

        }).start();
    }

    private int color(int resId) {
        return ContextCompat.getColor(this, resId);
    }
}
