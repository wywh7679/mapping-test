package com.benco.mapping.gps;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.LifecycleOwner;

import com.benco.mapping.BaseActivity;
import com.benco.mapping.GLMapActivity;
import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.domain.ApplicationsDataViewModel;
import com.benco.mapping.MainActivity;

import java.util.Date;

public class GPS extends BaseActivity {
    @SuppressLint("MissingPermission")
    private double lastLat = 0;
    private double lastLng = 0;
    private float azimuth = 0;
    public boolean captrueGPS = false;
    private String TAG = "GPS Class";
    @SuppressLint("MissingPermission")
    public boolean gps(LocationManager locationManager, ApplicationsDataViewModel AppDataVM, ImageView gpsIndicator, MainActivity mainActivity) {
        MainActivity.getText().observe(mainActivity, aidText -> {
            Log.d(TAG, "aidText: "+aidText.toString());
            if (aidText.equals("")) {
            } else {
                aid = Integer.parseInt(aidText.toString());
                //Log.d(TAG, "aidText: "+aid);
            }
        });

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
              return true;
        } else {
            //Toast.makeText(MainActivity.getAppContext(), "Turn on GPS to enable mapping", Toast.LENGTH_LONG).show();
            return false;
        }
    }
    public boolean gps3d(LocationManager locationManager, GLMapActivity mainActivity) {
        GLMapActivity.getText().observe(mainActivity, aidText -> {
            Log.d(TAG, "aidText: "+aidText.toString());
            if (aidText.equals("")) {
            } else {
                aid = Integer.parseInt(aidText.toString());
                //Log.d(TAG, "aidText: "+aid);
            }
        });

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
              return true;
        } else {
            //Toast.makeText(MainActivity.getAppContext(), "Turn on GPS to enable mapping", Toast.LENGTH_LONG).show();
            return false;
        }
    }
    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }
}
