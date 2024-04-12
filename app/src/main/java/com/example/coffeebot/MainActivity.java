package com.example.coffeebot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button firstScreenButton = findViewById(R.id.buttonNewBrew);
        Button secondScreenButton = findViewById(R.id.buttonSavedBrews);

        firstScreenButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewBrewActivity.class);
            startActivity(intent);
        });

        secondScreenButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SavedBrewActivity.class);
            startActivity(intent);
        });
    }
}
