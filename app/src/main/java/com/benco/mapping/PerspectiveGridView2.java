package com.benco.mapping;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PerspectiveGridView2 extends View {
    private Paint paint;
    private Camera camera;
    private float vanishingPointX;
    private float vanishingPointY;
    private float rotationX = 30; // Adjust this for different angles

    public PerspectiveGridView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        camera = new Camera();

        // Default vanishing point coordinates
        vanishingPointX = 0; // Will be set in onSizeChanged
        vanishingPointY = 0; // Will be set in onSizeChanged
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Set vanishing point to the center of the view
        vanishingPointX = w / 2;
        vanishingPointY = h / 2;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();

        // Apply camera transformations to the canvas
        camera.save();
        camera.rotateX(rotationX); // Rotate to create perspective
        camera.applyToCanvas(canvas);
        camera.restore();

        // Draw the grid
        drawGrid(canvas);

        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(2);

        int width = getWidth();
        int height = getHeight();

        // Draw horizontal lines
        for (int i = 0; i < height; i += 50) {
            float scale = (float) (height - i) / height; // Perspective effect
            int adjustedWidth = (int) (width * scale);
            canvas.drawLine(vanishingPointX - adjustedWidth / 2, i, vanishingPointX + adjustedWidth / 2, i, paint);
        }

        // Draw vertical lines
        for (int i = 0; i < width; i += 50) {
            float scale = (float) (width - i) / width; // Perspective effect
            int adjustedHeight = (int) (height * scale);
            canvas.drawLine(i, vanishingPointY - adjustedHeight / 2, i, vanishingPointY + adjustedHeight / 2, paint);
        }
    }

    // Method to set rotation
    public void setRotationX(float angle) {
        this.rotationX = angle;
        invalidate(); // Redraw the view
    }

    // Method to set the vanishing point
    public void setVanishingPoint(float x, float y) {
        this.vanishingPointX = x;
        this.vanishingPointY = y;
        invalidate(); // Redraw the view
    }
}
