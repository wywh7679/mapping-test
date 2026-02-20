package com.benco.mapping;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.data.ApplicationsDataListAdapter;
import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsDao;
import com.benco.mapping.data.LocationsRoomDatabase;
import com.benco.mapping.domain.ApplicationsDataViewModel;
import com.benco.mapping.domain.ApplicationsViewModel;
import com.benco.mapping.gps.GPS;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity implements SensorEventListener {
    private String TAG = "MainActivity";
    private boolean hasGPS = false;
    private double lastLat = 0;
    private double lastLng = 0;
    private boolean setALineClicked = false;
    private boolean setBLineClicked = false;
    public LocationManager locationManager;
    public static MutableLiveData<String> aidText;
    public Button gpsIndicatorOrig;
    public int numberOfSections = 2;
    //The Section Toggle Buttons
    public HashMap<Integer, Button> sectionToggles = new HashMap<>();;
    //The States of the Section Toggle Buttons
    public HashMap<Integer, Boolean> sectionStates = new HashMap<>();;
    public Button isSprayingToggle;
    public ImageView gpsIndicator;
    public EditText applicationIdEditText;

    public com.benco.mapping.gps.GPS gpsClass;
    SwitchCompat toggleGPS;
    private RecyclerView recyclerView;
    static final float ALPHA = 0.25f;
    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor accelerometer;
    private float[] gravityData = new float[3];
    private float[] geomagneticData  = new float[3];
    private boolean hasGravityData = false;
    private boolean hasGeomagneticData = false;
    private double rotationInDegrees;
    private ImageView toggleGPSImg;
    private RelativeLayout relativeTop;
    private RelativeLayout relativeLayout;
    private DrawViewBitmap drawView;
    private TextView gridSizeLabel;
    Observer<List<Applications>> applicationSpinnerObserver;
    private View previousFocusedView;
    public TextView speedLabel;
    public TextView headingLabel;
    public TextView distanceLabel;
    Button resetButton;
    Button loadButton;
    Button zoomInButton;
    Button zoomOutButton;
    ImageView settingsBtn;
    SeekBar zoomSeekBar;
    Gson configJSON;
    static HashMap applicationSettings = new HashMap<>();
    private static final int PERMISSION_REQUEST_CODE = 100;

    LocationsRoomDatabase locationsRoomDatabase;
    LocationsDao locationsDao;

    private PerspectiveGridView perspectiveGridView;
    private void createSectionButtons() {
        // Get reference to the RelativeLayout
        numberOfSections = Integer.parseInt(settings.get("totalNumberSections").toString());
        FlexboxLayout bottomLayout = findViewById(R.id.flex_bottom);
        String text = "";
        String textNumber = "";
        SpannableString spannableString;
        Button button;
        GradientDrawable drawable;
        RelativeLayout.LayoutParams params;
        int buttonId;
        int lastButtonId = 0;
        int textLength = 0;
        for (int i = 1; i <= numberOfSections; i++) {
            // Create a SpannableString
            textNumber = "\n"+i;
            text = "Section";
            textLength = textNumber.length();
            // Set different text sizes
            SpannableStringBuilder builder = new SpannableStringBuilder();
            spannableString = new SpannableString(text);
            spannableString.setSpan(new AbsoluteSizeSpan(8, true), 0, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // "Section" - 16sp
            builder.append(spannableString);


            SpannableString spannableString2 = new SpannableString(textNumber);
            spannableString2.setSpan(new AbsoluteSizeSpan(12, true), 0, textLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // textNumber - 24sp
            builder.append(spannableString2);

            button = new Button(this);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                button.setTextSize(9);
            }

            //button.setText(spannableString);
            button.setText(builder, TextView.BufferType.SPANNABLE);
            drawable = new GradientDrawable();
            // Set the background color
            drawable.setColor(Color.BLACK); // Background color

            // Set the border
            drawable.setStroke(5, Color.WHITE); // Border width and color

            // Set corner radius (optional)
            drawable.setCornerRadius(0f); // Corner radius in pixels

            // Set the drawable as the button background
            button.setBackground(drawable);
            // Create LayoutParams for Button 1
            params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    80);
            int bottomId = bottomLayout.getId();
            params.setMargins(16, 16, 0, 0); // Set margins (left, top, right, bottom)
            buttonId = View.generateViewId();
            sectionStates.put(buttonId, false);
            sectionToggles.put(buttonId, button);
            button.setId(buttonId);
            lastButtonId = buttonId;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GradientDrawable drawable = new GradientDrawable();
                    Boolean state = sectionStates.get(v.getId());
                    int bgColor = (Boolean.FALSE.equals(state))?Color.BLUE:Color.BLACK;
                    // Set the background color
                    drawable.setColor(bgColor); // Background color

                    // Set the border
                    drawable.setStroke(5, Color.WHITE); // Border width and color

                    // Set corner radius (optional)
                    drawable.setCornerRadius(0f); // Corner radius in pixels

                    // Set the drawable as the button background
                    v.setBackground(drawable);
                    sectionStates.put(v.getId(), !state);
                }
            });
            float sectionOpacity = 1f;
            sectionOpacity = (isSpraying == 1)?1f:.5f;
            for (Integer key : sectionToggles.keySet()) {
                Button button2 = sectionToggles.get(key);
                button2.setAlpha(sectionOpacity);
            }
            // Add Button to RelativeLayout
            bottomLayout.addView(button, params);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        isSprayingToggle = findViewById(R.id.isSprayingToggle);

        createSectionButtons();

        hasGPS = checkLocationPermission();
        if (hasGPS) {
            gpsIndicator = findViewById(R.id.gpsIndicator);

            gpsIndicator.setColorFilter(ContextCompat.getColor(this, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);

          /*  Drawable buttonDrawable = gpsIndicator.getBackground();
            buttonDrawable = DrawableCompat.wrap(buttonDrawable);
            //the color is a direct color int and not a color resource
            DrawableCompat.setTint(buttonDrawable, Color.GREEN);

            gpsIndicator.setBackground(buttonDrawable);*/
        }
        Log.d("main activity", "hasGPS: "+hasGPS);
        if (!hasGPS) return;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationsRoomDatabase = LocationsRoomDatabase.getDatabase(this);
        locationsDao = locationsRoomDatabase.locationsDao();

        Log.d("main activity", "locationManager: "+locationManager);
        if (locationManager == null) return;
        String intentLid = getIntent().getStringExtra("lid");
        if (intentLid != null) {
            lid = Integer.parseInt(intentLid);
        }
        String intentConfigJSON = getIntent().getStringExtra("configJSON");
        if (intentConfigJSON != null) {
            //configJSON = new Gson();
           // applicationSettings = configJSON.fromJson(intentConfigJSON, applicationSettings.getClass());
        } else {
            //applicationSettings = settings;
        }
        applicationSettings = settings;
        String intentAid = getIntent().getStringExtra("aid");
        String intentAidAction = getIntent().getStringExtra("aidAction");
        if (intentAid != null && !intentAid.isEmpty()) {

            aid = Integer.parseInt(intentAid);
            //aid =  Integer.parseInt(intentAid);
            if (intentAid != null && !intentAid.isEmpty()) {

            }
        }
        loadButton = findViewById(R.id.loadButton);
        zoomInButton = findViewById(R.id.zoomInButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        resetButton = findViewById(R.id.resetButton);
        loadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
                aid = Integer.parseInt(applicationIdEditText.getText().toString());

                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.putExtra("aid", aid+"");
                finish();
                startActivity(intent);
                // relativeLayout.removeView(drawView);
               // aid = Integer.parseInt(applicationIdEditText.getText().toString());
               // addDrawView();
            }
        });
        settingsBtn = findViewById(R.id.settingsBtn);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                // relativeLayout.removeView(drawView);
               // aid = Integer.parseInt(applicationIdEditText.getText().toString());
               // addDrawView();
            }
        });
        isSprayingToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
                isSpraying = (isSpraying == 1)?0:1;
                //String label = (isSpraying == 1)?"Sprayer\r\nOn":"Sprayer\r\nOff";
                int bgColor = (isSpraying == 1) ? Color.GREEN : Color.RED;
                //isSprayingToggle.setText(label);
                Drawable buttonDrawable = isSprayingToggle.getBackground();
                buttonDrawable = DrawableCompat.wrap(buttonDrawable);
                //the color is a direct color int and not a color resource

                DrawableCompat.setTint(buttonDrawable, bgColor);

                isSprayingToggle.setBackground(buttonDrawable);
                float sectionOpacity = 1f;
                sectionOpacity = (isSpraying == 1)?1f:.5f;
                for (Integer key : sectionToggles.keySet()) {
                    Button button = sectionToggles.get(key);
                    button.setAlpha(sectionOpacity);
                }
                // relativeLayout.removeView(drawView);
                // aid = Integer.parseInt(applicationIdEditText.getText().toString());
                // addDrawView();
            }
        });
        //MainActivity.getText().observe(this, textView::setText);
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        relativeLayout = (RelativeLayout) findViewById(R.id.relative_home);
        gridSizeLabel = findViewById(R.id.gridSizeLabel);
        speedLabel = findViewById(R.id.speedLabel);
        headingLabel = findViewById(R.id.headingLabel);
        distanceLabel = findViewById(R.id.distanceLabel);
        zoomSeekBar = findViewById(R.id.zoomSeekBar);
        zoomSeekBar.incrementProgressBy(10);
        ImageView homeBtn = findViewById(R.id.homeBtn);
        homeBtn.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               finish();
           }
       });
        addDrawView();
        //drawView = new DrawViewBitmap(this, relativeLayout, gridSizeLabel, resetButton, aid, distanceLabel);

        //relativeLayout.addView(drawView);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        aidText = new MutableLiveData<>();

        gpsClass = new GPS();
        gpsClass.captrueGPS = false;
        gpsClass.gps(locationManager, AppDataVM, gpsIndicator, this);
        applicationIdEditText = findViewById(R.id.application_id);
        applicationIdEditText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                 if (!s.toString().equals("")) {
                    //aid = Integer.parseInt(s.toString());
                    //setLatLngObserver();
                }
                // you can call or do what you want with your EditText here

                // yourEditText...
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        setValue(aid+"");
        toggleGPS = findViewById(R.id.toggleGPSCapture);
        toggleGPS.setOnCheckedChangeListener((buttonView, isChecked) -> {
            gpsClass.captrueGPS = isChecked ? true : false;
            String message = isChecked ? "GPS Capture ON" : "GPS Capture OFF";
            //Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
        LocationListener locationListener =  new LocationListener() {

            private Location lastLocation;
            private float lastSpeedTime = 0f;

            @Override
            public void onLocationChanged(@NonNull Location location) {
                //Log.d(TAG, "loc changed: " +location);
                if (!gpsClass.captrueGPS) return;
                //Meters per second to MPH
                double currentSpeed = location.getSpeed()*2.237;
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                int i = 0;
                String aLine = "0";
                if (setALineClicked) {
                    aLine = "1";
                    setALineClicked = false;
                }
                String bLine = "0";
                if (setBLineClicked) {
                    bLine = "1";
                    setBLineClicked = false;
                }
                JSONObject sprayStates = new JSONObject();
                try {
                    sprayStates.put("master", isSpraying);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                for (Boolean value : sectionStates.values()) {
                    if (isSpraying == 1) {
                        //Master is on, so apply individual states...
                        int state = (value)?1:0;
                        try {
                            sprayStates.put(""+i, state);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        //Master is off, so all are off...
                        try {
                            sprayStates.put(""+i, 0);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    i++;
                }
                //Log.d(TAG, "sprayStates: "+sprayStates);
                ApplicationsData appData = new ApplicationsData();
                appData.lid = lid;
                appData.aid = aid;
                appData.lat = lat;
                appData.lng = lng;
                appData.speed = currentSpeed;
                appData.dateTime = new Date(location.getTime());
                appData.azimuth = azimuth;
                appData.bearing = location.getBearing();
                appData.isSpraying = sprayStates.toString();
                appData.aLine = aLine;
                appData.bLine = bLine;
                appData.timestamp = location.getTime();
                AppDataVM.insertApplications(appData);
                speedLabel.setText(Math.round(currentSpeed)+"");
                headingLabel.setText(Math.round(location.getBearing())+"");
                // Toast.makeText(MainActivity.getAppContext(), "Location Changed " + lat + " " + lng + " " + currentSpeed+" "+azimuth, Toast.LENGTH_SHORT).show();

                //}
            }

            @Override
            public void onProviderEnabled(String s) {
                gpsIndicator.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onProviderDisabled(String s) {
                gpsIndicator.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red), android.graphics.PorterDuff.Mode.SRC_IN);
            }
        };
        toggleGPSImg = findViewById(R.id.toggleGPSCaptureImg);
        toggleGPSImg.setOnClickListener((View v) -> {
            gpsClass.captrueGPS = gpsClass.captrueGPS ? false : true;
            String message = gpsClass.captrueGPS ? "GPS Capture ON" : "GPS Capture OFF";
            int id = gpsClass.captrueGPS ? getResources().getIdentifier("@android:drawable/ic_media_play", null, null) :getResources().getIdentifier("@android:drawable/ic_media_pause", null, null);
            toggleGPSImg.setImageResource(id);


            if (gpsClass.captrueGPS) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                gpsIndicator.setColorFilter(ContextCompat.getColor(this, R.color.green), android.graphics.PorterDuff.Mode.SRC_IN);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                gpsIndicator.setColorFilter(ContextCompat.getColor(this, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);
                locationManager.removeUpdates(locationListener);
            }
            //@android:drawable/ic_media_play

            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
        View.OnFocusChangeListener topBarFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                int endHeight;
                String viewName = "Other";
                String previousViewName = "none";
                if (1 == 2) {
                    if (previousFocusedView != null) {
                        if (previousFocusedView.getId() == R.id.application_id) {
                            previousViewName = "application_id";
                        }
                        if (previousFocusedView.getId() == R.id.relative_top) {
                            previousViewName = "relative_top";
                        }
                    }
                    if (v.getId() == R.id.application_id) {
                        viewName = "application_id";
                    }
                    if (v.getId() == R.id.relative_top) {
                        viewName = "relative_top";
                    }
                    // Log.d(TAG, "onFocusChanged :"+hasFocus+" "+viewName+" "+previousViewName);

                    if (hasFocus) {
                        if (v.getId() == R.id.relative_top) {
                            drawView.setFocusable(true);
                            drawView.setClickable(true);
                            drawView.setFocusableInTouchMode(true);
                        }
                        previousFocusedView = v;
                        // Handle focus gained
                        //Toast.makeText(MainActivity.this, viewName+" gained focus", Toast.LENGTH_SHORT).show();
                        endHeight = 320;
                    /* ViewGroup.LayoutParams params = relativeTop.getLayoutParams();
                    params.height = 320;
                    relativeTop.setLayoutParams(params);
                    */
                    } else {
                        if (previousFocusedView != null) {
                            // Do something with the previously focused view
                            if (previousFocusedView.getId() == R.id.application_id && v.getId() == R.id.relative_top) {
                                return;
                            }
                            if (v.getId() == R.id.relative_top) {
                                drawView.setFocusable(false);
                                drawView.setClickable(false);
                                drawView.setFocusableInTouchMode(false);
                            }
                        }
                        if (v == previousFocusedView) {
                            //previousFocusedView = null; // Clear if it's losing focus

                        }
                        // Handle focus lost
                        //Toast.makeText(MainActivity.this, viewName+" lost focus", Toast.LENGTH_SHORT).show();
                        endHeight = 160;
                   /* ViewGroup.LayoutParams params = relativeTop.getLayoutParams();
                    params.height = 160;
                    relativeTop.setLayoutParams(params);*/
                    }
                    int startHeight = relativeTop.getHeight();
                    //int endHeight = startHeight == 160 ? 320 : 160; // Toggle height

                    ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            int animatedValue = (int) valueAnimator.getAnimatedValue();
                            ViewGroup.LayoutParams layoutParams = relativeTop.getLayoutParams();
                            layoutParams.height = animatedValue;
                            relativeTop.setLayoutParams(layoutParams);
                        }
                    });
                    animator.setDuration(300); // Duration of the animation
                    animator.start();
                }
            }
        };
        relativeTop = findViewById(R.id.relative_top);
        relativeTop.setOnFocusChangeListener(topBarFocusChangeListener);
        applicationIdEditText.setOnFocusChangeListener(topBarFocusChangeListener);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        AppDataVM = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()).create(ApplicationsDataViewModel.class);
        //navView.setVisibility(View.GONE);
        //setLatLngObserver();
        ApplicationsViewModel AppVM = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()).create(ApplicationsViewModel.class);
        // Observe the LiveData from the ViewModel
        //
        /*
        applicationSpinnerObserver = applications -> {

            if (!applications.isEmpty()) {
                List<String> itemNames = new ArrayList<>();
                for (Applications item : applications) {
                    itemNames.add(item.aid + "");
                }
                Spinner spinner = findViewById(R.id.applicationList);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item, itemNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            }
        };
        AppVM.getAllApplicationsFromVm().observe(this,applicationSpinnerObserver);
        */
        //DatabaseUtils dbUtils = new DatabaseUtils();
        //dbUtils.exportDatabase(getApplicationContext());
        //exportDatabase();

    }
    private void addDrawView() {
        if (1 == 1) {
            HashMap<String, Object> elements = new HashMap<>();
            elements.put("aLineButton", findViewById(R.id.aLineButton));
            elements.put("bLineButton", findViewById(R.id.aLineButton));
            elements.put("zoomOutButton", zoomOutButton);
            elements.put("zoomInButton", zoomInButton);
            elements.put("resetButton", resetButton);
            elements.put("mapView", relativeLayout);
            elements.put("gridSizeLabel", gridSizeLabel);
            elements.put("distanceLabel", distanceLabel);
            elements.put("zoomSeekBar", zoomSeekBar);
            drawView = new DrawViewBitmap(this, aid, applicationSettings, elements, new Locations("test", ""), locationsDao);
            relativeLayout.addView(drawView);

            findViewById(R.id.aLineButton).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setALineClicked = true;
                }
            });
            findViewById(R.id.bLineButton).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    setBLineClicked = true;
                }
            });
        } else {
            //setContentView(R.layout.perpective_grid_view);

            //perspectiveGridView = findViewById(R.id.relative_home);
            perspectiveGridView = new PerspectiveGridView(this, null);
            relativeLayout.addView(perspectiveGridView);
        }
    }
    public void setLatLngObserver() {
        recyclerView = findViewById(R.id.liveLatLngView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        AppDataVM.getApplicationsFromVm(aid).observe(this, applications ->
        {
            if (applications != null && !applications.isEmpty()) {
                ApplicationsDataListAdapter adapter = new ApplicationsDataListAdapter((ArrayList<ApplicationsData>) applications);
                recyclerView.setAdapter(adapter);
            }
        });
    }
    public void setValue(String value) { aidText.setValue(value); applicationIdEditText.setText(value); }
    public static LiveData<String> getText() {
        return aidText;
    }

    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, gravityData, 0, 3);
                hasGravityData = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, geomagneticData, 0, 3);
                hasGeomagneticData = true;
                break;
            default:
                return;
        }

        if (hasGravityData && hasGeomagneticData) {
            float identityMatrix[] = new float[9];
            float rotationMatrix[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, identityMatrix,
                    gravityData, geomagneticData);

            if (success) {
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                float rotationInRadians = orientationMatrix[0];
                rotationInDegrees = Math.toDegrees(rotationInRadians);
                gpsClass.setAzimuth(rotationInRadians);
                azimuth = rotationInRadians;
                // do something with the rotation in degrees
            }
        }
    }
    public boolean checkLocationPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        Boolean hasPermissions = true;
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
                hasPermissions = false;
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are already granted, proceed with your logic
            // e.g., start location updates, access storage
        }
        return hasPermissions;
    }
    public boolean checkLocationPermissionOrig() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Need GPS!")
                        .setMessage("Please give GPS permission!")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        99);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        99);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 99: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        gpsClass = new GPS();
                        gpsClass.captrueGPS = false;
                        gpsClass.gps(locationManager, AppDataVM, gpsIndicator, this);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Turn on GPS to enable mapping", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }
}


