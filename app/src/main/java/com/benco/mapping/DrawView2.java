package com.benco.mapping;

import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.domain.ApplicationsDataViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DrawView2 extends View {
    private static final String TAG = "DrawViewBitmap";
    private static final double EARTH_RADIUS = 6371; // Radius of the earth in kilometers
    private int minMoventInInches = 30; //Minimum amount between lat/lng before rendering a point
    private float ww = Integer.MAX_VALUE;
    private  float hh = Integer.MAX_VALUE;
    private float initialBearing = 0f; //Bearing of first point
    private float currentBearing = 0f; //Bearing of current point
    Canvas mapCanvas;
    Canvas plotCanvas;
    View mapView;
    final int SMOOTH_VAL = 6;
    ApplicationsDataViewModel AppDataVM;
    private ArrayList<List<Point>> parallelPoints = new ArrayList<>();
    private ArrayList<Point> points = new ArrayList<Point>(); //Lat/Lng as x,y relative to first point
    private ArrayList<Point> zeroPoints = new ArrayList<>(); //Lat/Lng as x,y relative to 0,0
    private ArrayList<Point> absPoints = new ArrayList<>(); //Lat/Lng as x,y relative to offsetX, offsetY
    private Paint paintMainLine = new Paint(Paint.ANTI_ALIAS_FLAG); //Paint for the main line
    private Paint paintSkinnyLine2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintSkinnyLine3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintSkinnyLine4 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintThickLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint aLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint lLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint rLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint lThickLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint rThickLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint cLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint labelPaint = new Paint();
    Path pathMainLine = new Path();
    Path pathSkinnyLineL = new Path();
    Path pathSkinnyLineR = new Path();
    Path pathThickLine = new Path();

    //Center reference for converting lat/lng to x/y
    Point cpoint;

    boolean setMinMax = false;
    boolean resizedDims = false;

    //How big we would like the screen based on overall canvas size
    int desiredWidth = 0;
    int desiredHeight = 0;

    //Used to calculate the needed canvas size
    float absmaxX = 0;
    float absmaxY = 0;
    float absminY = Integer.MAX_VALUE;
    float absminX = Integer.MAX_VALUE;
    float maxX = 0;
    float maxY = 0;
    float minY = Integer.MAX_VALUE;
    float minX = Integer.MAX_VALUE;

    //The current application_id
    int aid = 0;

    //Number of cols/rows for the grid
    private int numColumns, numRows;
    //width/height of the grid cells
    private int cellWidth, cellHeight;
    //NOT USED - an array to hold selected grid cells
    private boolean[][] cellChecked;

    //Zoom / Pan stuff
    public static Matrix matrix = new Matrix();
    public static Matrix savedMatrix = new Matrix();
    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    boolean isZoom = false;
    TextView gridSizeLabel;
    String gridSizeText = "10 ft";

    Boolean isForTouch = false;

    //double perPixel = .04d; //1.574808 inches
    //double perPixel = 0.3048; //1 Foot
    //double perPixel = 10d;
    double perPixel = 0.025399999999999992029d; //1 inch

    int thickStroke = 120;
    Button resetButton;

    double totalDistance = 0;
    private Matrix originalMatrix;
    Observer<List<ApplicationsData>> myObserver;
    private List<ApplicationsData> lastApplications;
    private TextView distanceLabel;
    HashMap settings;

    private float scaleFactor = 1f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float translateX = 0f;
    private float translateY = 0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    public DrawView2(Context context, View mapRView, TextView gridVSizeLabel, Button resetBtn, int application_id, TextView distanceRLabel, HashMap rsettings) {
        super(context);
        Log.d(TAG, "DrawViewBitmap");
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        Log.d(TAG, "Screen Dimensions: "+width+" "+height);
        //setFocusable(true);
        // setFocusableInTouchMode(true);
        settings = rsettings;
        aid = application_id;
        resetButton = resetBtn;
        matrix.reset();
        originalMatrix = new Matrix(); // Initialize the original matrix
        originalMatrix.set(matrix); // Store the original matrix
        savedMatrix.reset();
        mapView = mapRView;
        //mapView.setOnTouchListener(new Touch());
        gridSizeLabel = gridVSizeLabel;
        distanceLabel = distanceRLabel;
        resetButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                fitContentToScreen();
            }
        });
       /* resetButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
                Log.d(TAG, "resetButton clicked");
                if (isForTouch) {
                    resetButton.setText("false");
                    isForTouch = false;

                    //fitCanvasToScreen(mapView);

                } else {
                    resetButton.setText("true");
                    isForTouch = true;
                }
                //if (plotCanvas != null) {
                //    fitCanvasToScreen(plotCanvas);
                //}
            }
        });*/


        thickStroke = Integer.parseInt((String) settings.get("thickPathStroke"));
        //Log.d(TAG, "thickStroke: "+thickStroke);
        paintThickLine.setAntiAlias(true);
        //paintThickLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        int thickColor = Color.argb(128, 255, 0, 0);
        // paintThickLine.setColor(thickColor);
        paintThickLine.setColor((int) settings.get("thickPathColor"));
        paintThickLine.setStyle(Paint.Style.STROKE);
        paintThickLine.setStrokeWidth(thickStroke);
        paintThickLine.setStrokeCap(Paint.Cap.BUTT);
        paintThickLine.setStrokeJoin(Paint.Join.ROUND);
        paintThickLine.setStyle(Paint.Style.STROKE);
        //paintThickLine.setShadowLayer(5,0,0, Color.RED);

        // paintMainLine.setStyle(Paint.Style.FILL_AND_STROKE);
        paintMainLine.setAntiAlias(true);
        //paintMainLine.setAlpha(78);
        //paintMainLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintMainLine.setColor((int) settings.get("mainPathColor"));
        paintMainLine.setStrokeWidth(6);
        paintMainLine.setStrokeCap(Paint.Cap.BUTT);
        paintMainLine.setStrokeJoin(Paint.Join.ROUND);
        paintMainLine.setStyle(Paint.Style.STROKE);
        //paintMainLine.setShadowLayer(1,0,0, Color.GREEN);




        // paintSkinnyLine2.setStyle(Paint.Style.FILL_AND_STROKE);
        paintSkinnyLine2.setAntiAlias(true);
        //paintSkinnyLine2.setAlpha(78);
        //paintSkinnyLine2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintSkinnyLine2.setColor(Color.GREEN);
        paintSkinnyLine2.setStrokeWidth(252);
        paintSkinnyLine2.setStrokeCap(Paint.Cap.ROUND);
        paintSkinnyLine2.setStrokeJoin(Paint.Join.ROUND);
        paintSkinnyLine2.setStyle(Paint.Style.STROKE);
        //paintSkinnyLine2.setShadowLayer(1,0,0, Color.GREEN);

        // paintSkinnyLine2.setStyle(Paint.Style.FILL_AND_STROKE);
        paintSkinnyLine3.setAntiAlias(true);
        //paintSkinnyLine2.setAlpha(78);
        //paintSkinnyLine2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintSkinnyLine3.setColor(Color.BLUE);
        paintSkinnyLine3.setStrokeWidth(240);
        paintSkinnyLine3.setStrokeCap(Paint.Cap.ROUND);
        paintSkinnyLine3.setStrokeJoin(Paint.Join.ROUND);
        paintSkinnyLine3.setStyle(Paint.Style.STROKE);
        //paintSkinnyLine2.setShadowLayer(1,0,0, Color.GREEN);
        // paintSkinnyLine2.setStyle(Paint.Style.FILL_AND_STROKE);
        paintSkinnyLine4.setAntiAlias(true);
        //paintSkinnyLine4.setAlpha(78);
        //paintSkinnyLine4.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintSkinnyLine4.setColor(Color.BLUE);
        paintSkinnyLine4.setStrokeWidth(372);
        paintSkinnyLine4.setStrokeCap(Paint.Cap.ROUND);
        paintSkinnyLine4.setStrokeJoin(Paint.Join.ROUND);
        paintSkinnyLine4.setStyle(Paint.Style.STROKE);
        //paintSkinnyLine4.setShadowLayer(1,0,0, Color.GREEN);

        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        blackPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));

        aLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        aLinePaint.setColor(Color.WHITE);
        aLinePaint.setStrokeWidth(5);

        bLinePaint.setAntiAlias(true);
        bLinePaint.setStyle(Paint.Style.STROKE);
        bLinePaint.setColor((int) settings.get("section1ThickPathColor"));
        bLinePaint.setStrokeCap(Paint.Cap.BUTT);
        bLinePaint.setStrokeJoin(Paint.Join.ROUND);
        bLinePaint.setStrokeWidth(Integer.parseInt(settings.get("section1Width").toString()));
        bLinePaint.setBlendMode(BlendMode.DARKEN);
        bLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        // bLinePaint.setAlpha(78);



        rLinePaint.setAntiAlias(true);
        rLinePaint.setStyle(Paint.Style.STROKE);
        rLinePaint.setColor((int) settings.get("section1PathColor"));
        rLinePaint.setStrokeCap(Paint.Cap.BUTT);
        rLinePaint.setStrokeJoin(Paint.Join.ROUND);
        rLinePaint.setStrokeWidth(6);
        rLinePaint.setBlendMode(BlendMode.DARKEN);
        rLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        //rLinePaint.setAlpha(78);
        // bLinePaint.setShadowLayer(15,0,0, Color.RED);
        //bLinePaint.setShadowLayer(75,0,0, Color.RED);

        lLinePaint.setAntiAlias(true);
        lLinePaint.setStyle(Paint.Style.STROKE);
        lLinePaint.setColor((int) settings.get("section2PathColor"));
        lLinePaint.setStrokeCap(Paint.Cap.BUTT);
        lLinePaint.setStrokeJoin(Paint.Join.ROUND);
        lLinePaint.setStrokeWidth(6);
        lLinePaint.setBlendMode(BlendMode.DARKEN);
        lLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        // lLinePaint.setAlpha(78);

        rThickLinePaint.setAntiAlias(true);
        rThickLinePaint.setStyle(Paint.Style.STROKE);
        rThickLinePaint.setColor((int) settings.get("section1ThickPathColor"));
        rThickLinePaint.setStrokeCap(Paint.Cap.BUTT);
        rThickLinePaint.setStrokeJoin(Paint.Join.ROUND);
        rThickLinePaint.setStrokeWidth(Integer.parseInt(settings.get("section1Width").toString()));
        rThickLinePaint.setBlendMode(BlendMode.DARKEN);
        rThickLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        //rThickLinePaint.setAlpha(78);
        // bLinePaint.setShadowLayer(15,0,0, Color.RED);
        //bLinePaint.setShadowLayer(75,0,0, Color.RED);

        lThickLinePaint.setAntiAlias(true);
        lThickLinePaint.setStyle(Paint.Style.STROKE);
        lThickLinePaint.setColor((int) settings.get("section2ThickPathColor"));
        lThickLinePaint.setStrokeCap(Paint.Cap.BUTT);
        lThickLinePaint.setStrokeJoin(Paint.Join.ROUND);
        lThickLinePaint.setStrokeWidth(Integer.parseInt(settings.get("section2Width").toString()));
        lThickLinePaint.setBlendMode(BlendMode.DARKEN);
        lThickLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        //lThickLinePaint.setAlpha(78);


        cLinePaint.setAntiAlias(true);
        cLinePaint.setStyle(Paint.Style.STROKE);
        cLinePaint.setColor(Color.WHITE);
        cLinePaint.setStrokeCap(Paint.Cap.ROUND);
        cLinePaint.setStrokeJoin(Paint.Join.ROUND);
        cLinePaint.setStrokeWidth(6);

        labelPaint.setAntiAlias(true);
        labelPaint.setTextSize(20);
        labelPaint.setColor(Color.BLACK);
        parallelPoints.add(new ArrayList<>());
        parallelPoints.add(new ArrayList<>());
        // int dbAid = Integer.parseInt(aidText.getText().toString());
        Application application = (Application) context.getApplicationContext();
        AppDataVM = ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(ApplicationsDataViewModel.class);
        //AppDataVM.reloadAllApplicationsData(1)

        myObserver = applications -> {
            //isForTouch = true;
            points = new ArrayList<Point>();
            if (applications != null && !applications.isEmpty() && applications.size() > 1) {
                lastApplications = applications;
                processApplicationData(applications);
            }
        };

    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Initialize resources or listeners here

        Log.d(TAG, "adding observer for "+aid);
        AppDataVM.getAllApplicationsFromVm(aid).observe(getLifecycleOwner(), myObserver);
        /*AppDataVM.getAllApplicationsFromVm(aid).observe(getLifecycleOwner(), applications ->
        {
            //isForTouch = true;
            points = new ArrayList<Point>();
            if (applications != null && !applications.isEmpty() && applications.size() > 1) {
                lastApplications = applications;
                processApplicationData(applications);
            }
        });*/
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Clean up resources or listeners here
        Log.d(TAG, "removing observer for "+aid);
        AppDataVM.getAllApplicationsFromVm(aid).removeObserver(myObserver);
    }
    private void processApplicationData(List<ApplicationsData> applications) {
        absmaxX = Integer.MIN_VALUE;
        absmaxY = Integer.MIN_VALUE;
        absminY = Integer.MAX_VALUE;
        absminX = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE;
        minX = Integer.MAX_VALUE;
        points = new ArrayList<Point>();
        zeroPoints = new ArrayList<>();
        absPoints = new ArrayList<>();
        ApplicationsData firstApp = applications.get(0);
        ApplicationsData lastApp = applications.get(applications.size()-1);
        double clat = lastApp.lat;
        double clng = lastApp.lng;
        initialBearing = firstApp.bearing;
        cpoint = XY(clat, clng, clat, clng, perPixel, ww, hh);
        for (ApplicationsData appData : applications) {
            if (appData.speed == 0) continue;
            double lat = appData.lat;
            double lng = appData.lng;
            if (lat == 0 || lng == 0) continue;
            Point zpoint = XY(clat, clng, lat, lng, perPixel, 0, 0);
            Point tpoint = XY(clat, clng, lat, lng, perPixel, ww, hh);
            String isSpraying;
            try {
                JSONObject jsonObject = new JSONObject(appData.isSpraying);
                isSpraying = jsonObject.getString("master");
            } catch (JSONException e) {
                e.printStackTrace();
                isSpraying = "0";
            }
            zpoint.isSpraying = isSpraying;
            tpoint.isSpraying = isSpraying;
            points.add(tpoint);
            zeroPoints.add(zpoint);
            //Calculate min/max values for x and y and set offsets that will bring the x/y coordinates relative to 0,0
            float currentPointX = zpoint.x;
            float currentPointY = zpoint.y;
            if (minX > currentPointX) {
                minX = currentPointX;
            }
            if (minY > currentPointY) {
                minY = currentPointY;
            }
            if (maxX < currentPointX) {
                maxX = currentPointX;
            }
            if (maxY < currentPointY) {
                maxY = currentPointY;
            }
        }
        if (zeroPoints.size() < 2) return;
        float xAbsOffset = 0;
        float yAbsOffset = 0;
        if (minX < 0) {
            xAbsOffset = Math.abs(minX);
        }
        if (minX > 0) {
            xAbsOffset = -minX;
        }
        if (minY < 0) {
            yAbsOffset = Math.abs(minY);
        }
        if (minY > 0) {
            yAbsOffset = -minY;
        }
        setMinMax = true;
        desiredWidth = (int) Math.abs(maxX-minX);
        desiredHeight = (int) Math.abs(maxY-minY);
        cpoint = XY(clat, clng, clat, clng, perPixel, desiredWidth, desiredHeight);
        ApplicationsData lastPoint = new ApplicationsData();
        lastPoint.lat = 0d;
        lastPoint.lng = 0d;
        for (ApplicationsData appData : applications) {
            //Is this really needed?
            if (appData.speed == 0) continue;
            double lat = appData.lat;
            double lng = appData.lng;
            if (lat == 0 || lng == 0) continue;
            currentBearing = appData.bearing;
            Point zpoint = XY(clat, clng, lat, lng, perPixel, 0, 0);
            //Point tpoint = XY(clat, clng, lat, lng, perPixel, desiredWidth, desiredHeight);
            Point absPoint = new Point();
            String isSpraying = "0";
            String sectionState1 = "0";
            String sectionState2 = "0";
            try {
                JSONObject jsonObject = new JSONObject(appData.isSpraying);
                isSpraying = jsonObject.getString("master");
                sectionState1 = jsonObject.getString("0");
                sectionState2 = jsonObject.getString("1");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "isSrpaying: "+isSpraying);
            absPoint.isSpraying = isSpraying;
            absPoint.sectionState1 = sectionState1;
            absPoint.sectionState2 = sectionState2;
            //Calculate Distance from last point...
            if (lastPoint.lat == 0 && lastPoint.lng == 0) {
                absPoint.distance = distance(clat, clng, lat, lng) * 39370.1d;
            } else {
                absPoint.distance = distance(lastPoint.lat, lastPoint.lng, lat, lng) * 39370.1d;
            }
            //If we have moved since the last point...
            if (lastPoint.lat != appData.lat && lastPoint.lng != appData.lng) {
                //If we have moved at least this far....
                if (absPoint.distance > minMoventInInches) {
                    //Add offsets to make coordinates relative to 0,0
                    absPoint.x = zpoint.x + xAbsOffset;
                    absPoint.y = zpoint.y + yAbsOffset;
                    absPoint.bearing = appData.bearing;
                    absPoints.add(absPoint);
                    lastPoint = appData;
                    //Calculate min/max values for x and y and set offsets that we will use to calculate final dimensions and center the x/y coordinates in the view
                    float currentPointX = absPoint.x;
                    float currentPointY = absPoint.y;
                    if (absminX > currentPointX) {
                        absminX = currentPointX;
                    }
                    if (absminY > currentPointY) {
                        absminY = currentPointY;
                    }

                    if (absmaxX < currentPointX) {
                        absmaxX = currentPointX;
                    }
                    if (absmaxY < currentPointY) {
                        absmaxY = currentPointY;
                    }
                }
            }
        }
        desiredWidth = (int) absmaxX*2;
        desiredHeight = (int) Math.abs(absmaxY)*2;
        float xOffset = 0;
        float yOffset = 0;
        if (absmaxX < mapView.getLayoutParams().width) {
            xOffset = (mapView.getLayoutParams().width -  absmaxX) / 2;
        } else {
            xOffset = absmaxX/2;
        }
        if (absmaxY < mapView.getLayoutParams().height) {
            yOffset = (mapView.getLayoutParams().height - absmaxY) / 2;
        } else {
            yOffset = absmaxY/2;
        }
        //Add our offsets to our x/y coordinates
        for (Point point : absPoints) {
            point.x = point.x + xOffset+thickStroke;
            point.y = point.y + yOffset+thickStroke;
        }
        absmaxX = Math.max(absmaxX, mapView.getLayoutParams().width);
        absmaxY = Math.max(absmaxX, mapView.getLayoutParams().height);
        absmaxX = Math.max(absmaxX, desiredWidth);
        absmaxY = Math.max(absmaxY, desiredHeight);
        absmaxX += (thickStroke*2);
        absmaxY += (thickStroke*2);

        cpoint.x -= xOffset+thickStroke;
        cpoint.y -= yOffset+thickStroke;


        Log.d(TAG, "section1 width/2: "+Integer.parseInt(settings.get("section1Width").toString())/2);

        //Calculate parallel offsets
        for(int i = 0; i < absPoints.size(); i++){
            if(i >= 0){
                Point point = absPoints.get(i);

                if(i == 0){
                    Point next = absPoints.get(i + 1);
                    double degrees = Math.toDegrees(Math.atan2((double) (next.y-point.y), (double) (next.x-point.x)));
                    int offsetAngle = (int)degrees+90;
                    double[] offsetPoints = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section2Width")).toString()) /2, offsetAngle);
                    Point lpoint = new Point();
                    lpoint.x = (float)offsetPoints[0];
                    lpoint.y =  (float)offsetPoints[1];

                    Point rpoint = new Point();
                    rpoint.x = (float)offsetPoints[2];
                    rpoint.y =  (float)offsetPoints[3];

                    parallelPoints.get(0).add(lpoint);

                    double[] offsetPoints2 = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section1Width")).toString()) /2, offsetAngle);
                    Point lpoint2 = new Point();
                    lpoint2.x = (float)offsetPoints2[0];
                    lpoint2.y =  (float)offsetPoints2[1];

                    Point rpoint2 = new Point();
                    rpoint2.x = (float)offsetPoints2[2];
                    rpoint2.y =  (float)offsetPoints2[3];
                    parallelPoints.get(1).add(rpoint2);

                }
                else if(i == absPoints.size() - 1){
                    Point prev = absPoints.get(i - 1);

                    double degrees = Math.toDegrees(Math.atan2((double) (point.y-prev.y), (double) (point.x-prev.x)));
                    int offsetAngle = (int)degrees+90;
                    if (i == 1) {
                        double[] offsetPoints = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section2Width")).toString()) /2, offsetAngle);
                        Point lpoint = new Point();
                        lpoint.x = (float)offsetPoints[0];
                        lpoint.y =  (float)offsetPoints[1];

                        Point rpoint = new Point();
                        rpoint.x = (float)offsetPoints[2];
                        rpoint.y =  (float)offsetPoints[3];

                        parallelPoints.get(0).add(lpoint);
                        double[] offsetPoints2 = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section1Width")).toString()) /2, offsetAngle);
                        Point lpoint2 = new Point();
                        lpoint2.x = (float)offsetPoints2[0];
                        lpoint2.y =  (float)offsetPoints2[1];

                        Point rpoint2 = new Point();
                        rpoint2.x = (float)offsetPoints2[2];
                        rpoint2.y =  (float)offsetPoints2[3];

                        parallelPoints.get(1).add(rpoint2);
                    }
                    //Log.d(TAG, "degrees: "+degrees);
                    double[] offsetPoints = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section2Width")).toString()) /2, offsetAngle);
                    Point lpoint = new Point();
                    lpoint.x = (float)offsetPoints[0];
                    lpoint.y =  (float)offsetPoints[1];

                    Point rpoint = new Point();
                    rpoint.x = (float)offsetPoints[2];
                    rpoint.y =  (float)offsetPoints[3];

                    parallelPoints.get(0).add(lpoint);
                    double[] offsetPoints2 = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section1Width")).toString()) /2, offsetAngle);
                    Point lpoint2 = new Point();
                    lpoint2.x = (float)offsetPoints2[0];
                    lpoint2.y =  (float)offsetPoints2[1];

                    Point rpoint2 = new Point();
                    rpoint2.x = (float)offsetPoints2[2];
                    rpoint2.y =  (float)offsetPoints2[3];
                    parallelPoints.get(1).add(rpoint2);
                }
                else{
                    Point next = absPoints.get(i + 1);
                    Point prev = absPoints.get(i - 1);

                    double degrees = Math.toDegrees(Math.atan2((double) (point.y-prev.y), (double) (point.x-prev.x)));
                    int offsetAngle = (int)degrees+90;
                    //Log.d(TAG, "degrees: "+degrees);
                    double[] offsetPoints = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section2Width")).toString()) /2, offsetAngle);
                    Point lpoint = new Point();
                    lpoint.x = (float)offsetPoints[0];
                    lpoint.y =  (float)offsetPoints[1];

                    Point rpoint = new Point();
                    rpoint.x = (float)offsetPoints[2];
                    rpoint.y =  (float)offsetPoints[3];

                    parallelPoints.get(0).add(lpoint);
                    double[] offsetPoints2 = rotate_point_about_point(point.x, point.y, (float) Integer.parseInt(Objects.requireNonNull(settings.get("section1Width")).toString()) /2, offsetAngle);
                    Point lpoint2 = new Point();
                    lpoint2.x = (float)offsetPoints2[0];
                    lpoint2.y =  (float)offsetPoints2[1];

                    Point rpoint2 = new Point();
                    rpoint2.x = (float)offsetPoints2[2];
                    rpoint2.y =  (float)offsetPoints2[3];

                    parallelPoints.get(1).add(rpoint2);

                }
            }
        }
        //Normalize parallel offsets
        for(int ii = 0; ii < parallelPoints.size(); ii++) {
            List<Point> oPoints = parallelPoints.get(ii);

            for(int i = 0; i < oPoints.size(); i++){
                if(i >= 0){
                    Point point = oPoints.get(i);

                    if(i == 0){
                        Point next = oPoints.get(i + 1);
                        point.dx = ((next.x - point.x) / SMOOTH_VAL);
                        point.dy = ((next.y - point.y) / SMOOTH_VAL);
                    }
                    else if(i == oPoints.size() - 1){
                        Point prev = oPoints.get(i - 1);
                        point.dx = ((point.x - prev.x) / SMOOTH_VAL);
                        point.dy = ((point.y - prev.y) / SMOOTH_VAL);
                    }
                    else{
                        Point next = oPoints.get(i + 1);
                        Point prev = oPoints.get(i - 1);
                        point.dx = ((next.x - prev.x) / SMOOTH_VAL);
                        point.dy = ((next.y - prev.y) / SMOOTH_VAL);
                    }

                    parallelPoints.get(ii).set(i, point);

                }
            }
        }
        //Normalize main points
        for(int i = 0; i < absPoints.size(); i++){
            if(i >= 0){
                Point point = absPoints.get(i);

                if(i == 0){
                    Point next = absPoints.get(i + 1);
                    point.dx = ((next.x - point.x) / SMOOTH_VAL);
                    point.dy = ((next.y - point.y) / SMOOTH_VAL);
                }
                else if(i == absPoints.size() - 1){
                    Point prev = absPoints.get(i - 1);
                    point.dx = ((point.x - prev.x) / SMOOTH_VAL);
                    point.dy = ((point.y - prev.y) / SMOOTH_VAL);
                }
                else{
                    Point next = absPoints.get(i + 1);
                    Point prev = absPoints.get(i - 1);
                    point.dx = ((next.x - prev.x) / SMOOTH_VAL);
                    point.dy = ((next.y - prev.y) / SMOOTH_VAL);
                }
                absPoints.set(i, point);
            }
        }






        //if (!resizedDims) {
        if ((mapView.getLayoutParams().width != (int) absmaxX ||  mapView.getLayoutParams().height != (int) absmaxY))    {
            //
            // Log.d(TAG, "current layout parent dims: " + mapView.getLayoutParams().width + " " + mapView.getLayoutParams().height);
            // Log.d(TAG, "bottom: " + mapView.getTop() + " " + mapView.getBottom() + " " + mapView.getLeft() + " " + mapView.getRight());
            // Log.d(TAG, "requested layout dims: " + absmaxX + " " + absmaxY);
            mapView.getLayoutParams().width = (int) absmaxX; //Math.max(absmaxX, absmaxY);
            mapView.getLayoutParams().height = (int) absmaxY; //Math.max(absmaxY, absmaxX);
            requestLayout();
            resizedDims = true;
        } else {
            invalidate();
        }
    }
    public static float[] getScreenDimensions(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return new float[]{size.x, size.y}; // returns width and height in pixels
    }
    private void fitContentToScreen() {
        matrix.set(originalMatrix);
        invalidate();
        // Get the dimensions of the view
        // int viewWidth = getWidth();
        // int viewHeight = getHeight();
       /* DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int viewWidth = displayMetrics.widthPixels;
        int viewHeight = displayMetrics.heightPixels;
        // Calculate the scale factors for both dimensions
        float scaleX = (float) viewWidth / absmaxX;
        float scaleY = (float) viewHeight / absmaxY;
        Log.d(TAG, "fitContentToScreen: w/h"+viewHeight+" "+viewHeight+" scalex/scaley "+scaleX+" "+scaleY+" absmax "+absmaxX+" "+absmaxY);
        // Use the smaller scale factor to maintain aspect ratio
        float scale = Math.min(scaleX, scaleY);
        savedMatrix.set(matrix);
        matrix.set(savedMatrix);
        // Apply the scaling to the matrix
        matrix.setScale(scale, scale, absmaxX/2, absmaxY/2);
        this.setAnimationMatrix(matrix);
        invalidate(); // Redraw the view*/
    }
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        return distance;
    }
    private LifecycleOwner getLifecycleOwner() {
        Context context = getContext();
        while (!(context instanceof LifecycleOwner)) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        return (LifecycleOwner) context;
    }
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }
    private void drawGridLabel(Canvas canvas) {
        float cols = 0;
        float rows = 0;

        int density = 1;

        float[] f = new float[9];
        matrix.getValues(f);
        float scaleX = f[Matrix.MSCALE_X];
        float scaleY = f[Matrix.MSCALE_Y];
        scaleX = scaleFactor;
        Log.d(TAG, "mapView matrix scale: "+scaleX+" "+scaleY);
        if (canvas.getDensity() > density) density = canvas.getDensity();
        if (scaleX > 4) {
            //1 foot
            cols = absmaxX / (120 * density); //(1 x 12 x 10)
            rows = absmaxY / (120 * density);
            paintMainLine.setStrokeWidth(6);
            paintThickLine.setStrokeWidth(thickStroke);
            gridSizeText = "10 ft";
        } else if (scaleX <= 4 && scaleX > 2) {
            //10 feet
            cols = absmaxX / (120 * density); //(1 x 12 x 10)
            rows = absmaxY / (120 * density);
            paintMainLine.setStrokeWidth(6);
            paintThickLine.setStrokeWidth(thickStroke);
            gridSizeText = "1o ft";
        } else if (scaleX <= 2 && scaleX > .8) {
            //25 feet
            cols = absmaxX / (300 * density); //(1 x 12 x 25)
            rows = absmaxY / (300 * density);
            paintMainLine.setStrokeWidth(6);
            paintThickLine.setStrokeWidth(thickStroke);
            gridSizeText = "25 ft";
        } else if (scaleX <= .8 && scaleX > .4) {
            //50 feet
            cols = absmaxX / (600 * density); //(1 x 12 x 50)
            rows = absmaxY / (600 * density);
            paintMainLine.setStrokeWidth(6);
            paintThickLine.setStrokeWidth(thickStroke);
            gridSizeText = "50 ft";
        } else if (scaleX <= .4 && scaleX > .2) {
            //100 feet
            cols = absmaxX / (1200 * density); //(1 x 12 x 100)
            rows = absmaxY / (1200 * density);
            paintMainLine.setStrokeWidth(6);
            paintThickLine.setStrokeWidth(thickStroke);
            gridSizeText = "100 ft";
        } else if (scaleX <= .2 && scaleX > .08) {
            //250 feet
            cols = absmaxX / (3000 * density); //(1 x 12 x 250)
            rows = absmaxY / (3000 * density);
            paintMainLine.setStrokeWidth(6);
            paintThickLine.setStrokeWidth(thickStroke);
            gridSizeText = "250 ft";
        }  else if (scaleX <= .08 && scaleX > .04) {
            //500 feet
            cols = absmaxX / (6000 * density); //(1 x 12 x 500)
            rows = absmaxY / (6000 * density);
            paintMainLine.setStrokeWidth(12);
            paintThickLine.setStrokeWidth(thickStroke*2);
            gridSizeText = "500 ft";
        }   else if (scaleX <= .04 && scaleX > .01) {
            //1320 feet
            cols = absmaxX / (15840 * density); //(1 x 12 x 1320)
            rows = absmaxY / (15840 * density);
            paintMainLine.setStrokeWidth(18);
            paintThickLine.setStrokeWidth(thickStroke*3);
            gridSizeText = "1/4 mile";
        } else {
            //2640 feet
            cols = absmaxX / (31680 * density); //(1 x 12 x 2640)
            rows = absmaxY / (31680 * density);
            paintMainLine.setStrokeWidth(36);
            paintThickLine.setStrokeWidth(thickStroke*6);
            gridSizeText = "1/2 mile";
        }
        gridSizeLabel.setText(gridSizeText);
        setNumColumns(Math.round(cols));
        setNumRows(Math.round(rows));
    }
    public void drawGrid(Canvas canvas, Point point, Point point2) {
        //Log.v(TAG, "numColumns: "+numColumns+" numRows: "+numRows);
        //Log.v(TAG, "cpoint: "+cpoint.x+" "+cpoint.y);
        if (numColumns != 0 && numRows != 0) {


            int width = getWidth();
            int height = getHeight();
            int offsetAngle = 90;
            double[] offsetPoints = rotate_point_about_point(point.x, point.y, width, offsetAngle);
            Point lpoint = new Point();
            lpoint.x = (float)offsetPoints[0];
            lpoint.y =  (float)offsetPoints[1];

            Point rpoint = new Point();
            rpoint.x = (float)offsetPoints[2];
            rpoint.y =  (float)offsetPoints[3];

            for (int li = (int) cpoint.y; li < numColumns; li++) {
                canvas.drawLine(li * cellWidth, 0, li * cellWidth, height, blackPaint);
            }

            for (int li = (int) cpoint.x; li < numRows; li++) {
                canvas.drawLine(0, li * cellHeight, width, li * cellHeight, blackPaint);
            }
        }
    }
    public void drawBLine(Canvas canvas, Point point) {
        int width = getWidth();
        //canvas.drawLine(0, point.y, width, point.y, bLinePaint);
        int offsetAngle = 90;
        double[] offsetPoints = rotate_point_about_point(point.x, point.y, width, offsetAngle);
        Point lpoint = new Point();
        lpoint.x = (float)offsetPoints[0];
        lpoint.y =  (float)offsetPoints[1];

        Point rpoint = new Point();
        rpoint.x = (float)offsetPoints[2];
        rpoint.y =  (float)offsetPoints[3];

        canvas.drawLine(lpoint.x, lpoint.y, rpoint.x, rpoint.y, bLinePaint);
    }
    public void drawALine(Canvas canvas, Point point, Point point2) {
        int width = getWidth();
        //canvas.drawLine(0, point.y, width, point.y, aLinePaint);
        double degrees = Math.toDegrees(Math.atan2((double) (point2.y-point.y), (double) (point2.x-point.x)));
        int offsetAngle = (int)degrees+90;
        //int offsetAngle = 90;
        double[] offsetPoints = rotate_point_about_point(point.x, point.y, width, offsetAngle);
        Point lpoint = new Point();
        lpoint.x = (float)offsetPoints[0];
        lpoint.y =  (float)offsetPoints[1];

        Point rpoint = new Point();
        rpoint.x = (float)offsetPoints[2];
        rpoint.y =  (float)offsetPoints[3];

        canvas.drawLine(lpoint.x, lpoint.y, rpoint.x, rpoint.y, aLinePaint);
    }
    private Point findPointOnLine(Point startPoint, float slope, int distance) {
        Point newPoint = new Point();
        double inf = Double.POSITIVE_INFINITY;
        double negInf = Double.NEGATIVE_INFINITY;

        if (slope == 0) {
            newPoint.x = startPoint.x + distance;
            newPoint.y = startPoint.y;

        } else if (slope == inf || slope == negInf) {

            newPoint.x = startPoint.x;
            newPoint.y = startPoint.y + distance;

        } else {
            double dx = (distance / Math.sqrt(1 + (slope * slope)));
            double dy = slope * dx;
            newPoint.dx = (float)dx;
            newPoint.dy = (float)dy;
            newPoint.x = startPoint.x + (float)dx;
            newPoint.y = startPoint.y + (float)dy;
        }
        return newPoint;
    }
    private double[] rotate_point_about_point(float x1, float y1, float shift, int angle) {
        //Convert angle to radians
        double angle_rad = Math.toRadians(angle);

        //Calculate sine and cosine of angle
        double cos_angle = Math.cos(angle_rad);
        double sin_angle = Math.sin(angle_rad);

        //Translate point 2 so that point 1 is at the origin
        float x2 = x1 + shift;
        float y2 = y1;
        x2 -= x1;
        y2 -= y1;

        //Rotate point 2 about the origin
        double right_x = x2 * cos_angle - y2 * sin_angle;
        double right_y = x2 * sin_angle + y2 * cos_angle;

        //Translate point 2 back to its original position relative to point 1
        right_x += x1;
        right_y += y1;
        //---------

        //Translate point 2 so that point 1 is at the origin
        float x3= x1 - shift;
        float y3 = y1;
        x3 -= x1;
        y3 -= y1;

        //Rotate point 2 about the origin
        double left_x = x3 * cos_angle - y3 * sin_angle;
        double left_y = x3 * sin_angle + y3 * cos_angle;

        //Translate point 2 back to its original position relative to point 1
        left_x += x1;
        left_y += y1;

        return new double[] {right_x, right_y, left_x, left_y};
    }
    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw");

        canvas.save();
        canvas.translate(translateX, translateY); // Apply translation
        canvas.scale(scaleFactor, scaleFactor); // Scale the canvas
        plotCanvas = canvas;
        //matrix.set(savedMatrix);
        //matrix.postTranslate(1,1 );
        //matrix.setRotate(currentRotation, mid.x, mid.y);
        // mapView.setAnimationMatrix(matrix);
        pathSkinnyLineL.reset();
        pathSkinnyLineR.reset();
        pathMainLine.reset();
        pathThickLine.reset();
        // Log.d(TAG, "abs min max: "+absminX+" "+absminY+" "+absmaxX+" "+absmaxY);
        if (absPoints.size() < 2) return;
        totalDistance = 0;
        Log.d(TAG, "lengths: "+absPoints.size()+" "+parallelPoints.get(0).size()+" "+parallelPoints.get(1).size());
        for(int i = 0; i < absPoints.size(); i++){
            if(i >= 0) {
                Point point = absPoints.get(i);
                totalDistance += point.distance;
                if (i == 0) {
                    Point next = absPoints.get(i + 1);
                    pathThickLine.moveTo(point.x, point.y);
                    pathMainLine.moveTo(point.x, point.y);
                    Point lpoint = parallelPoints.get(0).get(i);
                    Point rpoint = parallelPoints.get(1).get(i);
                    if (Objects.equals(point.isSpraying, "1")) {

                        pathSkinnyLineL.moveTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                        //pathSkinnyLineR.lineTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                    } else {
                        pathSkinnyLineL.moveTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                    }
                } else if(i == absPoints.size() - 1) {
                    Point prev = absPoints.get(i - 1);
                    //pathThickLine.moveTo(prev.x, prev.y);
                    //pathMainLine.moveTo(prev.x, prev.y);
                    //pathMainLine.lineTo(point.x, point.y);
                    pathMainLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);

                    Point lprev = parallelPoints.get(0).get(i - 1);
                    Point rprev = parallelPoints.get(1).get(i - 1);
                    Point lpoint = parallelPoints.get(0).get(i);
                    Point rpoint = parallelPoints.get(1).get(i);
                    if (Objects.equals(point.isSpraying, "1")) {
                        //pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        //pathSkinnyLineL.lineTo(lpoint.x, lpoint.y);
                        if (Objects.equals(point.sectionState1, "1")) {
                            pathSkinnyLineL.quadTo(lprev.x, lprev.y, lpoint.x, lpoint.y);
                        } else {
                            pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        }
                        if (Objects.equals(point.sectionState2, "1")) {
                            pathSkinnyLineR.quadTo(rprev.x, rprev.y, rpoint.x, rpoint.y);
                        } else {
                            pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                        }
                        //pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                        //pathSkinnyLineR.lineTo(rpoint.x, rpoint.y);
                    } else {
                        pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                    }
                    //pathSkinnyLineL.cubicTo(lprev.x + lprev.dx, lprev.y + lprev.dy, lpoint.x - lpoint.dx, lpoint.y - lpoint.dy, lpoint.x, lpoint.y);
                    //pathSkinnyLineR.cubicTo(rprev.x + rprev.dx, rprev.y + rprev.dy, rpoint.x - rpoint.dx, rpoint.y - rpoint.dy, rpoint.x, rpoint.y);
                    // pathMainLine.setLastPoint(point.x, point.y);
                    //pathThickLine.setLastPoint(lastX, lastY);
                    // pathSkinnyLineL.setLastPoint(lpoint.x, lpoint.y);
                    // pathSkinnyLineR.setLastPoint(rpoint.x, rpoint.y);
                } else {
                    Point next = absPoints.get(i + 1);
                    Point prev = absPoints.get(i - 1);
                    //pathThickLine.moveTo(point.x, point.y);
                    //pathMainLine.moveTo(point.x, point.y);
                    pathMainLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);

                    Point lprev = parallelPoints.get(0).get(i - 1);
                    Point rprev = parallelPoints.get(1).get(i - 1);
                    Point lpoint = parallelPoints.get(0).get(i);
                    Point rpoint = parallelPoints.get(1).get(i);
                    if (Objects.equals(point.isSpraying, "1")) {
                        if (Objects.equals(point.sectionState1, "1")) {
                            pathSkinnyLineL.cubicTo(lprev.x + lprev.dx, lprev.y + lprev.dy, lpoint.x - lpoint.dx, lpoint.y - lpoint.dy, lpoint.x, lpoint.y);

                        } else {
                            pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        }
                        if (Objects.equals(point.sectionState2, "1")) {
                            pathSkinnyLineR.cubicTo(rprev.x + rprev.dx, rprev.y + rprev.dy, rpoint.x - rpoint.dx, rpoint.y - rpoint.dy, rpoint.x, rpoint.y);
                        } else {
                            pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                        }
                    } else {
                        pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                    }
                }

            }
        }
        if (1 == 2) {
            //Backwards loop to try and close the lines?
            int s = absPoints.size() - 1;
            for (int i = s; i >= 0; i--) {
                if (i >= 0) {
                    Point point = absPoints.get(i);
                    //totalDistance += point.distance;
                    if (i == 0) {
                        Point prev = absPoints.get(i + 1);
                        //pathThickLine.moveTo(prev.x, prev.y);
                        //pathMainLine.moveTo(prev.x, prev.y);
                        //pathMainLine.lineTo(point.x, point.y);
                        pathMainLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                        Point lprev = parallelPoints.get(1).get(i + 1);
                        Point rprev = parallelPoints.get(0).get(i + 1);
                        Point lpoint = parallelPoints.get(1).get(i);
                        Point rpoint = parallelPoints.get(0).get(i);
                        //pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        //pathSkinnyLineL.lineTo(lpoint.x, lpoint.y);
                        pathSkinnyLineL.quadTo(lprev.x, lprev.y, lpoint.x, lpoint.y);
                        //pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                        //pathSkinnyLineR.lineTo(rpoint.x, rpoint.y);
                        pathSkinnyLineR.quadTo(rprev.x, rprev.y, rpoint.x, rpoint.y);


                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                        pathSkinnyLineR.lineTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);

                        //pathSkinnyLineL.cubicTo(lprev.x + lprev.dx, lprev.y + lprev.dy, lpoint.x - lpoint.dx, lpoint.y - lpoint.dy, lpoint.x, lpoint.y);
                        //pathSkinnyLineR.cubicTo(rprev.x + rprev.dx, rprev.y + rprev.dy, rpoint.x - rpoint.dx, rpoint.y - rpoint.dy, rpoint.x, rpoint.y);
                        pathMainLine.setLastPoint(point.x, point.y);
                        //pathThickLine.setLastPoint(lastX, lastY);
                        pathSkinnyLineL.setLastPoint(lpoint.x, lpoint.y);
                        pathSkinnyLineR.setLastPoint(rpoint.x, rpoint.y);

                    } else if (i == absPoints.size() - 1) {

                        //Point next = absPoints.get(i + 1);
                        pathThickLine.moveTo(point.x, point.y);
                        pathMainLine.moveTo(point.x, point.y);
                        Point lpoint = parallelPoints.get(1).get(i);
                        Point rpoint = parallelPoints.get(0).get(i);
                        pathSkinnyLineL.moveTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                        pathSkinnyLineR.lineTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                    } else {
                        Point next = absPoints.get(i + 1);
                        Point prev = absPoints.get(i + 1);
                        //pathThickLine.moveTo(point.x, point.y);
                        //pathMainLine.moveTo(point.x, point.y);
                        pathMainLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                        Point lprev = parallelPoints.get(1).get(i + 1);
                        Point rprev = parallelPoints.get(0).get(i + 1);
                        Point lpoint = parallelPoints.get(1).get(i);
                        Point rpoint = parallelPoints.get(0).get(i);
                        pathSkinnyLineL.cubicTo(lprev.x + lprev.dx, lprev.y + lprev.dy, lpoint.x - lpoint.dx, lpoint.y - lpoint.dy, lpoint.x, lpoint.y);
                        pathSkinnyLineR.cubicTo(rprev.x - rprev.dx, rprev.y - rprev.dy, rpoint.x + rpoint.dx, rpoint.y + rpoint.dy, rpoint.x, rpoint.y);
                        //  pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                        // pathSkinnyLineR.lineTo(rprev.x, rprev.y);
                    }

                }
            }
        }



        //paintMainLine.setPathEffect(new CornerPathEffect(5));
        //paintMainLine.setPathEffect(new DiscretePathEffect(0,1));

        if (resizedDims) {
            //Move to bottom left
            //canvas.translate(-(Math.abs(maxX)-Math.abs(minX)), Math.abs(minY)+Math.abs(maxY));
            //Move to top left
            //canvas.translate(-(Math.abs(maxX)-Math.abs(minX)), 0);
        }


        //Point firstPoint = absPoints.get(0);
        // Point lastPoint = absPoints.get(absPoints.size()-1);
        //if (currentBearing != initialBearing) canvas.rotate((initialBearing-currentBearing)+180, canvas.getWidth()/2, canvas.getHeight()/2);
        //float currentHeadingInverse = (initialBearing-currentBearing);
        // float currentHeadingOffset = 360f-currentHeadingInverse;
        //canvas.rotate(currentHeadingInverse+currentHeadingOffset, canvas.getWidth()/2, canvas.getHeight()/2);



        //Makes it always face same dir?
        //canvas.rotate(currentHeadingInverse+currentHeadingOffset, canvas.getWidth()/2, canvas.getHeight()/2);

        //pathMainLine.op(pathMainLine, Path.Op.INTERSECT);

        //Bitmap bitmap = Bitmap.createBitmap((int)absmaxY, (int)absmaxY, Bitmap.Config.ARGB_8888);
        // Canvas canvas2 = new Canvas(bitmap);


        canvas.drawPath(pathSkinnyLineR, rThickLinePaint);
        canvas.drawPath(pathSkinnyLineL, lThickLinePaint);
        canvas.drawPath(pathSkinnyLineR, rLinePaint);
        canvas.drawPath(pathSkinnyLineL, lLinePaint);
        //pathMainLine.op(pathSkinnyLineR, Path.Op.INTERSECT);
        //pathSkinnyLineR.close();
        //canvas.drawPath(pathSkinnyLineR, cLinePaint);

        //canvas.drawPath(pathSkinnyLineL, cLinePaint);
        canvas.drawPath(pathMainLine, paintThickLine);
        canvas.drawPath(pathMainLine, paintMainLine);
        //canvas.drawText("Total Distance: "+Math.round(totalDistance),  absPoints.get(absPoints.size()-1).x+50, absPoints.get(absPoints.size()-1).y+50, labelPaint);
        String distanceText;
        if (totalDistance/12 < 1320) {
            distanceText = round(totalDistance / 12, 2)+" ft";
        } else {
            distanceText = round(totalDistance / 12 / 5280, 2)+" mi";
        }
        distanceLabel.setText(distanceText);
        drawGridLabel(canvas);
        drawGrid(canvas, absPoints.get(0), absPoints.get(1));
        drawALine(canvas, absPoints.get(0), absPoints.get(1));
        //drawALine(canvas, absPoints.get(absPoints.size()-1), absPoints.get(absPoints.size()-2));
        if (1 == 2 && !isForTouch) {
            matrix.set(savedMatrix);
            //matrix.postScale(.0036374696f, .0036374696f,179334, 326795);
            //Log.d(TAG, "prescale: x "+((canvas.getWidth() / 2) - absPoints.get(absPoints.size()-1).x)+" y "+((canvas.getHeight() / 2) - absPoints.get(absPoints.size()-1).y));

            //mapView.setAnimationMatrix(matrix);
            //matrix.set(savedMatrix);
            //savedMatrix.set(matrix);
            //matrix.set(savedMatrix);

            matrix.postTranslate(((canvas.getWidth() / 2) - absPoints.get(absPoints.size()-1).x), ((canvas.getHeight() / 2) - absPoints.get(absPoints.size()-1).y));
            //matrix.postTranslate(((canvas.getWidth() / 2) - lastX), ((canvas.getHeight() / 2) - lastY));
            //matrix.setRotate(currentRotation, mid.x, mid.y);
            mapView.setAnimationMatrix(matrix);
            //savedMatrix.set(matrix);
            isForTouch = false;
        } else {
            //isForTouch = false;
        }
        canvas.restore();
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    private void fitCanvasToScreen(View v) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        //int screenWidth = mapView.getWidth();
        //int screenHeight = mapView.getHeight();
        RectF viewRect = new RectF(0, 0, screenWidth, screenHeight);
        RectF drawableRect  = new RectF(0, 0, Math.abs(absmaxX),
                Math.abs(absmaxY));
        //savedMatrix.set(matrix);
        //matrix.reset();
        //Matrix matrix = new Matrix();
        Log.d(TAG, "fitCanvasToScreen screem: "+screenWidth+"x"+screenHeight+"  canvas: "+Math.abs(absmaxX)+"x"+Math.abs(absmaxY));
        boolean canFitRect = matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.START);
        Log.d(TAG, "fitCanvasToScreen canFitRect: "+canFitRect);
        mapView.setAnimationMatrix(matrix);
        invalidate();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                isZoom = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    isDragging = false;
                    isZoom = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    translateX += dx;
                    translateY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate(); // Redraw the view
                }
                if (isZoom) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        gestureDetector.onTouchEvent(event); // Handle other gestures
                        scaleDetector.onTouchEvent(event); // Handle scaling
                        //invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true; // Indicate the event was handled
    }
    /** Determine the space between the first two fingers */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    /** Calculate the mid point of the first two fingers */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float targetScale = scaleFactor * detector.getScaleFactor();
            animateScale(scaleFactor, targetScale); // Animate scaling
            return true; // Indicate the event was handled
        }
    }

    private void animateScale(float startScale, float endScale) {
        ValueAnimator animator = ValueAnimator.ofFloat(startScale, endScale);
        animator.addUpdateListener(animation -> {
            scaleFactor = (float) animation.getAnimatedValue();
            invalidate(); // Request redraw
        });
        animator.setDuration(300); // Duration of the animation
        animator.start();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // Handle single tap
            return true; // Indicate the event was handled
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float targetScale = (scaleFactor == 1f) ? 2f : 1f; // Example: Toggle zoom
            animateScale(scaleFactor, targetScale); // Animate scaling
            return true; // Indicate the event was handled
        }
    }
    @Override
    public void draw(@NonNull Canvas canvas) {
        Log.d(TAG, "draw");
        super.draw(canvas);
        //mapView.getLayoutParams().width = 30232;
        //mapView.getLayoutParams().height = 54858;
        //requestLayout();

    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //Log.d(TAG, "onSizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
        if (plotCanvas != null) {
            //fitCanvasToScreen(plotCanvas);
        }
    }

    public Point XY(double centerLatitude, double centerLongitude, double Latitude, double Longitude, double MetersPerPixel, float windowWidth, float windowHeight) {
        double rto = 1/MetersPerPixel;
        double dLAT = ((centerLatitude - Latitude)/ 0.00001) * rto;
        double dLNG = -1 * ((centerLongitude - Longitude) / 0.00001) * rto;
        int y = (int)Math.round(dLAT);
        int x = (int)Math.round(dLNG);
        Point crd = new Point();
        crd.x = x+(windowWidth/2);
        crd.y = y+(windowHeight/2);
        //Log.v("xy", "x: "+x);
        // Log.v("xy", "y: "+y);
        return crd;
    }
    public void setViewRotation(float rotation) {
        mapCanvas.rotate(rotation);
    }
    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
        calculateDimensions();
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        calculateDimensions();
    }

    public int getNumRows() {
        return numRows;
    }


    private void calculateDimensions() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }

        cellWidth = getWidth() / numColumns;
        cellHeight = getHeight() / numRows;

        cellChecked = new boolean[numColumns][numRows];

        //invalidate();
    }


}

class Point3 extends android.graphics.Point implements Serializable {
    float x, y;
    float dx, dy;
    float bearing;
    double distance;
    String isSpraying;
    String sectionState1;
    String sectionState2;
    @Override
    public String toString() {

        return "x: "+x+" y: "+y+" dx: "+dx+" dy:"+dy+" bearing: "+bearing+" distance: "+distance;
    }
}