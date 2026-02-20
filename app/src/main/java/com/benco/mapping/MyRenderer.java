package com.benco.mapping;

import static android.opengl.GLES20.glBlendEquationSeparate;
import static android.opengl.GLES20.glBlendFuncSeparate;
import static android.opengl.GLES20.glDepthFunc;
import static android.opengl.GLES20.glGetAttribLocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements MyGLSurfaceView.Renderer {
    private String TAG = "MyRenderer";
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] gridModelMatrix = new float[16];
    private FloatBuffer gridVertexBuffer;
    private FloatBuffer gridBackgroundBuffer;
    private FloatBuffer mainPathBuffer;
    private FloatBuffer leftPathBuffer;
    private FloatBuffer rightPathBuffer;
    private FloatBuffer thickPathBuffer;
    private FloatBuffer thickPathUvBuffer;
    private FloatBuffer thickPathColorBuffer;
    private FloatBuffer abLineBuffer;
    private int abLineCount;
    private FloatBuffer steeringLineBuffer;
    private int steeringLineCount;
    private final List<FloatBuffer> guideLineBuffers = new ArrayList<>();
    private final List<Integer> guideLineCounts = new ArrayList<>();
    private final List<StripLine> leftLines = new ArrayList<>();
    private final List<StripLine> rightLines = new ArrayList<>();
    private int mainPathCount;
    private int leftPathCount;
    private int rightPathCount;
    private int thickPathCount;
    private final float[] mainPathColor = new float[]{0f, 1f, 0f, .5f};
    private final float[] leftPathColor = new float[]{1f, 1f, 1f, 1f};
    private final float[] rightPathColor = new float[]{1f, 1f, 1f, 1f};
    private final float[] thickPathColor = new float[]{1f, 0f, 0f, 0.5f};
    private final float[] abLineColor = new float[]{0f, 1f, 0f, 1f};
    private final float[] guideLineColor = new float[]{1f, 1f, 1f, 1f};
    private final float[] steeringLineColor = new float[]{0f, 0f, 1f, 1f};
    private float mainPathHalfWidth = 1.0f;
    private int texturedProgram;
    private int mainLineTextureId;
    private int leftLineTextureId;
    private int rightLineTextureId;
    private int program;
    private Bounds pathBounds;
    private final float[] coordinates; // Array of coordinates
    private static final int MAX_SECTIONS = 16;
    private int[] lineFbos = new int[MAX_SECTIONS * 2];
    private int[] lineTextures = new int[MAX_SECTIONS * 2];
    private int fboWidth;
    private int fboHeight;
    private int mainPathFbo;
    private int mainPathTexture;
    private int abLineFbo;
    private int abLineTexture;
    private int guideLinesFbo;
    private int guideLinesTexture;
    private int steeringLineFbo;
    private int steeringLineTexture;
    private int fullscreenProgram;
    private FloatBuffer fullscreenVertexBuffer;
    private FloatBuffer fullscreenUvBuffer;

    int parsedFillColor = Color.parseColor("#121314");
    float fillRed = Color.red(parsedFillColor)/255f;
    float fillGreen = Color.green(parsedFillColor)/255f;
    float fillBlue = Color.blue(parsedFillColor)/255f;
    float fillAlpha = 1.0f;

    int parsedGridColor = Color.parseColor("#780000");
    float gridRed = Color.red(parsedGridColor)/255f;
    float gridGreen = Color.green(parsedGridColor)/255f;
    float gridBlue = Color.blue(parsedGridColor)/255f;
    float gridAlpha =.76f;


    float gridBackgroundAlpha = 0.78f;
    float gridBackgroundRed = .12f;
    float gridBackgroundGreen = .12f;
    float gridBackgroundBlue = .12f;

    private static final float GRID_HALF_SIZE = 126720f; //Two miles in inches
    private static final float GRID_STEP = 120f;
    private static final float GRID_Y = 0.01f;
    private float gridStep = GRID_STEP;
    private float gridAzimuth = 0f;
    private String gridSizeLabel = "10 ft";
    //float cameraY = -.12f;
    //float cameraX = (float) (radius * Math.sin(Math.toRadians(yaw)));
   // float cameraZ = (float) (radius * Math.cos(Math.toRadians(yaw)));
    private float previousX, previousY;
    private boolean isDragging;
    private float panSensitivity;
    float maxPanSensitivity = 978f;
    float minPanSensitivity = 5f;
    Context context;
    private int width;
    private int height;


    List<Point> absPoints;
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;

    //private float cameraX = 0.0f, cameraY = 0.0f, cameraZ = 9f; //top down...frustumM
    //private float cameraX = 0.0f, cameraY = -1.0f, cameraZ = 10f; //angled frustumM


    /*
    The most important values:
        cameraHeight (Y offset)
        Higher camera → you look downward more; lower camera → flatter angle.
        Defined in updateViewMatrix() as cameraHeight = 2.0f.

        cameraBehind (distance behind vehicle)
        Farther back → shallower tilt (grid looks flatter).
        Closer → steeper tilt (grid looks more “in your face”).
        Defined in updateViewMatrix() as cameraBehind = 6.0f.

        lookAhead (how far forward camera looks)
        Shorter lookAhead → camera points more downward toward the vehicle.
        Longer → camera points more toward the horizon.
        Defined in updateViewMatrix() as lookAhead = 3.0f.

        yaw and pitch
        yaw rotates the camera around the vertical axis.
        pitch (if used) would tilt the camera up/down; currently, you are not using pitch in the chase‑camera math, only yaw.

    Key values that move the horizon up/down:
        Camera height and look direction
        A higher camera or longer lookAhead pushes the horizon higher in view.
        A lower camera or shorter lookAhead pushes it down (more of the grid dominates the view).
        Set in updateViewMatrix(). 1

        Field of View (FOV)
        Your projection uses a very wide FOV = 120°, which pushes the horizon farther down and exaggerates perspective.
        Set in onSurfaceChanged() via Matrix.perspectiveM(...). 1
        Narrower FOV (e.g., 45–70°) makes the horizon feel higher and less distorted.

        Near/Far plane
        These don’t move the horizon but can clip the grid if they’re too restrictive.
        Also set in Matrix.perspectiveM(...)
     */


    private float cameraX, cameraY, cameraZ;
    float yaw = 0f;
    private float pitch = 0f;

    // Initial (screen load) camera state
    private float initialCameraBehind;
    private float initialCameraHeight;
    private float initialYaw;

    // Path end position
    private float pathEndX;
    private float pathEndZ;

    float vehicleX = 0;
    float vehicleY = 0;
    float vehicleZ = 0;
    float cameraBehind;
    float cameraHeight;
    float lookAhead;

    float minCameraBehind = 200f;
    float maxCameraBehind = (GRID_HALF_SIZE/48);
    float minCameraHeight = 200f;
    float maxCameraHeight = GRID_HALF_SIZE-500f;
    private float gridHalfSize = GRID_HALF_SIZE;

    public MyRenderer(Context context, float[] coords) {
        this.context = context;  // Initialize context
        getScreenDimensions(context);
        this.coordinates = coords;
        Matrix.setIdentityM(gridModelMatrix, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height); // Set the viewport
        // Create a perspective projection matrix
        float ratio = (float) width / height;
        if (width > height) {
            // Landscape
            cameraBehind = minCameraBehind;
            cameraHeight = minCameraHeight;
            lookAhead = 10.0f;
            Matrix.perspectiveM(projectionMatrix, 0, 78f, ratio, 0.5f, gridHalfSize*2);
        } else {
            cameraBehind = minCameraBehind;
            cameraHeight = minCameraHeight;
            lookAhead = 10.0f;
            // Portrait or square
            Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.5f, gridHalfSize);
        }
        fboWidth = width;
        fboHeight = height;

        GLES20.glGenFramebuffers(lineFbos.length, lineFbos, 0);
        GLES20.glGenTextures(lineTextures.length, lineTextures, 0);

        for (int i = 0; i < lineFbos.length; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lineTextures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                    GLES20.GL_RGBA, fboWidth, fboHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, lineFbos[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, lineTextures[i], 0);
        }
        int[] extraFbos = new int[4];
        int[] extraTextures = new int[4];
        GLES20.glGenFramebuffers(extraFbos.length, extraFbos, 0);
        GLES20.glGenTextures(extraTextures.length, extraTextures, 0);
        mainPathFbo = extraFbos[0];
        abLineFbo = extraFbos[1];
        guideLinesFbo = extraFbos[2];
        steeringLineFbo = extraFbos[3];
        mainPathTexture = extraTextures[0];
        abLineTexture = extraTextures[1];
        guideLinesTexture = extraTextures[2];
        steeringLineTexture = extraTextures[3];
        bindFboTexture(mainPathFbo, mainPathTexture);
        bindFboTexture(abLineFbo, abLineTexture);
        bindFboTexture(guideLinesFbo, guideLinesTexture);
        bindFboTexture(steeringLineFbo, steeringLineTexture);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    private float[] updateViewMatrix() {
        float[] viewMatrix = new float[16];

        float yawRad = (float) Math.toRadians(yaw);
        float forwardX = (float) Math.sin(yawRad);
        float forwardZ = (float) Math.cos(yawRad);
        float forwardY = (float) Math.sin(pitch);

        // Camera position
        cameraX = vehicleX - forwardX * cameraBehind;
        cameraY = vehicleY + cameraHeight; //+forwardY;
        cameraZ = vehicleZ - forwardZ * cameraBehind;

        // Look-at target
        float lookX = vehicleX + forwardX * lookAhead;
        float lookY = vehicleY;
        float lookZ = vehicleZ + forwardZ * lookAhead;

        // Build view matrix
        Matrix.setLookAtM(viewMatrix, 0,
                cameraX, cameraY, cameraZ,
                lookX, lookY, lookZ,
                0f, 1f, 0f);

        return viewMatrix;
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig eglConfig) {
        GLES20.glClearColor(fillRed, fillGreen, fillBlue, fillAlpha); // Clear color
        // Set the line width
        GLES20.glLineWidth(10.0f); // Set line thickness
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Enable depth testing
        GLES20.glDepthMask(true);
        glDepthFunc(GLES20.GL_LEQUAL);
        //glBlendFuncSeparate(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_CONSTANT_ALPHA, GLES20.GL_CONSTANT_ALPHA);
        //glBlendEquationSeparate( GLES20.GL_FUNC_ADD, GLES20.GL_FUNC_ADD );
        //glBlendFuncSeparate( GLES20.GL_ONE, GLES20.GL_ONE, GLES20.GL_ONE, GLES20.GL_ONE );

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);


        float[] quadVertices = new float[]{
                -GRID_HALF_SIZE, -1f, -GRID_HALF_SIZE,
                GRID_HALF_SIZE, -1f, -GRID_HALF_SIZE,
                -GRID_HALF_SIZE, -1f, GRID_HALF_SIZE,
                GRID_HALF_SIZE, -1f, GRID_HALF_SIZE
        };

        ByteBuffer quadBuffer = ByteBuffer.allocateDirect(quadVertices.length * 4);
        quadBuffer.order(ByteOrder.nativeOrder());
        gridBackgroundBuffer = quadBuffer.asFloatBuffer();
        gridBackgroundBuffer.put(quadVertices);
        gridBackgroundBuffer.position(0);

        // Create the grid vertices
        //float[] gridVertices = createThickGrid(1000, 1000, 25, .0f);
        //float[] gridVertices = calculateGridScale();

        /*float[] gridVertices = createGrid3D(gridHalfSize, GRID_STEP);
        ByteBuffer bb2 = ByteBuffer.allocateDirect(gridVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        gridVertexBuffer = bb2.asFloatBuffer();
        gridVertexBuffer.put(gridVertices);
        gridVertexBuffer.position(0);*/

        rebuildGridBuffers();


        // Load shaders and create a program
        String vertexShaderCode = loadShaderSource(R.raw.vertex_shader);
        String fragmentShaderCode = loadShaderSource(R.raw.fragment_shader);
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glCompileShader(vertexShader);

        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShader);
        program = createProgram(vertexShaderCode, fragmentShaderCode);

        texturedProgram = createProgram(
                loadShaderSource(R.raw.line_texture_vertex),
                loadShaderSource(R.raw.line_texture_fragment)
        );

        mainLineTextureId = createSolidTexture(Color.WHITE);
        leftLineTextureId = createSolidTexture(Color.LTGRAY);
        rightLineTextureId = createSolidTexture(Color.DKGRAY);

        // Fullscreen quad vertices in clip space
        float[] quadVerts = {
                -1f, -1f,
                1f, -1f,
                -1f,  1f,
                1f,  1f
        };

        float[] quadUvs = {
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f
        };

        fullscreenVertexBuffer = ByteBuffer.allocateDirect(quadVerts.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fullscreenVertexBuffer.put(quadVerts).position(0);

        fullscreenUvBuffer = ByteBuffer.allocateDirect(quadUvs.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fullscreenUvBuffer.put(quadUvs).position(0);

        // Compile fullscreen shader program
        fullscreenProgram = createProgram(
                loadShaderSource(R.raw.fullscreen_quad_vertex),
                loadShaderSource(R.raw.fullscreen_quad_fragment)
        );
    }

    public void getScreenDimensions(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // Draw lines with blending (disable depth if needed)
        GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        drawGrid();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        drawPathLines();
        float[] updatedView = updateViewMatrix();
        System.arraycopy(updatedView, 0, viewMatrix, 0, 16);
    }

    public void captureInitialCamera() {
        initialCameraBehind = cameraBehind;
        initialCameraHeight = cameraHeight;
        initialYaw = yaw;
    }
    public void setPathEndFromGeometry(float[] mainVertices) {
        if (mainVertices == null || mainVertices.length < 3) return;

        int last = mainVertices.length - 3;
        pathEndX = mainVertices[last];
        pathEndZ = mainVertices[last + 2];
    }
    public void resetCameraToPathEnd() {
        cameraBehind = initialCameraBehind;
        cameraHeight = initialCameraHeight;
        yaw = initialYaw;

        vehicleX = pathEndX;
        vehicleZ = pathEndZ;
    }
    private float[] createGrid3D(float halfSize, float step) {
        List<Float> vertices = new ArrayList<>();

        for (float i = -halfSize; i <= halfSize; i += step) {
            // Line parallel to Z (constant X = i)
            vertices.add(i);
            vertices.add(GRID_Y);
            vertices.add(-halfSize);
            vertices.add(i);
            vertices.add(GRID_Y);
            vertices.add(halfSize);

            // Line parallel to X (constant Z = i)
            vertices.add(-halfSize);
            vertices.add(GRID_Y);
            vertices.add(i);
            vertices.add(halfSize);
            vertices.add(GRID_Y);
            vertices.add(i);
        }

        float[] out = new float[vertices.size()];
        for (int j = 0; j < vertices.size(); j++) out[j] = vertices.get(j);
        return out;
    }
    public void setGridAzimuth(float degrees) {
        gridAzimuth = degrees;
    }
    private void drawGrid() {
        GLES20.glUseProgram(program);

        int colorHandle = GLES20.glGetUniformLocation(program, "u_Color");
        //GLES20.glUniform4f(colorHandle, gridRed, gridGreen, gridBlue, gridAlpha); // Pass red color


        int positionHandle = glGetAttribLocation(program, "a_Position");
        gridVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, gridVertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);


        //float snappedX = Math.round(vehicleX / GRID_STEP) * GRID_STEP;
       // float snappedZ = Math.round(vehicleZ / GRID_STEP) * GRID_STEP;
        float snappedX = Math.round(vehicleX / gridStep) * gridStep;
        float snappedZ = Math.round(vehicleZ / gridStep) * gridStep;

        Matrix.setIdentityM(gridModelMatrix, 0);
        Matrix.rotateM(gridModelMatrix, 0, -gridAzimuth, 0f, 1f, 0f); // rotate around Y
        Matrix.translateM(gridModelMatrix, 0, snappedX, 0f, snappedZ);

        // Set the Model-View-Projection matrix
        final float[] temp = new float[16];
        //Matrix.multiplyMM(temp, 0, projectionMatrix, 0, viewMatrix, 0);
        //System.arraycopy(temp, 0, modelViewProjectionMatrix, 0, temp.length);

        Matrix.multiplyMM(temp, 0, viewMatrix, 0, gridModelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, temp, 0);

        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjectionMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);

        // Draw the grid background
        if (gridBackgroundBuffer != null) {
            gridBackgroundBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, gridBackgroundBuffer);
            GLES20.glEnableVertexAttribArray(positionHandle);

            GLES20.glUniform4f(colorHandle, gridBackgroundRed, gridBackgroundGreen, gridBackgroundBlue, gridBackgroundAlpha);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

        // Draw the grid lines
        gridVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, gridVertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniform4f(colorHandle, gridRed, gridGreen, gridBlue, gridAlpha);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridVertexBuffer.capacity()/COORDS_PER_VERTEX); // Adjust based on vertex count

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private void drawPathLines() {
        if (mainPathBuffer == null || mainPathCount == 0) {
            return;
        }
        GLES20.glUseProgram(program);
        int positionHandle = glGetAttribLocation(program, "a_Position");
        int colorHandle = GLES20.glGetUniformLocation(program, "u_Color");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjectionMatrix");

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);

        boolean useFrameBuffer = true;

        if (useFrameBuffer) {
            renderLineBufferToFbo(mainPathBuffer, mainPathCount, mainPathColor, mainPathFbo);
            if (steeringLineBuffer != null && steeringLineCount > 0) {
                renderLineBufferToFbo(steeringLineBuffer, steeringLineCount, steeringLineColor, steeringLineFbo);
            }
            if (!guideLineBuffers.isEmpty()) {
                renderGuideLinesToFbo(guideLinesFbo, guideLineColor);
            }
            renderLineBufferToFbo(abLineBuffer, abLineCount, abLineColor, abLineFbo);
        } else {
            drawLineStrip(mainPathBuffer, mainPathCount, positionHandle, colorHandle, mainPathColor);
            if (steeringLineBuffer != null && steeringLineCount > 0) {
                drawLineStrip(steeringLineBuffer, steeringLineCount, positionHandle, colorHandle, steeringLineColor);
            }
            if (!guideLineBuffers.isEmpty()) {
                for (int i = 0; i < guideLineBuffers.size(); i++) {
                    FloatBuffer buffer = guideLineBuffers.get(i);
                    int count = guideLineCounts.get(i);
                    if (buffer != null && count > 0) {
                        drawLineStrip(buffer, count, positionHandle, colorHandle, guideLineColor);
                    }
                }
            }
        }
        if (abLineBuffer != null && abLineCount > 0) {
            //drawLineStrip(abLineBuffer, abLineCount, positionHandle, colorHandle, abLineColor);
        }

