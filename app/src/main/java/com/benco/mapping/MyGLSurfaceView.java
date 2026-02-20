package com.benco.mapping;

import android.content.Context;
//import android.opengl.EGLConfig;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyRenderer renderer;
    private float previousAngle;
    private boolean isRotating;

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
    }

    public void initRenderer(float[] coords) {
        if (renderer != null) {
            return;
        }
        setEGLConfigChooser(new MyConfigChooser());
        renderer = new MyRenderer(getContext(), coords);
        setRenderer(renderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (renderer == null) {
            return true;
        }
        if (event.getPointerCount() >= 2) {
            handleRotateGesture(event);
            return true;
        }
        // Pass single-touch events to the renderer for panning
        renderer.handleTouchEvent(event);
        return true;
    }

    public MyRenderer getRenderer() {
        return renderer;
    }

    private void handleRotateGesture(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    previousAngle = calculateAngle(event);
                    isRotating = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isRotating || event.getPointerCount() < 2) {
                    break;
                }
                float currentAngle = calculateAngle(event);
                float delta = currentAngle - previousAngle;
                renderer.addYaw(delta);
                previousAngle = currentAngle;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isRotating = false;
                break;
        }
    }

    private float calculateAngle(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
}
class MyConfigChooser implements GLSurfaceView.EGLConfigChooser {
    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int attribs[] = {
                EGL10.EGL_LEVEL, 0,
                EGL10.EGL_RENDERABLE_TYPE, 4,  // EGL_OPENGL_ES2_BIT
                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_SAMPLE_BUFFERS, 1,
                EGL10.EGL_SAMPLES, 4,  // This is for 4x MSAA.
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] configCounts = new int[1];
        egl.eglChooseConfig(display, attribs, configs, 1, configCounts);

        if (configCounts[0] == 0) {
            // Failed! Error handling.
            return null;
        } else {
            return configs[0];
        }
    }
}