package com.benco.mapping;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.benco.mapping.data.ApplicationsData;
import com.benco.mapping.data.ApplicationsDataListAdapter;
import com.benco.mapping.domain.ApplicationsDataViewModel;
//import com.benco.mapping.ui.home.HomeViewModel;
import com.benco.mapping.MainActivity;

public class DrawView extends View implements OnTouchListener {
    private static final String TAG = "DrawView";
    private static final double EARTH_RADIUS = 6371; // Radius of the earth in kilometers
    private float ww = Integer.MAX_VALUE;
    private  float hh = Integer.MAX_VALUE;
    private float initialBearing = 0f; //Bearing of first point
    private float currentBearing = 0f; //Bearing of current point
    Canvas mapCanvas;
    View mapView;
    ApplicationsDataViewModel AppDataVM;
    private ArrayList<List<Point>> parallelPoints = new ArrayList<>();
    private List<Point> points = new ArrayList<Point>(); //Lat/Lng as x,y relative to first point
    private List<Point> zeroPoints = new ArrayList<>(); //Lat/Lng as x,y relative to 0,0
    private List<Point> absPoints = new ArrayList<>(); //Lat/Lng as x,y relative to offsetX, offsetY
    private Paint paintMainLine = new Paint(Paint.ANTI_ALIAS_FLAG); //Paint for the main line
    private Paint paintSkinnyLine2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintSkinnyLine3 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintSkinnyLine4 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintThickLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint aLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint lLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint cLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Path pathMainLine = new Path();
    Path pathSkinnyLineL = new Path();
    Path pathSkinnyLineR = new Path();
    Path pathThickLine = new Path();
    boolean setMinMax = false;
    boolean resizedDims = false;
    int desiredWidth = 0;
    int desiredHeight = 0;
    float absmaxX = 0;
    float absmaxY = 0;
    float absminY = Integer.MAX_VALUE;
    float absminX = Integer.MAX_VALUE;
    float maxX = 0;
    float maxY = 0;
    float minY = Integer.MAX_VALUE;
    float minX = Integer.MAX_VALUE;
    int aid = 0;
    private int numColumns, numRows;
    private int cellWidth, cellHeight;
    Paint labelPaint = new Paint();
    private boolean[][] cellChecked;
    Point cpoint;

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

    TextView gridSizeLabel;

    Boolean isForTouch = false;

