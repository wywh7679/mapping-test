package com.benco.mapping;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PerspectiveGridView extends View {
    private Paint paint;
    private Camera camera;

    // Define vanishing point coordinates
    private float vanishingPointX;
    private float vanishingPointY;

    public PerspectiveGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        camera = new Camera();
        int h  = getHeight();
        int w = getWidth();
        // Set default vanishing point (center of the view)
        vanishingPointX = 0; // Will be set in onSizeChanged
        vanishingPointY =0; // Will be set in onSizeChanged
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Set vanishing point to the center of the view
        vanishingPointX = 0;
        vanishingPointY = 0;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        //super.onDraw(canvas);
        canvas.save();
       // canvas.rotate(2);
        // Apply camera transformations
        camera.save();
        // Rotate for perspective
        canvas.drawRect(0, 0, getWidth(), getHeight()/2, paint);
        camera.rotateZ(0);
        camera.rotateY(0);
        camera.rotateX(0);
        Log.d("perspective view", "camera distance: "+getCameraDistance());
        //camera.translate(0,0,5);
        camera.applyToCanvas(canvas);
        camera.restore();
        drawGrid(canvas);
        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);

        int width = getWidth();
        int height = getHeight();

        // Draw horizontal lines
        for (int i = height/2; i <= height; i += 50) {
            canvas.drawLine(-width*2, i, width*2, i, paint);
        }
        // Draw vertical lines
        for (int i = -width; i < width*2; i += 100) {
            float scale = (float) (width - i) / width; // Perspective effect
            int adjustedHeight = (int) (height/2 * (scale*1));

            canvas.drawLine(i,-height/2, vanishingPointY + adjustedHeight, height, paint);
        }
    }

    // Method to set the vanishing point
    public void setVanishingPoint(float x, float y) {
        this.vanishingPointX = x;
        this.vanishingPointY = y;
        invalidate(); // Redraw the view
    }
}