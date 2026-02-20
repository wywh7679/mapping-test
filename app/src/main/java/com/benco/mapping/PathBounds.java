package com.benco.mapping;

public class PathBounds {
    public final float minX, maxX, minZ, maxZ;
    public PathBounds(float minX, float maxX, float minZ, float maxZ) {
        this.minX = minX; this.maxX = maxX; this.minZ = minZ; this.maxZ = maxZ;
    }

    public static PathBounds computeBounds(float[] vertices) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float z = vertices[i + 2];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        return new PathBounds(minX, maxX, minZ, maxZ);
    }

    public float width() { return maxX - minX; }
    public float depth() { return maxZ - minZ; }
}

