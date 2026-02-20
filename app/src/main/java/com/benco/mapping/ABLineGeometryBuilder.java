package com.benco.mapping;

import android.util.Log;

import androidx.annotation.Nullable;

import com.benco.mapping.data.ApplicationsData;

import java.util.ArrayList;
import java.util.List;

public class ABLineGeometryBuilder {
    private static final float DEFAULT_METERS_PER_PIXEL = 0.0254f;
    private static final int DEFAULT_GUIDE_LINE_COUNT = 6;
    private static final int DEFAULT_SAMPLES_PER_SEGMENT = 10;
    private static final float DEFAULT_LINE_Y = 4f;
    private float abLineOffset = 0;
    private float abLineYaw = 0;
    private String TAG = "ABLineGeometryBuilder";
    public ABLineGeometry build(@Nullable List<ApplicationsData> applications,
                                @Nullable List<SectionStyle> sectionStyles, float _abLineOffset, float _abLineYaw) {
        abLineOffset = _abLineOffset;;
        abLineYaw = _abLineYaw;
        if (applications == null || applications.size() < 2) {
            return new ABLineGeometry(new float[0], new ArrayList<>(), new float[0]);
        }
        int startIndex = findALineIndex(applications);
        int endIndex = findBLineIndex(applications);
        if (startIndex < 0 || endIndex < 0 || startIndex == endIndex) {
            return new ABLineGeometry(new float[0], new ArrayList<>(), new float[0]);
        }
        if (startIndex > endIndex) {
            int swap = startIndex;
            startIndex = endIndex;
            endIndex = swap;
        }
        ApplicationsData start = applications.get(startIndex);
        ApplicationsData end = applications.get(endIndex);
        if (start.lat == 0 || start.lng == 0 || end.lat == 0 || end.lng == 0) {
            return new ABLineGeometry(new float[0], new ArrayList<>(), new float[0]);
        }
        double centerLat = end.lat;
        double centerLng = end.lng;
        Point aPoint = XY(centerLat, centerLng, start.lat, start.lng, DEFAULT_METERS_PER_PIXEL);
        Point bPoint = XY(centerLat, centerLng, end.lat, end.lng, DEFAULT_METERS_PER_PIXEL);
        /*float[] abLineVertices = new float[]{
                aPoint.x, DEFAULT_LINE_Y, aPoint.y,
                bPoint.x, DEFAULT_LINE_Y, bPoint.y
        };*/

        float[] offsetPoints = offsetAlongNormal(aPoint, bPoint, abLineOffset);
        float[] rotatedPoints = rotateLineAroundMidpoint(
                offsetPoints[0],
                offsetPoints[1],
                offsetPoints[2],
                offsetPoints[3],
                abLineYaw
        );
        aPoint.x = rotatedPoints[0];
        aPoint.y = rotatedPoints[1];
        bPoint.x = rotatedPoints[2];
        bPoint.y = rotatedPoints[3];
        float[] abLineVertices = new float[]{
                aPoint.x, DEFAULT_LINE_Y, aPoint.y,
                bPoint.x, DEFAULT_LINE_Y, bPoint.y
        };
        float spacing = guideLineSpacing(sectionStyles);
        List<float[]> guideLines = buildGuideLines(aPoint, bPoint, spacing);
        float[] steeringVerticesMain = buildSteeringVertices(applications, startIndex, endIndex, centerLat,
                centerLng, aPoint, bPoint, spacing, -1f);
        float[] steeringVerticesOpposite = buildSteeringVertices(applications, startIndex, endIndex, centerLat,
                centerLng, bPoint, aPoint, spacing, -1f);
        // 1. Determine the length of the new array
        int combinedLength = steeringVerticesMain.length + steeringVerticesOpposite.length;
        // 2. Create a new array with the combined length
        float[] steeringVertices = new float[combinedLength];
        // 3. Copy elements from the first array
        System.arraycopy(steeringVerticesMain, 0, steeringVertices, 0, steeringVerticesMain.length);

        // 4. Copy elements from the second array, starting after the first array's elements
        System.arraycopy(steeringVerticesOpposite, 0, steeringVertices, steeringVerticesMain.length, steeringVerticesOpposite.length);
        return new ABLineGeometry(abLineVertices, guideLines, steeringVertices);
    }
    private int findALineIndex(List<ApplicationsData> applications) {
        for (int i = 0; i < applications.size(); i++) {
            ApplicationsData data = applications.get(i);
            if ("1".equals(data.aLine)) {
                return i;
            }
        }
        return -1;
    }

