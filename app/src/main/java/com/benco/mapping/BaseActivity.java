package com.benco.mapping;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.benco.mapping.domain.ApplicationsDataViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class BaseActivity extends AppCompatActivity {
    public SharedPreferences sharedPreferences;
    static HashMap<String, Object> settings = new HashMap<>();
    public ApplicationsDataViewModel AppDataVM;
    public int lid = 0;
    public int aid = 0;
    public int isSpraying = 0;
    public Button gpsIndicatorOrig;
    public ImageView gpsIndicator;
    public float azimuth = 0;
    public HashMap<Integer, String> colorMap = new HashMap<>();
    //public boolean captrueGPS = false;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private ActivityResultLauncher<String> mGetContent;
    private String TAG = "BaseActivity";
    public  TextView.OnEditorActionListener settingsEditTextListener;
    public TextView.OnFocusChangeListener settingsEditTextFocusListener;
    private static final int COLOR_PICKER_ROWS = 4;
    private static final int COLOR_PICKER_COLUMNS = 6;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        colorMap.put(0, "#006B6B");
        colorMap.put(1, "#3EDD03");
        colorMap.put(2, "#CC6600");
        colorMap.put(3, "#FF9600");
        colorMap.put(4, "#FFFF00");
        colorMap.put(5, "#CCCCCC");
        colorMap.put(6, "#007848");
        colorMap.put(7, "#00AB66");
        colorMap.put(8, "#990001");
        colorMap.put(9, "#FF0000");
        colorMap.put(10, "#FF0066");
        colorMap.put(11, "#999999");
        colorMap.put(12, "#005292");
        colorMap.put(13, "#00AEEF");
        colorMap.put(14, "#4B0082");
        colorMap.put(15, "#8000FF");
        colorMap.put(16, "#FF87B8");
        colorMap.put(17, "#606060");
        colorMap.put(18, "#55DBFF");
        colorMap.put(19, "#002255");
        colorMap.put(20, "#C6B199");
        colorMap.put(21, "#8A5D3B");
        colorMap.put(22, "#564A3E");
        colorMap.put(23, "#444444");
        // Retrieving the value using its keys the file name must be same in both saving and retrieving the data
        sharedPreferences = getSharedPreferences("ThemePreferences", Context.MODE_PRIVATE);
        // The value will be default as empty string because for the very first time when the app is opened, there is nothing to show
        String AppTheme = sharedPreferences.getString("theme", "dark");
        settings.put("theme", AppTheme);
        if (AppTheme.equals("light")) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppThemeDark);
        }
        String textScaleMultiplier = sharedPreferences.getString("textScaleMultiplier", "1.0");
        settings.put("textScaleMultiplier", textScaleMultiplier);
        String unitSystem = sharedPreferences.getString("unitSystem", "us");
        settings.put("unitSystem", unitSystem);
        boolean showGrid = sharedPreferences.getBoolean("showGrid", true);
        settings.put("showGrid", showGrid);
        boolean showSolidBackground = sharedPreferences.getBoolean("showSolidBackground", true);
        settings.put("showSolidBackground", showSolidBackground);
        boolean showABLines = sharedPreferences.getBoolean("showABLines", true);
        settings.put("showABLines", showABLines);
        boolean showSteeringLines = sharedPreferences.getBoolean("showSteeringLines", true);
        settings.put("showSteeringLines", showSteeringLines);
        boolean showFieldBoundaries = sharedPreferences.getBoolean("showFieldBoundaries", true);
        settings.put("showFieldBoundaries", showFieldBoundaries);
        boolean showBasemap = sharedPreferences.getBoolean("showBasemap", false);
        settings.put("showBasemap", showBasemap);
        String basemapOpacity = sharedPreferences.getString("basemapOpacity", "0.55");
        settings.put("basemapOpacity", basemapOpacity);

        int gridColor = sharedPreferences.getInt("gridColor", Color.parseColor("#C0780000"));
        settings.put("gridColor", gridColor);
        int backgroundColor = sharedPreferences.getInt("backgroundColor", Color.parseColor("#C71F1F1F"));
        settings.put("backgroundColor", backgroundColor);
        int abLineColor = sharedPreferences.getInt("abLineColor", Color.parseColor("#FF00FF00"));
        settings.put("abLineColor", abLineColor);
        int steeringLineColor = sharedPreferences.getInt("steeringLineColor", Color.parseColor("#FF0000FF"));
        settings.put("steeringLineColor", steeringLineColor);
        int fieldBoundaryColor = sharedPreferences.getInt("fieldBoundaryColor", Color.parseColor("#FFFFFF00"));
        settings.put("fieldBoundaryColor", fieldBoundaryColor);

        int mainPathColor = sharedPreferences.getInt("mainPathColor", R.color.green);
        //Log.d(TAG, "mainPathColor: "+mainPathColor);
        settings.put("mainPathColor", mainPathColor);
        int thickPathColor = sharedPreferences.getInt("thickPathColor", R.color.red);
        settings.put("thickPathColor", thickPathColor);
        String thickPathStroke = sharedPreferences.getString("thickPathStroke", "120");
        settings.put("thickPathStroke", thickPathStroke);
        String totalNumberSections = sharedPreferences.getString("totalNumberSections", "1");
        settings.put("totalNumberSections", totalNumberSections);
        Integer sectionTotal = Integer.parseInt(totalNumberSections);
        for(Integer i = 1; i <= sectionTotal; i++) {
            String width = sharedPreferences.getString("section"+i+"Width", "30");
            settings.put("section"+i+"Width", width);

            String colorString = colorMap.get(i-1).replace("#", "#FF");
            String alphaColorString = colorString.replace("#FF", "#55");
            int colorInt = Color.parseColor(colorString);
            int alphaColorInt = Color.parseColor(alphaColorString);

            int sectionPathColor = sharedPreferences.getInt("section"+i+"PathColor", colorInt);
            settings.put("section"+i+"PathColor", sectionPathColor);
            int sectionThickPathColor = sharedPreferences.getInt("section"+i+"ThickPathColor", alphaColorInt);
            settings.put("section"+i+"ThickPathColor", sectionThickPathColor);

        }

        settingsEditTextListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String inputText = v.getText().toString();
                    String prefKey = "";
                    if (v.getTag() != null) {
                        // The tag is not null, proceed with your logic
                        // You should cast the tag to its original type before using it
                        Object tagObject = v.getTag();

                        // Example for a String tag
                        if (tagObject instanceof String) {
                            prefKey = (String) tagObject;
                            //Log.d("TagCheck", "Tag value is: " + prefKey);
                            // You can add more logic here
                        } else {
                            // Handle cases where the tag is a different type
                           // Log.d("TagCheck", "Tag is not a String or is a different object type.");
                        }

                    } else {
                        // The tag is null
                       // Log.d("TagCheck", "Tag is null.");
                    }
                    Log.d("oneditoraction", prefKey);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(prefKey, inputText); // Store an integer
                    editor.apply(); // or editor.commit();
                    settings.put(prefKey, inputText);
                    return true; // Consume the event
                }
                return false;
            }
        };
        settingsEditTextFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    TextView t = (TextView) v;
                    String inputText = t.getText().toString();
                    String prefKey = "";
                    if (t.getTag() != null) {
                        // The tag is not null, proceed with your logic
                        // You should cast the tag to its original type before using it
                        Object tagObject = t.getTag();

                        // Example for a String tag
                        if (tagObject instanceof String) {
                            prefKey = (String) tagObject;
                            Log.d("TagCheck", "Tag value is: " + prefKey);
                            // You can add more logic here
                        } else {
                            // Handle cases where the tag is a different type
                            Log.d("TagCheck", "Tag is not a String or is a different object type.");
                        }

                    } else {
                        // The tag is null
                        Log.d("TagCheck", "Tag is null.");
                    }
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(prefKey, inputText); // Store an integer
                    editor.apply(); // or editor.commit();
                    settings.put(prefKey, inputText);
                }else{
                   // Toast.makeText(this, "Get Focus", Toast.LENGTH_SHORT).show();
                }
            }
        };
        // Initialize the export launcher
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        if (uri != null) {
                            writeDatabaseToUri(uri);
                        } else {
                            showToast("No file selected.");
                        }
                    } else {
                        // User canceled the file selection
                        showToast("Export canceled by user.");
                    }
                }
        );

        // Initialize the import launcher
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        if (uri != null) {
                            readDatabaseFromUri(uri);
                        } else {
                            showToast("No file selected.");
                        }
                    } else {
                        // User canceled the file selection
                        showToast("Import canceled by user.");
                    }
                }
        );

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        // Handle the returned Uri
                        if (uri != null) {
                            Log.d(TAG, "Selected URI: " + uri.toString());
                            if (uri != null) {
                                readDatabaseFromUri(uri);
                            } else {
                                showToast("No file selected.");
                            }
                            // You can now use this Uri to open an input stream, etc.
                            // Example: open an InputStream with getContentResolver().openInputStream(uri)
                        } else {
                            Log.d(TAG, "No file selected");
                        }
                    }
                });
        //mGetContent.launch("application/octet-stream");

        // Common setup can be done here, like initializing a toolbar

        //importDatabase();
        //exportDatabase();
    }

    public void showDynamicColorPickerDialog(String preferenceKey, View v, Boolean withAlpha) {

        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.color_picker_dynamic);
        TableLayout tableLayout = dialog.findViewById(R.id.dynamicColorPickerTableLayout);
        int ci = 0;
        int screenWidth = getActivityWidth();
        int colorSize = screenWidth/10;
        // Create rows and columns
        for (int i = 0; i < COLOR_PICKER_ROWS; i++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));
            for (int j = 0; j < COLOR_PICKER_COLUMNS; j++) {
                String colorString = colorMap.get(ci).replace("#", "#FF");
                String alphaColorString = colorString.replace("#FF", "#55");
                int colorInt = Color.parseColor(colorString);
                int alphaColorInt = Color.parseColor(alphaColorString);
                Button button = new Button(this);
                //button.setText(colorString);
                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        colorSize,
                        colorSize);
                params.setMargins(7, 7,7,7);
                button.setLayoutParams(params);
                button.setGravity(Gravity.CENTER);
                int color = (withAlpha)?alphaColorInt:colorInt;
                button.setBackgroundColor(color);
                // Set OnClickListener for each button
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View b) {
                        int color = (withAlpha)?alphaColorInt:colorInt;
                        Log.d(TAG, "color: "+color);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(preferenceKey, color); // Store an integer
                        editor.apply(); // or editor.commit();
                        settings.put(preferenceKey, color);
                        v.setBackgroundColor(color);
                        dialog.dismiss();
                    }
                });

                // Add button to the row
                tableRow.addView(button);
                ci++;
            }
            // Add row to the table
            tableLayout.addView(tableRow);
        }
        if (withAlpha) {
        } else {
        }

        dialog.show();
    }
    public int getActivityWidth() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels; // Returns the width in pixels
    }
    public void showDynamicColorPickerDialogOld(String preferenceKey, View v, Boolean withAlpha) {
        HashMap<Integer, String> colorMap = new HashMap<>();
        colorMap.put(0, "#330000");
        colorMap.put(1, "#331900");
        colorMap.put(2, "#333300");
        colorMap.put(3, "#193300");
        colorMap.put(4, "#003300");
        colorMap.put(5, "#003319");
        colorMap.put(6, "#003333");
        colorMap.put(7, "#001933");
        colorMap.put(8, "#000033");
        colorMap.put(9, "#190033");
        colorMap.put(10, "#330033");
        colorMap.put(11, "#330019");
        colorMap.put(12, "#000000");
        colorMap.put(13, "#660000");
        colorMap.put(14, "#663300");
        colorMap.put(15, "#666600");
        colorMap.put(16, "#336600");
        colorMap.put(17, "#006600");
        colorMap.put(18, "#006633");
        colorMap.put(19, "#006666");
        colorMap.put(20, "#003366");
        colorMap.put(21, "#000066");
        colorMap.put(22, "#330066");
        colorMap.put(23, "#660066");
        colorMap.put(24, "#660033");
        colorMap.put(25, "#202020");
        colorMap.put(26, "#990000");
        colorMap.put(27, "#994C00");
        colorMap.put(28, "#999900");
        colorMap.put(29, "#4C9900");
        colorMap.put(30, "#009900");
        colorMap.put(31, "#00994C");
        colorMap.put(32, "#009999");
        colorMap.put(33, "#004C99");
        colorMap.put(34, "#000099");
        colorMap.put(35, "#4C0099");
        colorMap.put(36, "#990099");
        colorMap.put(37, "#99004C");
        colorMap.put(38, "#404040");
        colorMap.put(39, "#CC0000");
        colorMap.put(40, "#CC6600");
        colorMap.put(41, "#CCCC00");
        colorMap.put(42, "#66CC00");
        colorMap.put(43, "#00CC00");
        colorMap.put(44, "#00CC66");
        colorMap.put(45, "#00CCCC");
        colorMap.put(46, "#0066CC");
        colorMap.put(47, "#0000CC");
        colorMap.put(48, "#6600CC");
        colorMap.put(49, "#CC00CC");
        colorMap.put(50, "#CC0066");
        colorMap.put(51, "#606060");
        colorMap.put(52, "#FF0000");
        colorMap.put(53, "#FF8000");
        colorMap.put(54, "#FFFF00");
        colorMap.put(55, "#80FF00");
        colorMap.put(56, "#00FF00");
        colorMap.put(57, "#00FF80");
        colorMap.put(58, "#00FFFF");
        colorMap.put(59, "#0080FF");
        colorMap.put(60, "#0000FF");
        colorMap.put(61, "#7F00FF");
        colorMap.put(62, "#FF00FF");
        colorMap.put(63, "#FF007F");
        colorMap.put(64, "#808080");
        colorMap.put(65, "#FF3333");
        colorMap.put(66, "#FF9933");
        colorMap.put(67, "#FFFF33");
        colorMap.put(68, "#99FF33");
        colorMap.put(69, "#33FF33");
        colorMap.put(70, "#33FF99");
        colorMap.put(71, "#33FFFF");
        colorMap.put(72, "#3399FF");
        colorMap.put(73, "#3333FF");
        colorMap.put(74, "#9933FF");
        colorMap.put(75, "#FF33FF");
        colorMap.put(76, "#FF3399");
        colorMap.put(77, "#A0A0A0");
        colorMap.put(78, "#FF6666");
        colorMap.put(79, "#FFB266");
        colorMap.put(80, "#FFFF66");
        colorMap.put(81, "#B2FF66");
        colorMap.put(82, "#66FF66");
        colorMap.put(83, "#66FFB2");
        colorMap.put(84, "#66FFFF");
        colorMap.put(85, "#66B2FF");
        colorMap.put(86, "#6666FF");
        colorMap.put(87, "#B266FF");
        colorMap.put(88, "#FF66FF");
        colorMap.put(89, "#FF66B2");
        colorMap.put(90, "#C0C0C0");
        colorMap.put(91, "#FF9999");
        colorMap.put(92, "#FFCC99");
        colorMap.put(93, "#FFFF99");
        colorMap.put(94, "#CCFF99");
        colorMap.put(95, "#99FF99");
        colorMap.put(96, "#99FFCC");
        colorMap.put(97, "#99FFFF");
        colorMap.put(98, "#99CCFF");
        colorMap.put(99, "#9999FF");
        colorMap.put(100, "#CC99FF");
        colorMap.put(101, "#FF99FF");
        colorMap.put(102, "#FF99CC");
        colorMap.put(103, "#E0E0E0");
        colorMap.put(104, "#FFCCCC");
        colorMap.put(105, "#FFE5CC");
        colorMap.put(106, "#FFFFCC");
        colorMap.put(107, "#E5FFCC");
        colorMap.put(108, "#CCFFCC");
        colorMap.put(109, "#CCFFE5");
        colorMap.put(110, "#CCFFFF");
        colorMap.put(111, "#CCE5FF");
        colorMap.put(112, "#CCCCFF");
        colorMap.put(113, "#E5CCFF");
        colorMap.put(114, "#FFCCFF");
        colorMap.put(115, "#FFCCE5");
        colorMap.put(116, "#FFFFFF");
        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.color_picker_dynamic);
        TableLayout tableLayout = dialog.findViewById(R.id.dynamicColorPickerTableLayout);
        int ci = 0;
        // Create rows and columns
        for (int i = 0; i < COLOR_PICKER_ROWS; i++) {
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));

            for (int j = 0; j < COLOR_PICKER_COLUMNS; j++) {
                String colorString = colorMap.get(ci).replace("#", "#FF");
                String alphaColorString = colorString.replace("#FF", "#55");
                int colorInt = Color.parseColor(colorString);
                int alphaColorInt = Color.parseColor(alphaColorString);
                Button button = new Button(this);
                button.setText(colorString);
                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        108,
                        108);
                params.setMargins(7, 7,7,7);
                button.setLayoutParams(params);
                button.setGravity(Gravity.CENTER);
                int color = (withAlpha)?alphaColorInt:colorInt;
                button.setBackgroundColor(color);
                // Set OnClickListener for each button
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View b) {
                        int color = (withAlpha)?alphaColorInt:colorInt;
                        Log.d(TAG, "color: "+color);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(preferenceKey, color); // Store an integer
                        editor.apply(); // or editor.commit();
                        settings.put(preferenceKey, color);
                        v.setBackgroundColor(color);
                        dialog.dismiss();
                    }
                });

                // Add button to the row
                tableRow.addView(button);
                ci++;
            }

            // Add row to the table
            tableLayout.addView(tableRow);
        }
        if (withAlpha) {
        } else {
        }

        dialog.show();
    }
    public void showColorPickerDialog(String preferenceKey, View v, Boolean withAlpha) {
        final Dialog dialog = new Dialog(this);
        if (withAlpha) {
            dialog.setContentView(R.layout.color_alpha_picker_dialog);
        } else {
            dialog.setContentView(R.layout.color_picker_dialog);
        }

        Button redButton = dialog.findViewById(R.id.color_red);
        Button greenButton = dialog.findViewById(R.id.color_green);
        Button blueButton = dialog.findViewById(R.id.color_blue);
        Button yellowButton = dialog.findViewById(R.id.color_yellow);

        Button orangeButton = dialog.findViewById(R.id.color_orange);
        Button limeButton = dialog.findViewById(R.id.color_lime);
        Button aquaButton = dialog.findViewById(R.id.color_aqua);
        Button purpleButton = dialog.findViewById(R.id.color_purple);


        Button whiteButton = dialog.findViewById(R.id.color_white);
        Button lightGrayButton = dialog.findViewById(R.id.color_lightGray);
        Button grayButton = dialog.findViewById(R.id.color_gray);
        Button blackButton = dialog.findViewById(R.id.color_black);

        redButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.redA:R.color.red;

            Log.d(TAG, "color: "+color);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });

        greenButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.greenA:R.color.green;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });

        blueButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.blueA:R.color.blue;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        yellowButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.yellowA:R.color.yellow;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        orangeButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.orangeA:R.color.orange;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        limeButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.limeA:R.color.lime;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        aquaButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.aquaA:R.color.aqua;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        purpleButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.purpleA:R.color.purple;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });

        whiteButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.whiteA:R.color.white;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });

        lightGrayButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.lightGrayA:R.color.lightGray;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        grayButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.grayA:R.color.gray;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });


        blackButton.setOnClickListener(view -> {
            // Handle red color selection
            int color = (withAlpha)?R.color.blackA:R.color.black;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(preferenceKey, color); // Store an integer
            editor.apply(); // or editor.commit();
            settings.put(preferenceKey, color);
            v.setBackgroundColor(ContextCompat.getColor(this, color));
            dialog.dismiss();
        });
        dialog.show();
    }
    public void exportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "locations.db");
        exportLauncher.launch(intent);
    }

    public void importDatabase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        importLauncher.launch(intent);
    }

    private void writeDatabaseToUri(Uri uri) {
        // Implementation for writing the database to the specified URI
        try {
            String currentDBPath = getDatabasePath("locations").getAbsolutePath();
            File dbFile = new File(currentDBPath);
            FileInputStream fis = new FileInputStream(dbFile);
            OutputStream fos = getContentResolver().openOutputStream(uri);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();
            System.out.println("Database exported successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readDatabaseFromUri(Uri uri) {
        // Implementation for reading the database from the specified URI
        try {
            String currentDBPath = getDatabasePath("locations").getAbsolutePath();

            File dbFile = new File(currentDBPath);
            if (dbFile.exists()) {
                dbFile.delete(); // Delete the existing database
            }
            dbFile = new File(currentDBPath);
            FileInputStream fis = (FileInputStream) getContentResolver().openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.flush();
            fos.close();
            fis.close();
            System.out.println("Database imported successfully.");

            System.out.println("Database file:"+currentDBPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    // You can add more common methods here, such as logging, navigation, etc.
}