    Canvas plotCanvas;
    //double perPixel = .04d; //1.574808 inches
    //double perPixel = 0.3048; //1 Foot
    //double perPixel = 10d;
    double perPixel = 0.025399999999999992029d; //1 inch
    int thickStroke = 120;
    Button resetButton;
    Observer<List<ApplicationsData>> myObserver;
    private List<ApplicationsData> lastApplications;
    public DrawView(Context context, View mapRView, TextView gridVSizeLabel, Button resetBtn) {
        super(context);
        Log.d(TAG, "DrawView");
        //setFocusable(true);
        // setFocusableInTouchMode(true);
        resetButton = resetBtn;
        matrix.reset();
        savedMatrix.reset();
        mapView = mapRView;
        //mapView.setOnTouchListener(new Touch());
        mapView.setOnTouchListener(this);
        gridSizeLabel = gridVSizeLabel;
        thickStroke = (int) Math.round((120f));
        Log.d(TAG, "thickStroke: "+thickStroke);
        paintThickLine.setAntiAlias(true);
        //paintThickLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        int thickColor = Color.argb(128, 255, 0, 0);
       // paintThickLine.setColor(thickColor);
        paintThickLine.setColor(Color.RED);
        paintThickLine.setStrokeWidth(thickStroke);
        paintThickLine.setStrokeCap(Paint.Cap.ROUND);
        paintThickLine.setStrokeJoin(Paint.Join.ROUND);
        paintThickLine.setStyle(Paint.Style.STROKE);
        //paintThickLine.setShadowLayer(5,0,0, Color.RED);
        resetButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //DO SOMETHING! {RUN SOME FUNCTION ... DO CHECKS... ETC}
                Log.d(TAG, "resetButton clicked");
                if (isForTouch) {
                    resetButton.setText("false");
                    isForTouch = false;



                } else {
                    resetButton.setText("true");
                    isForTouch = true;
                }
                //if (plotCanvas != null) {
                //    fitCanvasToScreen(plotCanvas);
                //}
            }
        });
       // paintMainLine.setStyle(Paint.Style.FILL_AND_STROKE);
        paintMainLine.setAntiAlias(true);
        //paintMainLine.setAlpha(78);
        //paintMainLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintMainLine.setColor(Color.GREEN);
        paintMainLine.setStrokeWidth(6);
        paintMainLine.setStrokeCap(Paint.Cap.ROUND);
        paintMainLine.setStrokeJoin(Paint.Join.ROUND);
        paintMainLine.setStyle(Paint.Style.STROKE);
        //paintMainLine.setShadowLayer(1,0,0, Color.GREEN);

        // paintMainLine.setStyle(Paint.Style.FILL_AND_STROKE);
        paintMainLine.setAntiAlias(true);
        //paintMainLine.setAlpha(78);
        //paintMainLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintMainLine.setColor(Color.GREEN);
        paintMainLine.setStrokeWidth(6);
        paintMainLine.setStrokeCap(Paint.Cap.ROUND);
        paintMainLine.setStrokeJoin(Paint.Join.ROUND);
        paintMainLine.setStyle(Paint.Style.STROKE);
        //paintMainLine.setShadowLayer(1,0,0, Color.GREEN);

        // paintMainLine.setStyle(Paint.Style.FILL_AND_STROKE);
        paintMainLine.setAntiAlias(true);
        //paintMainLine.setAlpha(78);
        //paintMainLine.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
        paintMainLine.setColor(Color.GREEN);
        paintMainLine.setStrokeWidth(6);
        paintMainLine.setStrokeCap(Paint.Cap.ROUND);
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
        bLinePaint.setColor(Color.argb(128, 255, 0, 0));
        bLinePaint.setStrokeCap(Paint.Cap.BUTT);
        bLinePaint.setStrokeJoin(Paint.Join.ROUND);
        bLinePaint.setStrokeWidth(300);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bLinePaint.setBlendMode(BlendMode.DARKEN);
        }
        bLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        bLinePaint.setAlpha(78);
        //bLinePaint.setShadowLayer(75,0,0, Color.RED);

        lLinePaint.setAntiAlias(true);
        lLinePaint.setStyle(Paint.Style.STROKE);
        lLinePaint.setColor(Color.argb(128, 0, 0, 255));
        lLinePaint.setStrokeCap(Paint.Cap.BUTT);
        lLinePaint.setStrokeJoin(Paint.Join.ROUND);
        lLinePaint.setStrokeWidth(300);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            lLinePaint.setBlendMode(BlendMode.DARKEN);
        }
        lLinePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        lLinePaint.setAlpha(78);
        bLinePaint.setShadowLayer(15,0,0, Color.RED);


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
        MainActivity.getText().observe(getLifecycleOwner(), aidText -> {
            //Log.d(TAG, "aidText: "+aidText.toString());
            if (aidText.equals("")) {
            } else {
                AppDataVM.getAllApplicationsFromVm(aid).removeObserver(myObserver);
                aid = Integer.parseInt(aidText.toString());
               // AppDataVM.getAllApplicationsFromVm(aid).observe(getLifecycleOwner(), myObserver);
                //Log.d(TAG, "aidText: request layout");
                mapView.getLayoutParams().width = mapView.getLayoutParams().width+1;
                mapView.getLayoutParams().height = mapView.getLayoutParams().height+1;
                points = new ArrayList<Point>();
                zeroPoints = new ArrayList<>();
                absPoints = new ArrayList<>();
                //AppDataVM.reloadAllApplicationsData(aid);
                invalidate();
                requestLayout();
            }
        });
        myObserver = applications -> {
            //isForTouch = true;
            points = new ArrayList<Point>();
            if (applications != null && !applications.isEmpty() && applications.size() > 1) {
                lastApplications = applications;
                processApplicationData(applications);
            }
        };
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
        //What is the point of any of this?  Is there any reason not to go <home/>?
        //Yes, you need to keep your job and get PAID
        //You have missed ALOT of work lately - you need to be here!!!
        //You are a little stressed out....

    }
    private void recieveApplicationData(List<ApplicationsData> applications) {

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
            double zoom = 1;
            double multi = .4;
            if (lat == 0 || lng == 0) continue;
            Point zpoint = XY(clat, clng, lat, lng, perPixel, 0, 0);
            Point tpoint = XY(clat, clng, lat, lng, perPixel, ww, hh);
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
            //Calculate Distance from last point...
            if (lastPoint.lat == 0 && lastPoint.lng == 0) {
                absPoint.distance = distance(clat, clng, lat, lng) * 39370.1d;
            } else {
                absPoint.distance = distance(lastPoint.lat, lastPoint.lng, lat, lng) * 39370.1d;
            }
            //If we have moved since the last point...
            if (lastPoint.lat != appData.lat && lastPoint.lng != appData.lng) {
                //If we have moved at least this far....
                if (absPoint.distance > 18) {
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
        desiredWidth = (int) absmaxX * 2;
        desiredHeight = (int) Math.abs(absmaxY * 2);
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
        //if (!resizedDims) {
        if (mapView.getLayoutParams().width != (int) absmaxX ||  mapView.getLayoutParams().height != (int) absmaxY)    {
            //
            // Log.d(TAG, "current layout parent dims: " + mapView.getLayoutParams().width + " " + mapView.getLayoutParams().height);
            //Log.d(TAG, "bottom: " + mapView.getTop() + " " + mapView.getBottom() + " " + mapView.getLeft() + " " + mapView.getRight());
            // Log.d(TAG, "requested layout dims: " + absmaxX + " " + absmaxY);
            mapView.getLayoutParams().width = (int) absmaxX; //Math.max(absmaxX, absmaxY);
            mapView.getLayoutParams().height = (int) absmaxY; //Math.max(absmaxY, absmaxX);
            requestLayout();
            resizedDims = true;
        } else {
            invalidate();
        }
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
    private void drawCanvas(Canvas canvas, List<Point> pts){
        float lastX = 0;
        float lastY = 0;
        pathMainLine.reset();
        pathSkinnyLineR.reset();
        pathSkinnyLineL.reset();
        pathThickLine.reset();
        if (pts.size() > 1){

            //Path path = new Path();
            final int SMOOTH_VAL = 6;
            for(int i = pts.size() - 2; i < pts.size(); i++){
                if(i >= 0){
                    Point point = pts.get(i);

                    if(i == 0){
                        Point next = pts.get(i + 1);
                        point.dx = ((next.x - point.x) / SMOOTH_VAL);
                        point.dy = ((next.y - point.y) / SMOOTH_VAL);
                    }
                    else if(i == pts.size() - 1){
                        Point prev = pts.get(i - 1);
                        point.dx = ((point.x - prev.x) / SMOOTH_VAL);
                        point.dy = ((point.y - prev.y) / SMOOTH_VAL);
                    }
                    else{
                        Point next = pts.get(i + 1);
                        Point prev = pts.get(i - 1);
                        point.dx = ((next.x - prev.x) / SMOOTH_VAL);
                        point.dy = ((next.y - prev.y) / SMOOTH_VAL);
                    }
                }
            }

            boolean first = true;
            for(int i = 0; i < pts.size(); i++){
                Point point = pts.get(i);
                if(first){
                    first = false;
                    pathMainLine.moveTo(point.x, point.y);
                    pathThickLine.moveTo(point.x, point.y);
                }
                else{
                    Point prev = pts.get(i - 1);
                    pathMainLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                    pathThickLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                }
                lastX = point.x;
                lastY = point.y;
            }

        } else {
            if (pts.size() == 1) {
                Point point = pts.get(0);
                canvas.drawCircle(point.x, point.y, 2, paintMainLine);
                lastX = point.x;
                lastY = point.y;
            }
        }
        //pathThickLine.setLastPoint(lastX, lastY);
        // pathMainLine.setLastPoint(lastX, lastY);

        //pathThickLine.close();
        // pathMainLine.close();
        if (resizedDims) {
            //Move to bottom left
            //canvas.translate(-(Math.abs(maxX)-Math.abs(minX)), Math.abs(minY)+Math.abs(maxY));
            //Move to top left
            //canvas.translate(-(Math.abs(maxX)-Math.abs(minX)), 0);
        }
        canvas.drawPath(pathMainLine, paintMainLine);
        canvas.drawPath(pathThickLine, paintThickLine);


    }
    public void drawGrid(Canvas canvas) {
        Log.v(TAG, "numColumns: "+numColumns+" numRows: "+numRows);
        Log.v(TAG, "cpoint: "+cpoint.x+" "+cpoint.y);
        if (numColumns != 0 && numRows != 0) {
            int width = getWidth();
            int height = getHeight();
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
        canvas.drawLine(0, point.y, width, point.y, bLinePaint);
    }
    public void drawALine(Canvas canvas, Point point) {
        int width = getWidth();
        canvas.drawLine(0, point.y, width, point.y, aLinePaint);
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
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw");
        plotCanvas = canvas;
        //matrix.set(savedMatrix);
        //matrix.postTranslate(1,1 );
        //matrix.setRotate(currentRotation, mid.x, mid.y);
       // mapView.setAnimationMatrix(matrix);
        float[] f = new float[9];
        matrix.getValues(f);
        pathSkinnyLineL.reset();
        pathSkinnyLineR.reset();
        pathMainLine.reset();
        pathThickLine.reset();
        float scaleX = f[Matrix.MSCALE_X];
        float scaleY = f[Matrix.MSCALE_Y];
        double totalDistance = 0;
        Log.d(TAG, "mapView matrix scale: "+scaleX+" "+scaleY);
        // Log.d(TAG, "abs min max: "+absminX+" "+absminY+" "+absmaxX+" "+absmaxY);
        if (absPoints.size() < 1) return;
        if (1 == 2) {
            paintThickLine.setStyle(Paint.Style.STROKE);
            paintMainLine.setStyle(Paint.Style.STROKE);
            drawCanvas(canvas, absPoints);
        } else {

            //Calculate parallel offsets
            for(int i = 0; i < absPoints.size(); i++){
                if(i >= 0){
                    Point point = absPoints.get(i);

                    if(i == 0){
                        Point next = absPoints.get(i + 1);
                        double degrees = Math.toDegrees(Math.atan2((double) (next.y-point.y), (double) (next.x-point.x)));
                        int offsetAngle = (int)degrees+90;
                        double[] offsetPoints = rotate_point_about_point(point.x, point.y, 150, offsetAngle);
                        Point lpoint = new Point();
                        lpoint.x = (float)offsetPoints[0];
                        lpoint.y =  (float)offsetPoints[1];

                        Point rpoint = new Point();
                        rpoint.x = (float)offsetPoints[2];
                        rpoint.y =  (float)offsetPoints[3];

                        parallelPoints.get(0).add(lpoint);
                        parallelPoints.get(1).add(rpoint);

                    }
                    else if(i == absPoints.size() - 1){
                        Point prev = absPoints.get(i - 1);

                        double degrees = Math.toDegrees(Math.atan2((double) (point.y-prev.y), (double) (point.x-prev.x)));
                        int offsetAngle = (int)degrees+90;
                        if (i == 1) {
                            double[] offsetPoints = rotate_point_about_point(point.x, point.y, 150, offsetAngle);
                            Point lpoint = new Point();
                            lpoint.x = (float)offsetPoints[0];
                            lpoint.y =  (float)offsetPoints[1];

                            Point rpoint = new Point();
                            rpoint.x = (float)offsetPoints[2];
                            rpoint.y =  (float)offsetPoints[3];

                            parallelPoints.get(0).add(lpoint);
                            parallelPoints.get(1).add(rpoint);
                        }
                        //Log.d(TAG, "degrees: "+degrees);
                        double[] offsetPoints = rotate_point_about_point(point.x, point.y, 150, offsetAngle);
                        Point lpoint = new Point();
                        lpoint.x = (float)offsetPoints[0];
                        lpoint.y =  (float)offsetPoints[1];

                        Point rpoint = new Point();
                        rpoint.x = (float)offsetPoints[2];
                        rpoint.y =  (float)offsetPoints[3];

                        parallelPoints.get(0).add(lpoint);
                        parallelPoints.get(1).add(rpoint);
                    }
                    else{
                        Point next = absPoints.get(i + 1);
                        Point prev = absPoints.get(i - 1);

                        double degrees = Math.toDegrees(Math.atan2((double) (point.y-prev.y), (double) (point.x-prev.x)));
                        int offsetAngle = (int)degrees+90;
                        //Log.d(TAG, "degrees: "+degrees);
                        double[] offsetPoints = rotate_point_about_point(point.x, point.y, 150, offsetAngle);
                        Point lpoint = new Point();
                        lpoint.x = (float)offsetPoints[0];
                        lpoint.y =  (float)offsetPoints[1];

                        Point rpoint = new Point();
                        rpoint.x = (float)offsetPoints[2];
                        rpoint.y =  (float)offsetPoints[3];

                        parallelPoints.get(0).add(lpoint);
                        parallelPoints.get(1).add(rpoint);

                    }
                }
            }
            //Normalize parallel offsets
            final int SMOOTH_VAL = 6;
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
                }
            }
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
                        pathSkinnyLineL.moveTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                        pathSkinnyLineR.lineTo(lpoint.x, lpoint.y);
                        pathSkinnyLineR.moveTo(rpoint.x, rpoint.y);
                    } else if(i == absPoints.size() - 1) {
                        Point prev = absPoints.get(i - 1);
                        //pathThickLine.moveTo(prev.x, prev.y);
                        //pathMainLine.moveTo(prev.x, prev.y);
                        //pathMainLine.lineTo(point.x, point.y);
                         pathMainLine.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                        Point lprev = parallelPoints.get(0).get(i-1);
                        Point rprev = parallelPoints.get(1).get(i-1);
                        Point lpoint = parallelPoints.get(0).get(i);
                        Point rpoint = parallelPoints.get(1).get(i);
                        //pathSkinnyLineL.moveTo(lprev.x, lprev.y);
                        //pathSkinnyLineL.lineTo(lpoint.x, lpoint.y);
                        pathSkinnyLineL.quadTo(lprev.x, lprev.y, lpoint.x, lpoint.y);
                        //pathSkinnyLineR.moveTo(rprev.x, rprev.y);
                        //pathSkinnyLineR.lineTo(rpoint.x, rpoint.y);
                        pathSkinnyLineR.quadTo(rprev.x, rprev.y, rpoint.x, rpoint.y);

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
                        Point lprev = parallelPoints.get(0).get(i-1);
                        Point rprev = parallelPoints.get(1).get(i-1);
                        Point lpoint = parallelPoints.get(0).get(i);
                        Point rpoint = parallelPoints.get(1).get(i);
                        pathSkinnyLineL.cubicTo(lprev.x + lprev.dx, lprev.y + lprev.dy, lpoint.x - lpoint.dx, lpoint.y - lpoint.dy, lpoint.x, lpoint.y);
                        pathSkinnyLineR.cubicTo(rprev.x + rprev.dx, rprev.y + rprev.dy, rpoint.x - rpoint.dx, rpoint.y - rpoint.dy, rpoint.x, rpoint.y);

                    }

                }
            }
            if (1 == 2) {
                //Backwards loop to try and close the lines?
                int s = absPoints.size() - 1;
                for (int i = s; i >= 0; i--) {
                    if (i >= 0) {
                        Point point = absPoints.get(i);
                        totalDistance += point.distance;
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
            float cols = 0;
            float rows = 0;

            int density = 1;
            String gridSizeText = "10 ft";
            if (canvas.getDensity() > density) density = canvas.getDensity();
            if (scaleX > 4) {
                //1 foot
                cols = absmaxX / (120 * density); //(1 x 12 x 10)
                rows = absmaxY / (120 * density);
                paintMainLine.setStrokeWidth(6);
                paintThickLine.setStrokeWidth(120);
                gridSizeText = "10 ft";
            } else if (scaleX <= 4 && scaleX > 2) {
                //10 feet
                cols = absmaxX / (120 * density); //(1 x 12 x 10)
                rows = absmaxY / (120 * density);
                paintMainLine.setStrokeWidth(6);
                paintThickLine.setStrokeWidth(120);
                gridSizeText = "1o ft";
            } else if (scaleX <= 2 && scaleX > .8) {
                //25 feet
                cols = absmaxX / (300 * density); //(1 x 12 x 10)
                rows = absmaxY / (300 * density);
                paintMainLine.setStrokeWidth(6);
                paintThickLine.setStrokeWidth(120);
                gridSizeText = "25 ft";
            } else if (scaleX <= .8 && scaleX > .4) {
                //50 feet
                cols = absmaxX / (600 * density); //(1 x 12 x 10)
                rows = absmaxY / (600 * density);
                paintMainLine.setStrokeWidth(6);
                paintThickLine.setStrokeWidth(120);
                gridSizeText = "50 ft";
            } else if (scaleX <= .4 && scaleX > .2) {
                //100 feet
                cols = absmaxX / (1200 * density); //(1 x 12 x 10)
                rows = absmaxY / (1200 * density);
                paintMainLine.setStrokeWidth(6);
                paintThickLine.setStrokeWidth(120);
                gridSizeText = "100 ft";
             } else if (scaleX <= .2 && scaleX > .08) {
                //250 feet
                cols = absmaxX / (3000 * density); //(1 x 12 x 10)
                rows = absmaxY / (3000 * density);
                paintMainLine.setStrokeWidth(6);
                paintThickLine.setStrokeWidth(120);
                gridSizeText = "250 ft";
            }  else if (scaleX <= .08 && scaleX > .04) {
                //500 feet
                cols = absmaxX / (6000 * density); //(1 x 12 x 10)
                rows = absmaxY / (6000 * density);
                paintMainLine.setStrokeWidth(12);
                paintThickLine.setStrokeWidth(240);
                gridSizeText = "500 ft";
            }   else if (scaleX <= .04 && scaleX > .01) {
                //1320 feet
                cols = absmaxX / (15840 * density); //(1 x 12 x 10)
                rows = absmaxY / (15840 * density);
                paintMainLine.setStrokeWidth(18);
                paintThickLine.setStrokeWidth(360);
                gridSizeText = "1/4 mile Grid";
            } else {
                //2640 feet
                cols = absmaxX / (31680 * density); //(1 x 12 x 10)
                rows = absmaxY / (31680 * density);
                paintMainLine.setStrokeWidth(36);
                paintThickLine.setStrokeWidth(720);
                gridSizeText = "1/2 mile Grid";
            }
            gridSizeLabel.setText(gridSizeText);
            setNumColumns(Math.round(cols));
            setNumRows(Math.round(rows));

            Point firstPoint = absPoints.get(0);
            Point lastPoint = absPoints.get(absPoints.size()-1);
            //if (currentBearing != initialBearing) canvas.rotate((initialBearing-currentBearing)+180, canvas.getWidth()/2, canvas.getHeight()/2);
            float currentHeadingInverse = (initialBearing-currentBearing);
            float currentHeadingOffset = 360f-currentHeadingInverse;
            //canvas.rotate(currentHeadingInverse+currentHeadingOffset, canvas.getWidth()/2, canvas.getHeight()/2);


            Path pathShape = new Path();
            paintThickLine.setStyle(Paint.Style.STROKE);
            pathShape.moveTo(0, 0);
            pathShape.lineTo(10, 10);
            pathShape.lineTo(10, 0);
            pathShape.close();

            //PathDashPathEffect pathEffect = new PathDashPathEffect(pathShape, 30, 30, PathDashPathEffect.Style.ROTATE);




            CornerPathEffect cornerpatheffect = new CornerPathEffect(10);
            
            PathDashPathEffect pathdashpath  = new PathDashPathEffect(makePathDash(), 20, 0, PathDashPathEffect.Style.MORPH);
            ComposePathEffect pathEffect = new ComposePathEffect(pathdashpath, cornerpatheffect);
            //paintMainLine.setPathEffect(pathdashpath);


            //Inverts Line colors
            //pathThickLine.setFillType(Path.FillType.INVERSE_WINDING);
            //Makes it always face same dir?
            //canvas.rotate(currentHeadingInverse+currentHeadingOffset, canvas.getWidth()/2, canvas.getHeight()/2);

            //pathMainLine.op(pathMainLine, Path.Op.INTERSECT);
            canvas.drawPath(pathSkinnyLineR, bLinePaint);
            canvas.drawPath(pathSkinnyLineL, lLinePaint);
            //pathMainLine.op(pathSkinnyLineR, Path.Op.INTERSECT);
            //pathSkinnyLineR.close();
            canvas.drawPath(pathSkinnyLineR, cLinePaint);

            canvas.drawPath(pathSkinnyLineL, cLinePaint);
            canvas.drawPath(pathMainLine, paintMainLine);
            canvas.drawText("Total Distance: "+Math.round(totalDistance),  absPoints.get(absPoints.size()-1).x+50, absPoints.get(absPoints.size()-1).y+50, labelPaint);
            drawGrid(canvas);
            drawALine(canvas, firstPoint);
            if (!isForTouch) {
                matrix.set(savedMatrix);
               // matrix.postScale(.03f, .03f,((canvas.getWidth() / 2) - absPoints.get(absPoints.size()-1).x), ((canvas.getHeight() / 2) - absPoints.get(absPoints.size()-1).y));
                //mapView.setAnimationMatrix(matrix);
                //matrix.set(savedMatrix);
                //savedMatrix.set(matrix);
                //matrix.set(savedMatrix);

                matrix.postTranslate(((canvas.getWidth() / 2) - absPoints.get(absPoints.size()-1).x), ((canvas.getHeight() / 2) - absPoints.get(absPoints.size()-1).y));
                //matrix.postTranslate(((canvas.getWidth() / 2) - lastX), ((canvas.getHeight() / 2) - lastY));
                //matrix.setRotate(currentRotation, mid.x, mid.y);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mapView.setAnimationMatrix(matrix);
                } else {

                }
                //savedMatrix.set(matrix);
                //isForTouch = false;
            } else {
                //isForTouch = false;
            }
        }
    }
    private void fitCanvasToScreen(Canvas canvas) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        //int screenWidth = mapView.getWidth();
        //int screenHeight = mapView.getHeight();
        RectF viewRect = new RectF(0, 0, screenWidth, screenHeight);
        RectF drawableRect  = new RectF(0, 0, absmaxX,
                +absmaxY);
        savedMatrix.set(matrix);
        matrix.reset();
        //Matrix matrix = new Matrix();
        Log.d(TAG, "fitCanvasToScreen screem: "+screenWidth+"x"+screenHeight+"  canvas: "+canvas.getWidth()+"x"+canvas.getHeight());
        boolean canFitRect = matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
        Log.d(TAG, "fitCanvasToScreen canFitRect: "+canFitRect);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mapView.setAnimationMatrix(matrix);
        }
        //invalidate();
    }
    private Path makePathDash() {
       /* Path p = new Path();
        p.moveTo(-6, 9/2);
        p.lineTo(6,9/2);
        p.lineTo(6,9/2-3);
        p.lineTo(-6, 9/2-3);
        p.close();
        p.moveTo(-6, -(9/2));
        p.lineTo(6,-(9/2));
        p.lineTo(6, -(9/2-3));
        p.lineTo(-6, -(9/2-3));

        return p;*/
        Path pathShape = new Path();
        pathShape.moveTo(0, 40);
        pathShape.lineTo(0, 50);
        pathShape.lineTo(10, 50);
        pathShape.lineTo(10, 40);
        pathShape.lineTo(0, 40);
        pathShape.moveTo(0, -40);
        pathShape.lineTo(0, -50);
        pathShape.lineTo(20, -50);
        pathShape.lineTo(20, -40);
        pathShape.lineTo(0, -40);
        return pathShape;
    }
    @Override
    public void draw(Canvas canvas) {
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
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        RelativeLayout view = (RelativeLayout) v;
        // Dump touch event to log
        //dumpEvent(event);
       // Log.v("touch", "mapView scroll: "+mapView.getTop()+" "+mapView.getRight()+" "+mapView.getBottom()+" "+mapView.getLeft());
        float currentRotation = v.getRotation();
        //v.setRotation(0);
        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                resetButton.setText("true");
                isForTouch = true;
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    resetButton.setText("true");
                    isForTouch = true;
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;

                // v.setRotation(currentRotation);
                break;
            case MotionEvent.ACTION_MOVE:
                resetButton.setText("true");
                isForTouch = true;
                if (mode == DRAG) {
                    // ...
                    matrix.set(savedMatrix);
                    Log.v("Touch Class", "event xy: "+event.getX()+" "+event.getY());
                    matrix.postTranslate(event.getX() - start.x,event.getY() - start.y );
                    //v.setMa
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        //v.setScaleX(scale);
                        //v.setScaleY(scale);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                        //Log.v("Touch Class", "scale: "+scale);
                    }
                }
                break;
        }

        //matrix.setRotate(currentRotation, mid.x, mid.y);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.setAnimationMatrix(matrix);
        }
        invalidate();
        // Log.d("bencosprayer", "touch mode: "+mode;
        //MyAnimation animation = new MyAnimation(matrix);
        //animation.setDuration(0);
        //animation.setFillAfter(true);
        //v.setAnimation(animation);
        return true; // indicate event was handled
    }

    /** Show an event in the LogCat view, for debugging */
    private void dumpEvent(MotionEvent event) {
        String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
                "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(
                    action >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
        //Log.d("bencosprayer", "touch: "+sb);
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
}

class Point2 implements Serializable {
    float x, y;
    float dx, dy;
    float bearing;
    double distance;
    @Override
    public String toString() {

        return "x: "+x+" y: "+y+" dx: "+dx+" dy:"+dy+" bearing: "+bearing+" distance: "+distance;
    }
}