    private int findBLineIndex(List<ApplicationsData> applications) {
        for (int i = 0; i < applications.size(); i++) {
            ApplicationsData data = applications.get(i);
            if ("1".equals(data.bLine)) {
                return i;
            }
        }
        return -1;
    }

    private float guideLineSpacing(List<SectionStyle> sectionStyles) {
        if (sectionStyles == null || sectionStyles.isEmpty()) {
            return 300f;
        }
        int count = Math.min(sectionStyles.size(), 16);
        float totalWidth = 0f;
        for (int i = 0; i < count; i++) {
            totalWidth += sectionStyles.get(i).getWidth();
        }
        if (totalWidth <= 0f) {
            return 300f;
        }
        return totalWidth;
    }

    private List<float[]> buildGuideLines(Point aPoint, Point bPoint, float spacing) {
        List<float[]> guideLines = new ArrayList<>();
        float dx = bPoint.x - aPoint.x;
        float dy = bPoint.y - aPoint.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f) {
            return guideLines;
        }
        float unitX = dx / length;
        float unitY = dy / length;
        float normalX = -unitY;
        float normalY = unitX;

        int guideCount = DEFAULT_GUIDE_LINE_COUNT;
        for (int i = 1; i <= guideCount; i++) {
            float offset = spacing * i;
            guideLines.add(offsetLine(aPoint, bPoint, normalX, normalY, offset));
            guideLines.add(offsetLine(aPoint, bPoint, normalX, normalY, -offset));
        }
        return guideLines;
    }
    private float[] offsetAlongNormal(Point aPoint, Point bPoint, float offset) {
        float dx = bPoint.x - aPoint.x;
        float dy = bPoint.y - aPoint.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length == 0f) {
            return new float[]{aPoint.x, aPoint.y, bPoint.x, bPoint.y};
        }
        float unitX = -dx / length;
        float unitY = -dy / length;
        float normalX = -unitY;
        float normalY = unitX;
        float ax = aPoint.x + normalX * offset;
        float ay = aPoint.y + normalY * offset;
        float bx = bPoint.x + normalX * offset;
        float by = bPoint.y + normalY * offset;
        return new float[]{ax, ay, bx, by};
    }
    private float[] rotateLineAroundMidpoint(float ax, float ay, float bx, float by, float yawDegrees) {
        if (yawDegrees == 0f) {
            return new float[]{ax, ay, bx, by};
        }
        float midX = (ax + bx) / 2f;
        float midY = (ay + by) / 2f;
        double radians = Math.toRadians(yawDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float relAx = ax - midX;
        float relAy = ay - midY;
        float relBx = bx - midX;
        float relBy = by - midY;
        float rotAx = relAx * cos - relAy * sin;
        float rotAy = relAx * sin + relAy * cos;
        float rotBx = relBx * cos - relBy * sin;
        float rotBy = relBx * sin + relBy * cos;
        return new float[]{midX + rotAx, midY + rotAy, midX + rotBx, midY + rotBy};
    }
    private float[] buildSteeringVertices(List<ApplicationsData> applications, int startIndex, int endIndex,
                                          double centerLat, double centerLng, Point aPoint, Point bPoint,
                                          float spacing, float dxyMod) {
        List<float[]> guideLines = new ArrayList<>();
        List<GuideSample> samples = new ArrayList<>();
        float dx = bPoint.x - aPoint.x;
        float dy = bPoint.y - aPoint.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float unitX = dxyMod*dx / length;
        float unitY = dxyMod*dy / length;
        float normalX = -unitY;
        float normalY = unitX;

        float safeSpacing = spacing <= 0f ? 1f : spacing;
        int guideCount = DEFAULT_GUIDE_LINE_COUNT;
        /*for (int i = 1; i <= guideCount; i++) {
            float offset = spacing * i;
            Point snappedPoint = new Point();
            snappedPoint.x = aPoint.x + unitX + normalX * offset;
            snappedPoint.y = aPoint.y + unitY + normalY * offset;
            snappedPoint.z = 1f;
            int clampedIndex = Math.max(-DEFAULT_GUIDE_LINE_COUNT, Math.min(DEFAULT_GUIDE_LINE_COUNT, i));
            samples.add(new GuideSample(snappedPoint, clampedIndex, spacing));
        }*/
        for (int i = guideCount; i >= -guideCount; i--) {
            //if ((i & 1) != 0) continue; // Skip every other row...
            float offset = spacing * i;
            Point snappedPoint = new Point();
            snappedPoint.x = aPoint.x + unitX + normalX * offset;
            snappedPoint.y = aPoint.y + unitY + normalY * offset;
            snappedPoint.z = 1f;
            int clampedIndex = Math.max(-DEFAULT_GUIDE_LINE_COUNT, Math.min(DEFAULT_GUIDE_LINE_COUNT, i));
            samples.add(new GuideSample(snappedPoint, clampedIndex, spacing));
        }
        if (samples.size() < 2) {
            return new float[0];
        }
        List<Float> vertices = new ArrayList<>();
        GuideSample first = samples.get(0);
        addVertex(vertices, first.point);
        for (int i = 1; i < samples.size(); i++) {
            GuideSample prev = samples.get(i - 1);
            GuideSample curr = samples.get(i);
            if (prev.guideIndex == curr.guideIndex) {
                addVertex(vertices, curr.point);
                continue;
            }
            float deltaOffset = (curr.guideIndex - prev.guideIndex) * safeSpacing;
            float midAlong = (prev.along + curr.along) / 2f;
            float midOffset = (prev.guideIndex + curr.guideIndex) * .5f * safeSpacing;
            float midX = aPoint.x + unitX * midAlong + normalX * midOffset;
            float midY = aPoint.y + unitY * midAlong + normalY * midOffset;
            float c1x = (prev.point.x + midX) / 2f;
            float c1y = (prev.point.y + midY) / 2f;
            float c2x = (midX + curr.point.x) / 2f;
            float c2y = (midY + curr.point.y) / 2f;
            sampleCubic(
                    prev.point.x,
                    prev.point.y,
                    c1x,
                    c1y,
                    c2x,
                    c2y,
                    curr.point.x,
                    curr.point.y,
                    DEFAULT_SAMPLES_PER_SEGMENT,
                    vertices,
                    true
            );
        }
        float[] out = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            out[i] = vertices.get(i);
        }
        return out;
    }

    private void sampleCubic(
            float x0,
            float y0,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3,
            int samples,
            List<Float> outVertices,
            boolean skipFirst
    ) {
        int start = skipFirst ? 1 : 0;
        for (int i = start; i <= samples; i++) {
            float t = i / (float) samples;
            float u = 1f - t;

            float tt = t * t;
            float uu = u * u;
            float uuu = uu * u;
            float ttt = tt * t;

            float x = (uuu * x0)
                    + (3 * uu * t * x1)
                    + (3 * u * tt * x2)
                    + (ttt * x3);

            float y = (uuu * y0)
                    + (3 * uu * t * y1)
                    + (3 * u * tt * y2)
                    + (ttt * y3);
            outVertices.add(x);
            outVertices.add(DEFAULT_LINE_Y);
            outVertices.add(y);
        }
    }

    private void addVertex(List<Float> vertices, Point point) {
        vertices.add(point.x);
        vertices.add(DEFAULT_LINE_Y);
        vertices.add(point.y);
    }

    private static class GuideSample {
        private final Point point;
        private final int guideIndex;
        private final float along;

        private GuideSample(Point point, int guideIndex, float along) {
            this.point = point;
            this.guideIndex = guideIndex;
            this.along = along;
        }
    }

    private float[] offsetLine(Point aPoint, Point bPoint, float normalX, float normalY, float offset) {
        float ax = aPoint.x + normalX * offset;
        float ay = aPoint.y + normalY * offset;
        float bx = bPoint.x + normalX * offset;
        float by = bPoint.y + normalY * offset;
        return new float[]{ax, DEFAULT_LINE_Y, ay, bx, DEFAULT_LINE_Y, by};
    }

    private Point XY(double centerLatitude, double centerLongitude, double latitude, double longitude,
                     double metersPerPixel) {
        double rto = 1 / metersPerPixel;
        double dLat = ((centerLatitude - latitude) / 0.00001) * rto;
        double dLng = -1 * ((centerLongitude - longitude) / 0.00001) * rto;
        int y = (int) Math.round(dLat);
        int x = (int) Math.round(dLng);
        Point crd = new Point();
        crd.x = x;
        crd.y = y;
        return crd;
    }
}