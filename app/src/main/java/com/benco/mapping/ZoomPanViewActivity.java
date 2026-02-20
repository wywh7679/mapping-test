package com.benco.mapping;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ZoomPanViewActivity extends AppCompatActivity {
    private ZoomPanView zoomPanView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zoom_pan_view);

        zoomPanView = findViewById(R.id.zoomPanView);
        //Bitmap largeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.your_large_image);
        //zoomPanView.setBitmap(largeBitmap);

        Button btnZoomIn = findViewById(R.id.btnZoomIn);
        Button btnZoomOut = findViewById(R.id.btnZoomOut);
        Button btnResetZoom = findViewById(R.id.btnResetZoom);

        btnZoomIn.setOnClickListener(v -> zoomPanView.zoomIn());
        btnZoomOut.setOnClickListener(v -> zoomPanView.zoomOut());
        btnResetZoom.setOnClickListener(v -> zoomPanView.resetZoom());
    }
}