/*
            if (thickPathBuffer != null && thickPathCount > 0) {
                //drawTexturedTriangleStrip(thickPathBuffer, thickPathUvBuffer, thickPathColorBuffer, thickPathCount, mainLineTextureId);
            }
            //drawLineStrip(mainPathBuffer, mainPathCount, positionHandle, colorHandle, mainPathColor);
            if (abLineBuffer != null && abLineCount > 0) {
                drawLineStrip(abLineBuffer, abLineCount, positionHandle, colorHandle, abLineColor);
            }
            if (!guideLineBuffers.isEmpty()) {
                for (int i = 0; i < guideLineBuffers.size(); i++) {
                    FloatBuffer buffer = guideLineBuffers.get(i);
                    int count = guideLineCounts.get(i);
                    if (buffer != null && count > 0) {
                        drawLineStrip(buffer, count, positionHandle, colorHandle, guideLineColor);
                    }
                }
            }
            if (steeringLineBuffer != null && steeringLineCount > 0) {
                drawLineStrip(steeringLineBuffer, steeringLineCount, positionHandle, colorHandle, steeringLineColor);
            }

*/
        List<StripLine> listCopyLeft = new ArrayList<>(leftLines);
        List<StripLine> listCopyRight = new ArrayList<>(rightLines);

        if (useFrameBuffer) {

            // Render each line to its own FBO (no blending)
            int lineIndex = 0;

            for (StripLine line : listCopyLeft) {
                renderLineToFbo(line, lineFbos[lineIndex], fboWidth, fboHeight);
                lineIndex++;
            }
            for (StripLine line : listCopyRight) {
                renderLineToFbo(line, lineFbos[lineIndex], fboWidth, fboHeight);
                lineIndex++;
            }
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, width, height);


            for (int i = 0; i < lineIndex; i++) {
                drawFullscreenTexture(lineTextures[i]);
            }
            drawFullscreenTexture(mainPathTexture);
            if (steeringLineTexture != 0) {
                drawFullscreenTexture(steeringLineTexture);
            }
            if (guideLinesTexture != 0) {
                drawFullscreenTexture(guideLinesTexture);
            }
            if (abLineTexture != 0) {
                drawFullscreenTexture(abLineTexture);
            }
        } else {
            for (StripLine line : listCopyLeft) {
                line.draw(texturedProgram, projectionMatrix, viewMatrix);
                //line.drawTriangleStrip(texturedProgram, projectionMatrix, viewMatrix);
            }
            for (StripLine line : listCopyRight) {
                line.draw(texturedProgram, projectionMatrix, viewMatrix);
                // line.drawTriangleStrip(texturedProgram, projectionMatrix, viewMatrix);
            }
        }
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
    private void drawFullscreenTexture(int textureId) {
        GLES20.glUseProgram(fullscreenProgram);

        int positionHandle = glGetAttribLocation(fullscreenProgram, "a_Position");
        int texCoordHandle = glGetAttribLocation(fullscreenProgram, "a_TexCoord");
        int textureHandle = GLES20.glGetUniformLocation(fullscreenProgram, "u_Texture");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        fullscreenVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, fullscreenVertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        fullscreenUvBuffer.position(0);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, fullscreenUvBuffer);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
    private void renderLineBufferToFbo(FloatBuffer buffer, int count, float[] color, int fbo) {
        if (buffer == null || count <= 0) {
            return;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, fboWidth, fboHeight);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);
        int positionHandle = glGetAttribLocation(program, "a_Position");
        int colorHandle = GLES20.glGetUniformLocation(program, "u_Color");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjectionMatrix");

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);

        drawLineStrip(buffer, count, positionHandle, colorHandle, color);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void renderGuideLinesToFbo(int fbo, float[] color) {
        if (guideLineBuffers.isEmpty()) {
            return;
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, fboWidth, fboHeight);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);
        int positionHandle = glGetAttribLocation(program, "a_Position");
        int colorHandle = GLES20.glGetUniformLocation(program, "u_Color");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjectionMatrix");

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);

        for (int i = 0; i < guideLineBuffers.size(); i++) {
            FloatBuffer buffer = guideLineBuffers.get(i);
            int count = guideLineCounts.get(i);
            if (buffer != null && count > 0) {
                drawLineStrip(buffer, count, positionHandle, colorHandle, color);
            }
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void bindFboTexture(int fbo, int textureId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, fboWidth, fboHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }
    private void renderLineToFbo(StripLine line, int fbo, int width, int height) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        line.draw(texturedProgram, projectionMatrix, viewMatrix);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setAbsPoints(List<Point> AbsPoints) {
        absPoints = AbsPoints;
        //Log.d(TAG, "absPoints: "+absPoints.toString());
    }
    private void drawLineStrip(FloatBuffer buffer, int count, int positionHandle, int colorHandle, float[] color) {
        buffer.position(0);
        GLES20.glUniform4f(colorHandle, color[0], color[1], color[2], color[3]);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, count);
    }

    private void drawTriangleStrip(FloatBuffer buffer, int count, int positionHandle, int colorHandle, float[] color) {
        buffer.position(0);
        GLES20.glUniform4f(colorHandle, color[0], color[1], color[2], color[3]);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, count);
    }

    private void drawTexturedTriangleStrip(FloatBuffer vertexBuffer, FloatBuffer uvBuffer,
                                           FloatBuffer colorBuffer, int count, int textureId) {
        if (vertexBuffer == null || uvBuffer == null || colorBuffer == null || textureId == 0) {
            return;
        }
        GLES20.glUseProgram(texturedProgram);
        int positionHandle = glGetAttribLocation(texturedProgram, "a_Position");
        int texCoordHandle = glGetAttribLocation(texturedProgram, "a_TexCoord");
        int colorHandle = glGetAttribLocation(texturedProgram, "a_Color");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(texturedProgram, "u_ModelViewProjectionMatrix");
        int textureHandle = GLES20.glGetUniformLocation(texturedProgram, "u_Texture");

        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        uvBuffer.position(0);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        colorBuffer.position(0);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);
        GLES20.glEnableVertexAttribArray(colorHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, count);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void setPathGeometry(PathGeometry geometry, List<SectionStyle> sectionStyles) {
        if (geometry == null) {
            mainPathBuffer = null;
            leftPathBuffer = null;
            rightPathBuffer = null;
            thickPathBuffer = null;
            mainPathCount = 0;
            leftPathCount = 0;
            rightPathCount = 0;
            thickPathCount = 0;
            leftLines.clear();
            rightLines.clear();
            return;
        }
        mainPathBuffer = buildFloatBuffer(geometry.getMainVertices());
        mainPathCount = geometry.getMainVertices().length / COORDS_PER_VERTEX;
        leftPathCount = geometry.getLeftVertices().size();
        rightPathCount = geometry.getRightVertices().size();
        rebuildThickPath(geometry.getMainVertices());
        updateBounds(geometry.getMainVertices());
        rebuildSectionLines(geometry, sectionStyles);
    }

    public void setABLineGeometry(ABLineGeometry geometry) {
        if (geometry == null || geometry.getAbLineVertices().length == 0) {
            abLineBuffer = null;
            abLineCount = 0;
            steeringLineBuffer = null;
            steeringLineCount = 0;
            guideLineBuffers.clear();
            guideLineCounts.clear();
            return;
        }
        abLineBuffer = buildFloatBuffer(geometry.getAbLineVertices());
        abLineCount = geometry.getAbLineVertices().length / COORDS_PER_VERTEX;
        float[] steeringVertices = geometry.getSteeringVertices();
        steeringLineBuffer = buildFloatBuffer(steeringVertices);
        steeringLineCount = steeringVertices == null ? 0 : steeringVertices.length / COORDS_PER_VERTEX;
        guideLineBuffers.clear();
        guideLineCounts.clear();
        for (float[] guideVertices : geometry.getGuideLineVertices()) {
            FloatBuffer buffer = buildFloatBuffer(guideVertices);
            guideLineBuffers.add(buffer);
            guideLineCounts.add(guideVertices.length / COORDS_PER_VERTEX);
        }
    }

    public void setPathColors(int mainColorInt, int leftColorInt, int rightColorInt) {
        setColor(mainPathColor, mainColorInt);
        setColor(leftPathColor, leftColorInt);
        setColor(rightPathColor, rightColorInt);
    }

    public void setPathWidths(float mainPathWidth, int thickColorInt) {
        mainPathHalfWidth = Math.max(0.1f, mainPathWidth / 2f);
        setColor(thickPathColor, thickColorInt);
        if (mainPathBuffer != null && mainPathCount > 1) {
            float[] mainVertices = new float[mainPathCount * COORDS_PER_VERTEX];
            mainPathBuffer.position(0);
            mainPathBuffer.get(mainVertices);
            mainPathBuffer.position(0);
            rebuildThickPath(mainVertices);
        }
    }

    private void setColor(float[] outColor, int colorInt) {
        outColor[0] = Color.red(colorInt) / 255f;
        outColor[1] = Color.green(colorInt) / 255f;
        outColor[2] = Color.blue(colorInt) / 255f;
        outColor[3] = Color.alpha(colorInt) / 255f;
    }

    private FloatBuffer buildFloatBuffer(float[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.position(0);
        return floatBuffer;
    }

    private float[] buildThickStripVertices(float[] lineVertices, float halfWidth, float z2) {
        if (lineVertices == null || lineVertices.length < COORDS_PER_VERTEX * 2) {
            return new float[0];
        }
        List<Float> strip = new ArrayList<>();
        int pointCount = lineVertices.length / COORDS_PER_VERTEX;
        for (int i = 0; i < pointCount; i++) {
            int idx = i * COORDS_PER_VERTEX;
            float x = lineVertices[idx];
            float y = lineVertices[idx + 1];
            float z = lineVertices[idx + 2];

            vehicleX = x;
            vehicleZ = y;

            float dirX;
            float dirZ;
            if (i == 0) {
                dirX = lineVertices[idx + 3] - x;
                dirZ = lineVertices[idx + 5] - z;
            } else if (i == pointCount - 1) {
                dirX = x - lineVertices[idx - 3];
                dirZ = z - lineVertices[idx - 1];
            } else {
                dirX = lineVertices[idx + 3] - lineVertices[idx - 3];
                dirZ = lineVertices[idx + 5] - lineVertices[idx - 1];
            }

            float len = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (len == 0f) {
                continue;
            }
            dirX /= len;
            dirZ /= len;

            float normalX = -dirZ;
            float normalZ = dirX;

            strip.add(x + normalX * halfWidth);
            strip.add(y);
            strip.add(z + normalZ * halfWidth);

            strip.add(x - normalX * halfWidth);
            strip.add(y);
            strip.add(z - normalZ * halfWidth);
        }

        float[] out = new float[strip.size()];
        for (int i = 0; i < strip.size(); i++) {
            out[i] = strip.get(i);
        }
        yaw = absPoints.get(absPoints.size()-1).bearing-180;
        return out;
    }

    private void rebuildThickPath(float[] mainVertices) {
        thickPathBuffer = buildFloatBuffer(buildThickStripVertices(mainVertices, mainPathHalfWidth, 1));
        thickPathCount = thickPathBuffer == null ? 0 : thickPathBuffer.capacity() / COORDS_PER_VERTEX;
        thickPathUvBuffer = buildFloatBuffer(buildThickStripUVs(mainVertices));
        thickPathColorBuffer = buildFloatBuffer(buildColorStrip(thickPathCount, thickPathColor));
    }

    private float[] buildThickStripUVs(float[] lineVertices) {
        if (lineVertices == null || lineVertices.length < COORDS_PER_VERTEX * 2) {
            return new float[0];
        }
        List<Float> uvs = new ArrayList<>();
        int pointCount = lineVertices.length / COORDS_PER_VERTEX;
        float totalDistance = 0f;
        float[] distances = new float[pointCount];
        for (int i = 1; i < pointCount; i++) {
            int idx = i * COORDS_PER_VERTEX;
            float dx = lineVertices[idx] - lineVertices[idx - COORDS_PER_VERTEX];
            float dz = lineVertices[idx + 2] - lineVertices[idx - COORDS_PER_VERTEX + 2];
            totalDistance += Math.sqrt(dx * dx + dz * dz);
            distances[i] = totalDistance;
        }
        float textureScale = 10f;
        for (int i = 0; i < pointCount; i++) {
            float v = distances[i] / textureScale;
            uvs.add(0f);
            uvs.add(v);
            uvs.add(1f);
            uvs.add(v);
        }
        float[] out = new float[uvs.size()];
        for (int i = 0; i < uvs.size(); i++) {
            out[i] = uvs.get(i);
        }
        return out;
    }

    private int createSolidTexture(int color) {
        int size = 64;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        bitmap.recycle();
        return textureId;
    }

    private float[] buildColorStrip(int vertexCount, float[] color) {
        if (vertexCount <= 0) {
            return new float[0];
        }
        float[] colors = new float[vertexCount * 4];
        for (int i = 0; i < vertexCount; i++) {
            int offset = i * 4;
            colors[offset] = color[0];
            colors[offset + 1] = color[1];
            colors[offset + 2] = color[2];
            colors[offset + 3] = color[3];
        }
        return colors;
    }

    private void rebuildSectionLines(PathGeometry geometry, List<SectionStyle> styles) {
        leftLines.clear();
        rightLines.clear();
        if (styles == null || styles.isEmpty()) {
            return;
        }
        int maxSections = Math.min(styles.size(), 16);
        for (int i = 0; i < maxSections; i++) {
            if (i < geometry.getLeftVertices().size()) {
                leftLines.add(new StripLine(geometry.getLeftVertices().get(i), styles.get(i), mainLineTextureId, i));
            }
            if (i < geometry.getRightVertices().size()) {
                rightLines.add(new StripLine(geometry.getRightVertices().get(i), styles.get(i), mainLineTextureId, i));
            }
        }
    }

    private class StripLine {
        private final FloatBuffer vertices;
        private final FloatBuffer uvs;
        private final FloatBuffer colors;
        private final int count;
        private final int textureId;
        private float zIndex;
        StripLine(float[] lineVertices, SectionStyle style, int textureId ,float z) {
            float halfWidth = Math.max(0.1f, style.getWidth() / 2f);
            float[] strip = buildThickStripVertices(lineVertices, halfWidth, z);
            this.vertices = buildFloatBuffer(strip);
            this.count = strip.length / COORDS_PER_VERTEX;
            this.uvs = buildFloatBuffer(buildThickStripUVs(lineVertices));
            float[] color = new float[4];
            setColor(color, style.getColor());
            this.zIndex = z;
            this.colors = buildFloatBuffer(buildColorStrip(count, color));
            this.textureId = textureId;
        }
        void draw(int program, float[] projection, float[] view) {
            if (vertices == null || uvs == null || colors == null || count == 0) {
                return;
            }
            GLES20.glUseProgram(program);
            int positionHandle = glGetAttribLocation(program, "a_Position");
            int texCoordHandle = glGetAttribLocation(program, "a_TexCoord");
            int colorHandle = glGetAttribLocation(program, "a_Color");
            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjectionMatrix");
            int textureHandle = GLES20.glGetUniformLocation(program, "u_Texture");

            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projection, 0, view, 0);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(textureHandle, 0);

            vertices.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertices);
            GLES20.glEnableVertexAttribArray(positionHandle);

            uvs.position(0);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvs);
            GLES20.glEnableVertexAttribArray(texCoordHandle);

            colors.position(0);
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colors);
            GLES20.glEnableVertexAttribArray(colorHandle);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, count);

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
            GLES20.glDisableVertexAttribArray(colorHandle);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }
    public void setCameraBehind(float value, boolean absolute) {
        if (absolute) {
            if (value < minCameraBehind) value = minCameraBehind;
            if (value > maxCameraBehind) value = maxCameraBehind;
            cameraBehind = Math.max(minCameraBehind, value);
        } else {
            if (cameraBehind + value < minCameraBehind) value = minCameraBehind-cameraBehind;
            if (cameraBehind + value > maxCameraBehind) value = maxCameraBehind-cameraBehind;
            cameraBehind = Math.max(minCameraBehind, cameraBehind + value);
        }
        //Log.d(TAG, "setCameraBehind: " + cameraBehind);
    }

    public void setCameraHeight(float value,  boolean absolute) {
        if (absolute) {
            if (value < minCameraHeight) value = minCameraHeight;
            if (value > maxCameraHeight) value = maxCameraHeight;
            cameraHeight = Math.max(minCameraHeight, value);
        } else {
            if (cameraHeight + value < minCameraHeight) value = minCameraHeight-cameraHeight;
            if (cameraHeight + value > maxCameraHeight) value = maxCameraHeight-cameraHeight;
            cameraHeight = Math.max(minCameraHeight, cameraHeight+value);
        }
        //Log.d(TAG, "setCameraHeight: " + cameraHeight);
    }

    public float getCameraBehind() {
        return cameraBehind;
    }
    public float getCameraYaw() {
        return yaw;
    }
    public void addYaw(float deltaDegrees) {
        yaw += deltaDegrees;
    }
    public void fitCameraToBounds() {
        if (pathBounds == null) {
            return;
        }
        float width = pathBounds.maxX - pathBounds.minX;
        float depth = pathBounds.maxZ - pathBounds.minZ;
        float largest = Math.max(width, depth);
        if (largest <= 0f) {
            return;
        }
        boolean moveCamera = false;
        if (moveCamera) {
            vehicleX = (pathBounds.minX + pathBounds.maxX) / 2f;
            vehicleZ = (pathBounds.minZ + pathBounds.maxZ) / 2f;
            float fovRadians = (float) Math.toRadians(60f);
            float distance = (largest / 2f) / (float) Math.tan(fovRadians / 2f);
            cameraBehind = Math.max(2f, distance);
        }
        updateGridHalfSize(largest * 2f);
    }
    public void updateGridHalfSize(float halfSize) {
        gridHalfSize = Math.max(GRID_HALF_SIZE, halfSize * 1.1f);
        rebuildGridBuffers();
    }
    public void setGridScaleFromZoom(float cameraDistance) {
        float newStep = gridStep;
        String label = "10 ft";
        //Log.d(TAG, "cameraDistance: "+cameraDistance);
        if (cameraDistance < minCameraHeight) {
            newStep = 120f; // 10 ft in inches
            label = "10 ft";
        } else if (cameraDistance < 2000f) {
            newStep = 300f; // 25 ft
            label = "25 ft";
        } else if (cameraDistance < 4000f) {
            newStep = 600f; // 50 ft
            label = "50 ft";
        } else if (cameraDistance < 6000f) {
            newStep = 1200f; // 100 ft
            label = "100 ft";
        } else if (cameraDistance < 8000f) {
            newStep = 3000f; // 250 ft
            label = "250 ft";
        } else if (cameraDistance < 10000f) {
            newStep = 6000f; // 500 ft
            label = "500 ft";
        } else if (cameraDistance < 14000f) {
            newStep = 15840f; // 1/4 mile
            label = "1/4 mile";
        } else {
            newStep = 31680f; // 1/2 mile
            label = "1/2 mile";
        }
        if (newStep != gridStep) {
            gridStep = newStep;
            gridSizeLabel = label;
            rebuildGridBuffers();
        } else {
            gridSizeLabel = label;
        }
    }

    public String getGridSizeLabel() {
        return gridSizeLabel;
    }
    public float[] calculateGridScale() {
        float gridScale;
        float gridLineWidth;
        String gridSizeText;
        if (cameraHeight < 3000f) {
            //10 feet
            gridScale = 10f;
            gridLineWidth = 6f;
            gridSizeText = "1o ft";
        } else if (cameraHeight <= 4000f && cameraHeight > 3000f) {
            //25 feet
            gridScale = 25f;
            gridLineWidth = 6f;
            gridSizeText = "25 ft";
        } else if (cameraHeight <= 5000f && cameraHeight > 4000f) {
            //50 feet
            gridScale = 50f;
            gridLineWidth = 6f;
            gridSizeText = "50 ft";
        } else if (cameraHeight <= 6000f && cameraHeight > 5000f) {
            //100 feet
            gridScale = 100f;
            gridLineWidth = 6f;
            gridSizeText = "100 ft";
        } else if (cameraHeight <= 7000f && cameraHeight > 6000f) {
            //250 feet
            gridScale = 250f;
            gridLineWidth = 6f;
            gridSizeText = "250 ft";
        }  else if (cameraHeight <= 8000f && cameraHeight > 7000f) {
            //500 feet
            gridScale = 500f;
            gridLineWidth = 12f;
            gridSizeText = "500 ft";
        }   else if (cameraHeight <= 9000f && cameraHeight > 8000f) {
            //1320 feet
            gridScale = 1320f;
            gridLineWidth = 18f;
            gridSizeText = "1/4 mile";
        } else {
            //2640 feet
            gridScale = 2640f;
            gridLineWidth = 36f;
            gridSizeText = "1/2 mile";
        }
        Log.d(TAG, "gridScale: "+gridScale+" gridLineWidth: "+gridLineWidth+" gridSizeText: "+gridSizeText);
        GLES20.glLineWidth(gridLineWidth);
        return createGrid3D(gridHalfSize, GRID_STEP);
    }


    public void updateGridHalfSizeOld(float halfSize) {
        gridHalfSize = Math.max(GRID_HALF_SIZE, halfSize * 1.1f);
        float[] quadVertices = new float[]{
                -gridHalfSize, 0f, -gridHalfSize,
                gridHalfSize, 0f, -gridHalfSize,
                -gridHalfSize, 0f, gridHalfSize,
                gridHalfSize, 0f, gridHalfSize
        };
        gridBackgroundBuffer = buildFloatBuffer(quadVertices);
        float[] gridVertices = createGrid3D(gridHalfSize, GRID_STEP);
        gridVertexBuffer = buildFloatBuffer(gridVertices);
    }

    private void updateBounds(float[] vertices) {
        if (vertices == null || vertices.length < COORDS_PER_VERTEX) {
            pathBounds = null;
            return;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += COORDS_PER_VERTEX) {
            float x = vertices[i];
            float z = vertices[i + 2];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        pathBounds = new Bounds(minX, maxX, minZ, maxZ);
    }
    private void rebuildGridBuffers() {
        float[] quadVertices = new float[]{
                -gridHalfSize, 0f, -gridHalfSize,
                gridHalfSize, 0f, -gridHalfSize,
                -gridHalfSize, 0f, gridHalfSize,
                gridHalfSize, 0f, gridHalfSize
        };
        gridBackgroundBuffer = buildFloatBuffer(quadVertices);
        float[] gridVertices = createGrid3D(gridHalfSize, gridStep);
        gridVertexBuffer = buildFloatBuffer(gridVertices);
    }
    private static class Bounds {
        private final float minX;
        private final float maxX;
        private final float minZ;
        private final float maxZ;

        Bounds(float minX, float maxX, float minZ, float maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }
    public void handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                previousX = event.getX();
                previousY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isDragging) {
                    break;
                }
                float deltaX = event.getX() - previousX;
                float deltaY = event.getY() - previousY;

                float yawRad = (float) Math.toRadians(yaw);
                float rightX = (float) Math.cos(yawRad);
                float rightZ = (float) -Math.sin(yawRad);
                float forwardX = (float) Math.sin(yawRad);
                float forwardZ = (float) Math.cos(yawRad);
                panSensitivity = ((cameraBehind-minCameraBehind)/(maxCameraBehind*minCameraBehind))*(maxPanSensitivity-minPanSensitivity)+minPanSensitivity;
               // panSensitivity = panSensitivity*(cameraBehind/100);
                float panX = (deltaX * rightX + deltaY * forwardX) * panSensitivity;
                float panZ = (deltaX * rightZ + deltaY * forwardZ) * panSensitivity;

                vehicleX -= panX;
                vehicleZ -= panZ;

                previousX = event.getX();
                previousY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;

        }
    }
    private String loadShaderSource(int resourceId) {
        InputStream is = context.getResources().openRawResource(resourceId);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
    // Load shader from file method
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Check for compile errors
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e("Shader", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    // Create program method
    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES20.glCreateProgram(); // Create a new program
        GLES20.glAttachShader(program, vertexShader); // Attach the vertex shader
        GLES20.glAttachShader(program, fragmentShader); // Attach the fragment shader
        GLES20.glLinkProgram(program); // Link the program

        // Check for linking errors
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("OpenGL", "Error linking program: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0; // Return 0 to indicate failure
        }
        return program; // Return the linked program
    }
}
