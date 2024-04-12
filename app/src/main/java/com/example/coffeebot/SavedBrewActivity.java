package com.example.coffeebot;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SavedBrewActivity extends AppCompatActivity {
    private Spinner presetsSpinner;
    private Button scheduleTimeButton, brewNowButton, deletePresetButton;
    private ArrayAdapter<String> adapter;
    private List<String> presetNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savedbrew);

        presetsSpinner = findViewById(R.id.presetsSpinner);
        scheduleTimeButton = findViewById(R.id.scheduleTimeButton);
        brewNowButton = findViewById(R.id.brewNowButton);
        deletePresetButton = findViewById(R.id.deletePresetButton);

        loadPresets();
        setupButtonListeners();
    }

    private void loadPresets() {
        SharedPreferences prefs = getSharedPreferences("CoffeePresets", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        presetNames = new ArrayList<>(allEntries.keySet());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, presetNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetsSpinner.setAdapter(adapter);

        presetsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // This ensures that whenever a new item is selected, the correct preset data is ready for use.
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected if necessary
            }
        });
    }

    private void setupButtonListeners() {
        scheduleTimeButton.setOnClickListener(v -> scheduleBrew(true));
        brewNowButton.setOnClickListener(v -> scheduleBrew(false));
        deletePresetButton.setOnClickListener(v -> deleteSelectedPreset());
    }

    private void scheduleBrew(boolean schedule) {
        String presetName = (String) presetsSpinner.getSelectedItem();
        if (presetName != null && !presetName.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("CoffeePresets", MODE_PRIVATE);
            String presetData = prefs.getString(presetName, "");
            if (!presetData.isEmpty()) {
                if (schedule) {
                    showTimePickerDialog(presetData);
                } else {
                    sendGetRequestToPi(presetData, getCurrentTimeForImmediateRequest());
                }
            } else {
                Toast.makeText(this, "Preset data not found.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No preset selected.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendGetRequestToPi(String presetData, String time) {
        String[] parts = presetData.split(";");
        String size = parts[0];
        String caramel = parts[1];
        String vanilla = parts[2];

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.121.4.204:5000/endpoint?caramel=" + caramel + "&vanilla=" + vanilla + "&size=" + size + "&time=" + time;

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SavedBrewActivity.this, "Failed to connect to the server.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(SavedBrewActivity.this, "Server error: " + response.message(), Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(SavedBrewActivity.this, "Brew scheduled successfully!", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void deleteSelectedPreset() {
        String selectedPreset = (String) presetsSpinner.getSelectedItem();
        if (selectedPreset != null && !selectedPreset.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("CoffeePresets", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(selectedPreset);
            editor.apply();

            Toast.makeText(this, "Preset deleted: " + selectedPreset, Toast.LENGTH_SHORT).show();

            // Remove from the adapter and update the spinner
            presetNames.remove(selectedPreset);
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "No preset selected or preset list is empty.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentTimeForImmediateRequest() {
        Calendar now = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    private void showTimePickerDialog(String presetData) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfHour) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                    sendGetRequestToPi(presetData, formattedTime);
                },
                hour,
                minute,
                true // Use true here for 24-hour mode if required
        );

        timePickerDialog.setTitle("Select Time for Brewing");
        timePickerDialog.show();
    }
}