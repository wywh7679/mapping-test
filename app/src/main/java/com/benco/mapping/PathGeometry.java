package com.benco.mapping;

public class PathGeometry {
    private final float[] mainVertices;
    private final java.util.List<float[]> leftVertices;
    private final java.util.List<float[]> rightVertices;

    public PathGeometry(float[] mainVertices, java.util.List<float[]> leftVertices,
                        java.util.List<float[]> rightVertices) {
        this.mainVertices = mainVertices;
        this.leftVertices = leftVertices;
        this.rightVertices = rightVertices;
    }

    public float[] getMainVertices() {
        return mainVertices;
    }

    public java.util.List<float[]> getLeftVertices() {
        return leftVertices;
    }

    public java.util.List<float[]> getRightVertices() {
        return rightVertices;
    }
}
