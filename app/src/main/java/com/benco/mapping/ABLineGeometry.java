package com.benco.mapping;

import java.util.List;

public class ABLineGeometry {
    private final float[] abLineVertices;
    private final List<float[]> guideLineVertices;
    private final float[] steeringVertices;

    public ABLineGeometry(float[] abLineVertices, List<float[]> guideLineVertices, float[] steeringVertices) {
        this.abLineVertices = abLineVertices;
        this.guideLineVertices = guideLineVertices;
        this.steeringVertices = steeringVertices;

    }

    public float[] getAbLineVertices() {
        return abLineVertices;
    }

    public List<float[]> getGuideLineVertices() {
        return guideLineVertices;
    }
    public float[] getSteeringVertices() {
        return steeringVertices;
    }
}
