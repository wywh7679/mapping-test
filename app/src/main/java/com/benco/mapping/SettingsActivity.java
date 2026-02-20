package com.benco.mapping;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.function.BiConsumer;
public class SettingsActivity extends BaseActivity {
    private ImageView homeBtn;
    private TableLayout settingsTableLayoutSprayer;
    private TableLayout settingsTableNumberSections;
    private TableRow settingsTableSprayerPreview;
    private EditText totalNumberSections;
    private int currentStep = 0;
    private int totalSections;
    private int currentRow = 0;
    private Button backButton, nextButton, doneButton;
    private TableRow[] tableRows;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        settingsTableLayoutSprayer = findViewById(R.id.settings_table_sprayer_sections);
        settingsTableNumberSections = findViewById(R.id.settings_table_sprayer);
        settingsTableSprayerPreview = findViewById(R.id.settings_table_sprayer_preview);
        homeBtn = findViewById(R.id.homeBtn);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                finish();
                startActivity(intent);
            }
        });
        Button colorPickerButton = findViewById(R.id.color_picker_button);
        colorPickerButton.setBackgroundColor((int) settings.get("mainPathColor"));
        colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDynamicColorPickerDialog("mainPathColor", v, false);
            }
        });
        Button colorPickerButton2 = findViewById(R.id.color_picker_button2);
        colorPickerButton2.setBackgroundColor((int) settings.get("thickPathColor"));
        colorPickerButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDynamicColorPickerDialog("thickPathColor", v, true);
            }
        });
        EditText thickPathStroke = findViewById(R.id.thickPathStroke);

        thickPathStroke.setText(settings.get("thickPathStroke").toString());
        // Handle Enter key press
        thickPathStroke.setOnEditorActionListener(settingsEditTextListener);
        // Handle focus change
        thickPathStroke.setOnFocusChangeListener(settingsEditTextFocusListener);

        totalNumberSections = findViewById(R.id.totalNumberSections);
        totalNumberSections.setText(settings.get("totalNumberSections").toString());
        totalNumberSections.setTag("totalNumberSections");
        // Handle Enter key press
        totalNumberSections.setOnEditorActionListener(settingsEditTextListener);
        // Handle focus change
        totalNumberSections.setOnFocusChangeListener(settingsEditTextFocusListener);

        backButton = findViewById(R.id.back_button);
        nextButton = findViewById(R.id.next_button);
        doneButton = findViewById(R.id.done_button);
        backButton.setOnClickListener(v -> showPreviousStep());
        nextButton.setOnClickListener(v -> showNextStep());
        doneButton.setOnClickListener(v -> finishWizard());
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Closes the current activity and returns to the previous one
            }
        });
        startForm();
    }
    private void startForm() {
        Integer sectionTotal = Integer.parseInt(totalNumberSections.getText().toString());
        totalSections = sectionTotal;
        currentRow = 0;
        tableRows = new TableRow[sectionTotal*4];
        settingsTableLayoutSprayer.removeAllViews();
        settingsTableSprayerPreview.removeAllViews();
        int totalWeight = 0;
        for(Integer i = 1; i <= sectionTotal; i++) {
            Object weightObject = settings.get("section"+i+"Width");
            int weight = 30;
            String weightString = "0";
            if (weightObject != null) {
                weight = Integer.parseInt(weightObject.toString());
                weightString = weightObject.toString();
            }
            Object color = settings.get("section"+i+"PathColor");
            int pathColor = 0;
            if (color != null) {
                pathColor = (int) color;
            }
            Object thickColor = settings.get("section"+i+"ThickPathColor");
            int thickPathColor = 0;
            if (thickColor != null) {
                thickPathColor = (int) thickColor;
            }
            totalWeight += weight;
            addRows(""+i, weightString, pathColor , thickPathColor);
            Context context = new ContextThemeWrapper(this, R.style.SectionBox);
            TextView textView = new TextView(context);
            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    weight
            );
            textView.setLayoutParams(params);
            textView.setBackgroundColor((int) pathColor);
            textView.setText(i+"");

            settingsTableSprayerPreview.addView(textView);
        }
        settingsTableSprayerPreview.setWeightSum(totalWeight);
    }
    private void addRows(String sectionId, String width, int color1, int color2) {
        int viewState = (Integer.parseInt(sectionId) == currentStep)? View.VISIBLE:View.GONE;
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                6
        );
        params.setMargins(0, 0, 0, 15); // Set bottom margin
        TableRow.LayoutParams params2 = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                4
        );
        params2.setMargins(0, 0, 0, 30); // Set bottom margin

        TableRow.LayoutParams params3 = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                2
        );
        params3.setMargins(0, 0, 0, 30); // Set bottom margin

        TableRow row0 = new TableRow(this);
        row0.setWeightSum(6);
        row0.setLayoutParams(params);
        TextView textView0 = new TextView(this);
        textView0.setLayoutParams(params2);
        textView0.setText("Section "+sectionId);
        textView0.setTextSize(24);
        row0.addView(textView0);
        TooltipCompat.setTooltipText(row0, sectionId);
        row0.setTag(sectionId);
        row0.setVisibility(viewState);
        tableRows[currentRow] = row0;
        currentRow++;
        settingsTableLayoutSprayer.addView(row0);

        // First TableRow
        TableRow row1 = new TableRow(this);
        row1.setWeightSum(6);
        row1.setLayoutParams(params);
        TextView textView1 = new TextView(this);
        textView1.setLayoutParams(params2);
        textView1.setText("Section "+sectionId+" Width");
        EditText editText1 = new EditText(this);
        editText1.setLayoutParams(params3);
        editText1.setId(View.generateViewId());
        editText1.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editText1.setSingleLine(true);
        editText1.setHint("section"+sectionId+"Width");
        TooltipCompat.setTooltipText(editText1, "section"+sectionId+"Width");
        editText1.setTag("section"+sectionId+"Width");
        editText1.setText(width);
        row1.addView(textView1);
        row1.addView(editText1);

        editText1.setOnEditorActionListener(settingsEditTextListener);
        // Handle focus change
        editText1.setOnFocusChangeListener(settingsEditTextFocusListener);

        TooltipCompat.setTooltipText(row1, sectionId);
        row1.setTag(sectionId);
        row1.setVisibility(viewState);
        tableRows[currentRow] = row1;
        currentRow++;
        settingsTableLayoutSprayer.addView(row1);

        // Second TableRow
        TableRow row2 = new TableRow(this);
        row2.setLayoutParams(params);
        row2.setWeightSum(6);

        TextView textView2 = new TextView(this);
        textView2.setLayoutParams(params2);
        textView2.setText("Color");

        Button button1 = new Button(this);
        button1.setLayoutParams(params3);
        button1.setId(View.generateViewId());
        button1.setBackgroundColor(color1);
        button1.setText("Choose Color");

        row2.addView(textView2);
        row2.addView(button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDynamicColorPickerDialog("section"+sectionId+"PathColor", v, false);
            }
        });

        TooltipCompat.setTooltipText(row2, sectionId);
        row2.setTag(sectionId);
        row2.setVisibility(viewState);
        tableRows[currentRow] = row2;
        currentRow++;
        settingsTableLayoutSprayer.addView(row2);

        // Third TableRow
        TableRow row3 = new TableRow(this);
        row3.setLayoutParams(params);
        row3.setWeightSum(6);

        TextView textView3 = new TextView(this);
        textView3.setLayoutParams(params2);
        textView3.setText("Highlight Color");

        Button button2 = new Button(this);
        button2.setLayoutParams(params3);
        button2.setId(View.generateViewId());
        button2.setBackgroundColor(color2);
        button2.setText("Choose Color");

        row3.addView(textView3);
        row3.addView(button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDynamicColorPickerDialog("section"+sectionId+"ThickPathColor", v, true);
            }
        });
        TooltipCompat.setTooltipText(row3, sectionId);
        row3.setTag(sectionId);
        row3.setVisibility(viewState);
        tableRows[currentRow] = row3;
        currentRow++;
        settingsTableLayoutSprayer.addView(row3);
    }
    private void showCurrentStep() {
        if (currentStep == 0) {
            settingsTableNumberSections.setVisibility(View.VISIBLE);
        } else {
            settingsTableNumberSections.setVisibility(View.GONE);
        }
        if (currentStep == 1) {
            startForm();
        }
        for (int i = 0; i < tableRows.length; i++) {
            int step = Integer.parseInt(tableRows[i].getTag().toString());
            tableRows[i].setVisibility(step == currentStep ? View.VISIBLE : View.GONE);
        }
        backButton.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);
        nextButton.setVisibility(currentStep < totalSections ? View.VISIBLE : View.GONE);
        doneButton.setVisibility(currentStep == totalSections ? View.VISIBLE : View.GONE);
    }
    private void finishWizard() {
        currentStep = 0;
        startForm();
        showCurrentStep();
    }
    private void showNextStep() {
        if (currentStep < totalSections) {
            currentStep++;
            showCurrentStep();
        }
    }

    private void showPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            showCurrentStep();
        }
    }
}