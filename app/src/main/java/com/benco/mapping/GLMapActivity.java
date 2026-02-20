package com.benco.mapping;

import static com.benco.mapping.DrawViewBitmap.round;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
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
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.Applications;
import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.data.Locations;
import com.benco.mapping.data.LocationsDao;
import com.benco.mapping.data.LocationsRoomDatabase;
import com.benco.mapping.domain.ApplicationsDataViewModel;
import com.benco.mapping.domain.ApplicationsViewModel;
import com.benco.mapping.gps.GPS;
import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class GLMapActivity extends BaseActivity implements SensorEventListener {
    private MyGLSurfaceView glSurfaceView;
    private String TAG = "GLMapActivity";
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

    public GPS gpsClass;
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
    ImageView compass;
    Observer<List<Applications>> applicationSpinnerObserver;
    private View previousFocusedView;
    public TextView speedLabel;
    public TextView headingLabel;
    public TextView distanceLabel;
    Button resetButton;
    Button loadButton;
    Button zoomInButton;
    Button zoomOutButton;
    Button abLineDrawerHandle;
    Button abLinePlusButton;
    Button abLineMinusButton;
    Button abLineRotatePlusButton;
    Button abLineRotateMinusButton;
    List<SectionStyle> sectionStyles;
    List<ApplicationsData> applicationsData;
    private ApplicationsDataViewModel AppDataVM;
    ImageView settingsBtn;
    SeekBar zoomSeekBar;
    float lastSeekBarValue = 0;
    private static final float DEFAULT_AB_LINE_OFFSET = 0f;
    private float abLineOffset = DEFAULT_AB_LINE_OFFSET;

    private static final float DEFAULT_AB_LINE_YAW_DEGREES = 0f;
    private float abLineYaw = DEFAULT_AB_LINE_YAW_DEGREES;

    Gson configJSON;
    static HashMap applicationSettings = new HashMap<>();
    private static final int PERMISSION_REQUEST_CODE = 100;

    LocationsRoomDatabase locationsRoomDatabase;
    LocationsDao locationsDao;
    MyRenderer renderer;
    private Applications currentApp;
    private final PathGeometryBuilder geometryBuilder = new PathGeometryBuilder();
    private final ABLineGeometryBuilder abLineGeometryBuilder = new ABLineGeometryBuilder();
    private String currentConfigJson = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glmapactivity);

        glSurfaceView = findViewById(R.id.glSurface);
        zoomInButton = findViewById(R.id.zoomInButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        zoomSeekBar = findViewById(R.id.zoomSeekBar);
        abLineDrawerHandle = findViewById(R.id.abLineDrawerHandle);
        abLinePlusButton = findViewById(R.id.abLinePlusButton);
        abLineMinusButton = findViewById(R.id.abLineMinusButton);
        abLineRotatePlusButton = findViewById(R.id.abLineRotatePlusButton);
        abLineRotateMinusButton = findViewById(R.id.abLineRotateMinusButton);
        abLinePlusButton.setOnClickListener(v -> {
            abLineOffset += 18f;
            ABLineGeometry abLineGeometry = abLineGeometryBuilder.build(applicationsData, sectionStyles, abLineOffset, abLineYaw);
            renderer.setABLineGeometry(abLineGeometry);
        });
        abLineMinusButton.setOnClickListener(v -> {
            abLineOffset -= 18f;
            ABLineGeometry abLineGeometry = abLineGeometryBuilder.build(applicationsData, sectionStyles, abLineOffset, abLineYaw);
            renderer.setABLineGeometry(abLineGeometry);
        });
        abLineRotatePlusButton.setOnClickListener(v -> {
            abLineYaw += 1f;
            ABLineGeometry abLineGeometry = abLineGeometryBuilder.build(applicationsData, sectionStyles, abLineOffset, abLineYaw);
            renderer.setABLineGeometry(abLineGeometry);
        });
        abLineRotateMinusButton.setOnClickListener(v -> {
            abLineYaw -= 1f;
            ABLineGeometry abLineGeometry = abLineGeometryBuilder.build(applicationsData, sectionStyles, abLineOffset, abLineYaw);
            renderer.setABLineGeometry(abLineGeometry);
        });
        float[] coordinates = { /* your coords */ };
        glSurfaceView.initRenderer(coordinates);

        renderer = glSurfaceView.getRenderer();
        compass = findViewById(R.id.compassImage);

        zoomInButton.setOnClickListener(v -> {
            renderer.setCameraBehind(-900f, false);
            renderer.setCameraHeight(-900f, false);
            zoomSeekBar.setProgress(zoomSeekBar.getProgress()-90);
            renderer.setGridScaleFromZoom(zoomSeekBar.getProgress());
            updateGridSizeLabel();
        });

        zoomOutButton.setOnClickListener(v -> {
            renderer.setCameraBehind(900f, false);
            renderer.setCameraHeight(900f, false);
            zoomSeekBar.setProgress(zoomSeekBar.getProgress()+90);
            renderer.setGridScaleFromZoom(zoomSeekBar.getProgress());
            updateGridSizeLabel();
        });

        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lastSeekBarValue = progress;
                renderer.setCameraBehind(progress, true);
                renderer.setCameraHeight(progress, true);
                renderer.setGridScaleFromZoom(progress);
                updateGridSizeLabel();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        isSprayingToggle = findViewById(R.id.isSprayingToggle);

        createSectionButtons();

        hasGPS = checkLocationPermission();
        if (hasGPS) {
            gpsIndicator = findViewById(R.id.gpsIndicator);

            gpsIndicator.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);

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
        resetButton.setOnClickListener(v -> {
            renderer.resetCameraToPathEnd();

            // Reset zoom UI to initial value if you use a SeekBar
            zoomSeekBar.setProgress(200);

            // If you use grid scaling, update it after reset
            renderer.setGridScaleFromZoom(zoomSeekBar.getProgress());
            updateGridSizeLabel();
        });
        loadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
                aid = Integer.parseInt(applicationIdEditText.getText().toString());

                Intent intent = new Intent(GLMapActivity.this, GLMapActivity.class);
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
                Intent intent = new Intent(GLMapActivity.this, SettingsActivity.class);
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
        //relativeLayout = (RelativeLayout) findViewById(R.id.relative_home);
        gridSizeLabel = findViewById(R.id.gridSizeLabel);
        speedLabel = findViewById(R.id.speedLabel);
        headingLabel = findViewById(R.id.headingLabel);
        distanceLabel = findViewById(R.id.distanceLabel);
        applyDisplaySettings();
        zoomSeekBar = findViewById(R.id.zoomSeekBar);
        zoomSeekBar.incrementProgressBy(10);
        ImageView homeBtn = findViewById(R.id.homeBtn);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        //addDrawView();
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
        gpsClass.gps3d(locationManager, this);
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
                speedLabel.setText(formatSpeed(currentSpeed));
                headingLabel.setText(Math.round(location.getBearing())+"");
                compass.setRotation(location.getBearing());
                // Toast.makeText(MainActivity.getAppContext(), "Location Changed " + lat + " " + lng + " " + currentSpeed+" "+azimuth, Toast.LENGTH_SHORT).show();

                //}
            }

            @Override
            public void onProviderEnabled(String s) {
                gpsIndicator.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onProviderDisabled(String s) {
                gpsIndicator.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red), PorterDuff.Mode.SRC_IN);
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
                gpsIndicator.setColorFilter(ContextCompat.getColor(this, R.color.green), PorterDuff.Mode.SRC_IN);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                gpsIndicator.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
                locationManager.removeUpdates(locationListener);
            }
            //@android:drawable/ic_media_play

            Toast.makeText(GLMapActivity.this, message, Toast.LENGTH_SHORT).show();
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
        ApplicationsViewModel AppVM = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()).create(ApplicationsViewModel.class);

        AppVM.getApplication(aid).observe(this, applications -> {
            currentApp = applications.get(0);
            //Log.d(TAG, "currentApp: "+currentApp.config);
        });
        AppDataVM = ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()).create(ApplicationsDataViewModel.class);
        AppDataVM.getAllApplicationsFromVm(aid).observe(this, applications -> {
            sectionStyles = buildSectionStylesFromConfig(currentApp.config);
            applicationsData = applications;
            PathGeometry geometry = geometryBuilder.build(applications, sectionStyles);
            ABLineGeometry abLineGeometry = abLineGeometryBuilder.build(applications, sectionStyles, abLineOffset, abLineYaw);
            List<Point> absPoints = geometryBuilder.getAbsPoints();
            setLabels(absPoints);
            Point location = absPoints.get(absPoints.size()-1);
            compass.setRotation(location.bearing);
            //Log.d(TAG, "geometry: "+absPoints);
            renderer.setAbsPoints(absPoints);
            renderer.setPathGeometry(geometry, sectionStyles);
            renderer.setABLineGeometry(abLineGeometry);
            renderer.setPathEndFromGeometry(geometry.getMainVertices());
            renderer.fitCameraToBounds();
            updateGridSizeLabel();
            // Save “initial” camera once at screen load
            renderer.captureInitialCamera();
            int mainColor = parseConfigInt(currentConfigJson, "mainPathColor", Color.GREEN);
            int leftColor = parseConfigInt(currentConfigJson, "section2PathColor", Color.WHITE);
            int rightColor = parseConfigInt(currentConfigJson, "section1PathColor", Color.WHITE);
            renderer.setPathColors(mainColor, leftColor, rightColor);

            int thickColor = parseConfigInt(currentConfigJson, "thickPathColor", Color.RED);
            int thickWidth = parseConfigInt(currentConfigJson, "thickPathStroke", 120);
            renderer.setPathWidths(thickWidth, thickColor);

            boolean showGrid = parseSettingBoolean("showGrid", true);
            boolean showSolidBackground = parseSettingBoolean("showSolidBackground", true);
            boolean showABLines = parseSettingBoolean("showABLines", true);
            boolean showSteeringLines = parseSettingBoolean("showSteeringLines", true);
            renderer.setDisplayToggles(showGrid, showSolidBackground, showABLines, showSteeringLines);
            renderer.setGridColor(parseSettingColor("gridColor", Color.parseColor("#C0780000")));
            renderer.setGridBackgroundColor(parseSettingColor("backgroundColor", Color.parseColor("#C71F1F1F")));
            renderer.setAbLineColor(parseSettingColor("abLineColor", Color.GREEN));
            renderer.setSteeringLineColor(parseSettingColor("steeringLineColor", Color.BLUE));
            renderer.setFieldBoundaryDisplay(parseSettingBoolean("showFieldBoundaries", true));
            renderer.setFieldBoundaryColor(parseSettingColor("fieldBoundaryColor", Color.YELLOW));
            renderer.setFieldBoundaryGeometry(buildFieldBoundaryGeometry(applications));
        });
        AppVM.getAllApplicationsByLid(lid).observe(this, applications -> {
            for (Applications application : applications) {
                if (application.aid == aid) {
                    currentConfigJson = application.config == null ? "" : application.config;
                    //Log.d(TAG, "application.config"+application.config);
                    break;
                }
            }
        });
        //navView.setVisibility(View.GONE);
        //setLatLngObserver();

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
    private void updateGridSizeLabel() {
        if (gridSizeLabel != null) {
            gridSizeLabel.setText(renderer.getGridSizeLabel());
        }
    }
    public void setLabels(List<Point> absPoints) {
        double distance = 0;
        double bearing = 0;
        //double speed = 0;
        for (int i = 0; i < absPoints.size(); i++) {
            distance += absPoints.get(i).distance;
            bearing = absPoints.get(i).bearing;
        }
        String distanceText;
        boolean useMetric = isMetricUnits();
        if (useMetric) {
            double meters = distance * 0.0254;
            if (meters < 1000) {
                distanceText = round(meters, 2) + " m";
            } else {
                distanceText = round(meters / 1000, 2) + " km";
            }
        } else {
            if (distance/12 < 1320) {
                distanceText = round(distance / 12, 2)+" ft";
            } else {
                distanceText = round(distance / 12 / 5280, 2)+" mi";
            }
        }
        distanceLabel.setText(distanceText);
        headingLabel.setText(Math.round(bearing)+"");
    }
    public void setValue(String value) { aidText.setValue(value); applicationIdEditText.setText(value); }
    public static LiveData<String> getText() {
        return aidText;
    }

    private void applyDisplaySettings() {
        float multiplier = parseSettingFloat("textScaleMultiplier", 1.0f);
        applyTextSizeMultiplier(multiplier);
        speedLabel.setText(formatSpeed(0f));
    }

    private void applyTextSizeMultiplier(float multiplier) {
        float safeMultiplier = Math.max(0.7f, Math.min(multiplier, 2.0f));
        List<TextView> labels = new ArrayList<>();
        labels.add(gridSizeLabel);
        labels.add(speedLabel);
        labels.add(headingLabel);
        labels.add(distanceLabel);
        for (TextView label : labels) {
            if (label != null) {
                label.setTextSize(12f * safeMultiplier);
            }
        }
    }

    private String formatSpeed(double speedMph) {
        if (isMetricUnits()) {
            double speedKmh = speedMph * 1.60934;
            return Math.round(speedKmh) + " km/h";
        }
        return Math.round(speedMph) + " mph";
    }

    private boolean isMetricUnits() {
        Object value = settings.get("unitSystem");
        return value != null && "metric".equalsIgnoreCase(value.toString());
    }

    private float parseSettingFloat(String key, float fallback) {
        Object value = settings.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean parseSettingBoolean(String key, boolean fallback) {
        Object value = settings.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private int parseSettingColor(String key, int fallback) {
        Object value = settings.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private List<float[]> buildFieldBoundaryGeometry(List<ApplicationsData> applications) {
        if (applications == null || applications.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Locations location = locationsDao.getLocationByIdSync(lid);
            if (location == null || location.config == null || location.config.isEmpty()) {
                return new ArrayList<>();
            }
            JSONObject config = new JSONObject(location.config);
            if (!config.has("boundaryFilePath")) {
                return new ArrayList<>();
            }
            String boundaryFilePath = config.getString("boundaryFilePath");
            java.io.File shpFile = new java.io.File(boundaryFilePath);
            if (!shpFile.exists()) {
                return new ArrayList<>();
            }

            ApplicationsData last = applications.get(applications.size() - 1);
            double centerLat = last.lat;
            double centerLng = last.lng;

            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            for (ApplicationsData appData : applications) {
                if (appData.speed == 0 || appData.lat == 0 || appData.lng == 0) {
                    continue;
                }
                float[] xy = toXY(centerLat, centerLng, appData.lat, appData.lng, 0.0254f);
                minX = Math.min(minX, xy[0]);
                minY = Math.min(minY, xy[1]);
            }
            float xOffset = minX < 0 ? Math.abs(minX) : -minX;
            float yOffset = minY < 0 ? Math.abs(minY) : -minY;

            java.io.FileInputStream fis = new java.io.FileInputStream(shpFile);
            List<List<double[]>> parts = ShapefileBoundaryReader.readBoundaryParts(fis, false);
            fis.close();

            List<float[]> strips = new ArrayList<>();
            for (List<double[]> part : parts) {
                if (part.size() < 2) {
                    continue;
                }
                boolean closed = Math.abs(part.get(0)[0] - part.get(part.size()-1)[0]) < 1e-10 &&
                        Math.abs(part.get(0)[1] - part.get(part.size()-1)[1]) < 1e-10;
                int extra = closed ? 0 : 1;
                float[] strip = new float[(part.size() + extra) * 3];
                int idx = 0;
                for (double[] lngLat : part) {
                    float[] xy = toXY(centerLat, centerLng, lngLat[1], lngLat[0], 0.0254f);
                    strip[idx++] = xy[0] + xOffset;
                    strip[idx++] = 0f;
                    strip[idx++] = xy[1] + yOffset;
                }
                if (!closed) {
                    float[] xy = toXY(centerLat, centerLng, part.get(0)[1], part.get(0)[0], 0.0254f);
                    strip[idx++] = xy[0] + xOffset;
                    strip[idx++] = 0f;
                    strip[idx] = xy[1] + yOffset;
                }
                strips.add(strip);
            }
            return strips;
        } catch (Exception e) {
            Log.e(TAG, "Boundary load failed", e);
            return new ArrayList<>();
        }
    }

    private float[] toXY(double centerLatitude, double centerLongitude, double latitude, double longitude, double metersPerPixel) {
        double rto = 1 / metersPerPixel;
        double dLat = ((centerLatitude - latitude) / 0.00001) * rto;
        double dLng = -1 * ((centerLongitude - longitude) / 0.00001) * rto;
        int y = (int) Math.round(dLat);
        int x = (int) Math.round(dLng);
        return new float[]{x, y};
    }

    private int parseSettingInt(String key, int fallback) {
        Object value = settings.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseConfigInt(String configJson, String key, int fallback) {
        if (configJson == null || configJson.isEmpty()) {
            return fallback;
        }
        try {
            JSONObject jsonObject = new JSONObject(configJson);
            //Log.d(TAG, "jsonObject"+jsonObject);
            if (!jsonObject.has(key)) {
                return fallback;
            }
            Object value = jsonObject.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (JSONException | NumberFormatException e) {
            return fallback;
        }
    }

    private List<SectionStyle> buildSectionStylesFromConfig(String configJson) {
        int sectionCount = parseConfigInt(configJson, "totalNumberSections", 1);
        sectionCount = Math.min(sectionCount, 16);
        Log.d(TAG, "configJson: "+configJson);
        Log.d(TAG, "sectionCount: "+sectionCount);
        List<SectionStyle> styles = new ArrayList<>();
        for (int i = 1; i <= sectionCount; i++) {
            String widthKey = "section" + i + "Width";
            String colorKey = "section" + i + "PathColor";
            int width = parseConfigInt(configJson, widthKey, 30);
            int color = parseConfigInt(configJson, colorKey, Color.WHITE);

            int colorInt = color; // Example int value

            int alpha = (colorInt >> 24) & 0xFF;
            int red   = (colorInt >> 16) & 0xFF;
            int green = (colorInt >> 8)  & 0xFF;
            int blue  = (colorInt >> 0)  & 0xFF; // or simply (colorInt & 0xFF)
            int newAlpha = 100;
            int packedColor = (newAlpha << 24) | (red << 16) | (green << 8) | blue;
            color = packedColor;
            //Log.d(TAG, "width: "+width);
            //Log.d(TAG, "color: "+color);
            Log.d(TAG, "alpha: "+alpha+" red: "+red+" green: "+green+" blue: "+blue);
            styles.add(new SectionStyle(width, color));
        }
        return styles;
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
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, identityMatrix, gravityData, geomagneticData);
            if (success) {
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                float rotationInRadians = orientationMatrix[0];
                rotationInDegrees = Math.toDegrees(rotationInRadians);
                gpsClass.setAzimuth(rotationInRadians);
                azimuth = rotationInRadians;
                // do something with the rotation in degrees
                //renderer.setGridAzimuth((float) Math.toDegrees(azimuth));
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
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

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
                                ActivityCompat.requestPermissions(GLMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},99);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},99);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
                        gpsClass.gps3d(locationManager, this);
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
}
