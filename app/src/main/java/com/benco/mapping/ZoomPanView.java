package com.benco.mapping;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ValueAnimator;

public class ZoomPanView extends View {
    private Bitmap canvasBitmap;
    private Paint paint;
    private float scale = 1f;
    private float translateX = 0f, translateY = 0f;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;
    private float maxVelocity;
    private ValueAnimator animator;

    public ZoomPanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        maxVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
    }

    public void setBitmap(Bitmap bitmap) {
        this.canvasBitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvasBitmap != null) {
            canvas.save();
            canvas.translate(translateX, translateY);
            canvas.scale(scale, scale);
            canvas.drawBitmap(canvasBitmap, 0, 0, paint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;

                // Initialize VelocityTracker
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    translateX += dx;
                    translateY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                }
                velocityTracker.addMovement(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000, maxVelocity);
                    float velocityX = velocityTracker.getXVelocity();
                    float velocityY = velocityTracker.getYVelocity();
                    startFling(velocityX, velocityY);
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return true;
    }
    private void fitImageToScreen() {
        if (canvasBitmap != null) {
            // Scale the image to fit the screen
            float bitmapWidth = canvasBitmap.getWidth();
            float bitmapHeight = canvasBitmap.getHeight();
            float screenWidth = getWidth();
            float screenHeight = getHeight();

            // Calculate the scale to fit the screen while maintaining aspect ratio
            scale = Math.min(screenWidth / bitmapWidth, screenHeight / bitmapHeight);
            translateX = (screenWidth - (bitmapWidth * scale)) / 2; // Center the image
            translateY = (screenHeight - (bitmapHeight * scale)) / 2; // Center the image
        }
    }

    private void startFling(float velocityX, float velocityY) {
        // Cancel any ongoing animation
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        // Create an interpolator for smoother transitions
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(300); // Duration of the fling
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            translateX += velocityX * animatedValue * 0.1f;
            translateY += velocityY * animatedValue * 0.1f;

            // Apply boundary checks
            applyBoundaryChecks();
            invalidate();
        });

        animator.start();
    }

    private void applyBoundaryChecks() {
        float bitmapWidth = canvasBitmap.getWidth() * scale;
        float bitmapHeight = canvasBitmap.getHeight() * scale;

        float minX = getWidth() - bitmapWidth;
        float maxX = 0;
        float minY = getHeight() - bitmapHeight;
        float maxY = 0;

        translateX = Math.max(minX, Math.min(translateX, maxX));
        translateY = Math.max(minY, Math.min(translateY, maxY));
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();
            scale = Math.max(0.1f, Math.min(scale, 10.0f));
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            startFling(velocityX, velocityY);
            return true;
        }
    }
    public void zoomIn() {
        scale *= 1.1f; // Increase scale by 10%
        applyBoundaryChecks();
        invalidate();
    }

    public void zoomOut() {
        scale /= 1.1f; // Decrease scale by 10%
        applyBoundaryChecks();
        invalidate();
    }

    public void resetZoom() {
        scale = 1f; // Reset scale to default
        translateX = 0f; // Reset translation
        translateY = 0f; // Reset translation
        invalidate();
    }

}