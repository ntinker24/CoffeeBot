package com.example.coffeebot;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView responseTextView;
    private SeekBar sugarSlider, creamSlider;
    private TextView sugarText, creamText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        responseTextView = findViewById(R.id.responseTextView);
        sugarSlider = findViewById(R.id.sugarSlider);
        creamSlider = findViewById(R.id.creamSlider);
        sugarText = findViewById(R.id.sugarText);
        creamText = findViewById(R.id.creamText);

        // Find the button by its ID and set a click listener on the button
        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendGetRequestToPi();
                Log.d("CoffeeBot", "Button pressed");
            }
        });

        sugarSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sugarText.setText("Sugar: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Add any required functionality here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Add any required functionality here
            }
        });

        creamSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                creamText.setText("Cream: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Add any required functionality here
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Add any required functionality here
            }
        });
    }

    private void sendGetRequestToPi() {
        // Create a new OkHttpClient
        OkHttpClient client = new OkHttpClient();

        String url = "http://10.121.4.204:5000/endpoint";

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
                        responseTextView.setText(responseData); // Update the TextView on UI thread
                    });
                } else {
                    // If response is not successful (e.g., server returned error code)
                    runOnUiThread(() -> {
                        responseTextView.setText("Failed to get response from server"); // Update UI on failure
                    });
                }
            }
        });
    }
}
