package com.linmu.collision_warning_system;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.linmu.collision_warning_system.services.LocationService;

import java.math.RoundingMode;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {



    public Handler locationHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            TextView coordinateTextView = findViewById(R.id.location_coordinate);
            if (msg.what == 1000) {
                Bundle bundle = msg.getData();
                double latitude = bundle.getDouble("lat");
                double longitude = bundle.getDouble("lon");
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setMaximumFractionDigits(5);
                nf.setRoundingMode(RoundingMode.UP);
                String coordinate = "( " + nf.format(longitude) + " , " + nf.format(latitude) + " )";
                coordinateTextView.setText(coordinate);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationService locationService = new LocationService(getApplicationContext(),locationHandler);
        locationService.startLocation();

    }


}