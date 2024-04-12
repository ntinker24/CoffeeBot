package com.example.coffeebot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.app.TimePickerDialog;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NewBrewActivity extends AppCompatActivity {

//    private TextView responseTextView;
    private SeekBar caramelSlider, vanillaSlider;
    private TextView caramelText, vanillaText;
    private RadioGroup coffeeSizeRadioGroup;
    private ImageView resultImageView;
    private Button timeButton;
    private String selectedTime = "";
    private MediaPlayer mediaPlayer;
    private CheckBox savePresetCheckBox;
    private EditText presetNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newbrew);

//        responseTextView = findViewById(R.id.responseTextView);
        caramelSlider = findViewById(R.id.caramelSlider);
        vanillaSlider = findViewById(R.id.vanillaSlider);
        caramelText = findViewById(R.id.caramelText);
        vanillaText = findViewById(R.id.vanillaText);
        coffeeSizeRadioGroup = findViewById(R.id.coffeeSizeRadioGroup);
        resultImageView = findViewById(R.id.resultImageView);
        timeButton = findViewById(R.id.timeButton);
        Button connectButton = findViewById(R.id.connectButton);
        savePresetCheckBox = findViewById(R.id.savePresetCheckBox);
        presetNameEditText = findViewById(R.id.presetNameEditText);
        selectedTime = "";

        savePresetCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                presetNameEditText.setVisibility(View.VISIBLE);
            } else {
                presetNameEditText.setVisibility(View.GONE);
            }
        });

        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(NewBrewActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        // Schedule the request for the selected time
                        scheduleGetRequestForSelectedTime(selectedTime);
                        // Navigate back immediately after scheduling
                        navigateBackToMainActivity();
                    }
                }, hour, minute, true);
                timePickerDialog.show();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send the request immediately
                sendGetRequestToPi();

                // Check if the preset should be saved
                if (savePresetCheckBox.isChecked()) {
                    String presetName = presetNameEditText.getText().toString().trim();
                    if (!presetName.isEmpty()) {
                        savePreset(presetName);
                    } else {
                        Toast.makeText(NewBrewActivity.this, "Please enter a preset name", Toast.LENGTH_SHORT).show();
                    }
                }
                
                // Navigate back immediately
                navigateBackToMainActivity();
            }
        });

        RadioGroup coffeeSizeRadioGroup = findViewById(R.id.coffeeSizeRadioGroup);
        coffeeSizeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.eightOzRadioButton) {
                    Log.d("CoffeeSize", "8 oz selected");
                } else if (checkedId == R.id.tenOzRadioButton) {
                    Log.d("CoffeeSize", "10 oz selected");
                } else if (checkedId == R.id.twelveOzRadioButton) {
                    Log.d("CoffeeSize", "12 oz selected");
                }
            }
        });

        caramelSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                caramelText.setText("Caramel: " + progress);
                if (progress == 10) {
                    resultImageView.setVisibility(View.VISIBLE);
                    resultImageView.setImageResource(R.drawable.laughingcat);
                    playSound();
                } else {
                    resultImageView.setVisibility(View.GONE);
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null; // Reset mediaPlayer
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        vanillaSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                vanillaText.setText("Vanilla: " + progress);
                if (progress == 10) {
                    resultImageView.setVisibility(View.VISIBLE);
                    resultImageView.setImageResource(R.drawable.laughingcat);
                    playSound();
                } else {
                    resultImageView.setVisibility(View.GONE);
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null; // Reset mediaPlayer
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void playSound() {
        // Stop and release any existing MediaPlayer instance before creating a new one
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.catlaugh);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null; // Set mediaPlayer to null after releasing
        });
        mediaPlayer.start();
    }

    private void sendGetRequestToPi() {
        // Create a new OkHttpClient
        OkHttpClient client = new OkHttpClient();

        // Retrieve the current values of the sliders
        int caramelValue = caramelSlider.getProgress();
        int vanillaValue = vanillaSlider.getProgress();

        // Retrieve the selected coffee size
        int selectedSizeId = coffeeSizeRadioGroup.getCheckedRadioButtonId();
        String coffeeSize = ""; // Default to empty or some default value
        if (selectedSizeId == R.id.eightOzRadioButton) {
            coffeeSize = "8";
        } else if (selectedSizeId == R.id.tenOzRadioButton) {
            coffeeSize = "10";
        } else if (selectedSizeId == R.id.twelveOzRadioButton) {
            coffeeSize = "12";
        }

        // Check if selectedTime is empty and set to current time if so
        if (selectedTime.isEmpty()) {
            Calendar now = Calendar.getInstance();
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        }

        String url = "http://10.121.4.204:5000/endpoint?caramel=" + caramelValue + "&vanilla=" + vanillaValue
                + "&size=" + coffeeSize + "&time=" + selectedTime;;

        // Build the request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Asynchronous Call to avoid blocking the main thread
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // Handle the error
                Log.e("CoffeeBot", "Request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String responseData = response.body().string();
                    Log.d("CoffeeBot", "Response received: " + responseData);
                    runOnUiThread(() -> {
//                        responseTextView.setText(responseData); // Update the TextView on UI thread
                    });
                } else {
                    // If response is not successful (e.g., server returned error code)
                    runOnUiThread(() -> {
//                        responseTextView.setText("Failed to get response from server"); // Update UI on failure
                    });
                }
                navigateBackToMainActivity(); // Navigate back after handling the response
            }
        });
    }

    private void navigateBackToMainActivity() {
        // Create an Intent to start MainActivity
        Intent intent = new Intent(NewBrewActivity.this, MainActivity.class);
        // Flags to clear the activity stack and bring an existing instance of MainActivity to the top
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void scheduleGetRequestForSelectedTime(String selectedTime) {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        try {
            Date date = sdf.parse(selectedTime);
            Calendar selectedCalendarTime = Calendar.getInstance();
            assert date != null;
            selectedCalendarTime.setTime(date);
            selectedCalendarTime.set(Calendar.YEAR, now.get(Calendar.YEAR));
            selectedCalendarTime.set(Calendar.MONTH, now.get(Calendar.MONTH));
            selectedCalendarTime.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            long delayMillis = selectedCalendarTime.getTimeInMillis() - now.getTimeInMillis();
            if (delayMillis > 0) {
                new Handler().postDelayed(() -> sendGetRequestToPi(), delayMillis);
            } else {
                Log.e("NewBrewActivity", "Selected time is in the past. Not scheduling.");
            }
        } catch (ParseException e) {
            Log.e("NewBrewActivity", "Error parsing selected time", e);
        }
    }

    private void savePreset(String presetName) {
        int caramelPumps = caramelSlider.getProgress();
        int vanillaPumps = vanillaSlider.getProgress();
        int selectedSizeId = coffeeSizeRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedSizeId);
        String coffeeSize = selectedRadioButton.getText().toString();

        // Combine the preferences into a single string with a delimiter for easy parsing later
        String combinedPreferences = caramelPumps + ";" + vanillaPumps + ";" + coffeeSize;

        // Use SharedPreferences to save the preset under the provided preset name
        SharedPreferences prefs = getSharedPreferences("CoffeePresets", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(presetName, combinedPreferences);
        editor.apply();

        Toast.makeText(this, "Preset '" + presetName + "' saved!", Toast.LENGTH_SHORT).show();
    }

}