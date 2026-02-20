package com.benco.mapping;

import android.util.Log;

import com.benco.mapping.data.ApplicationsData;

import java.util.ArrayList;
import java.util.List;

public class PathGeometryBuilder {
    private static final String TAG = "PathGeometryBuilder";
    private static final double EARTH_RADIUS = 6371;
    private static final int SMOOTH_VAL = 3;
    private static final int MIN_MOVEMENT_IN_INCHES = 30;
    //private static final float DEFAULT_METERS_PER_PIXEL = 0.3048f; //one foot per pixel
    private static final float DEFAULT_METERS_PER_PIXEL = 0.0254f; //one inch per pixel
    private static final int DEFAULT_SAMPLES_PER_SEGMENT = 12;
    List<Point> absPoints;

    public PathGeometry build(List<ApplicationsData> applications, List<SectionStyle> sectionStyles) {
        if (applications == null || applications.size() < 2) {
            return new PathGeometry(new float[0], new ArrayList<>(), new ArrayList<>());
        }

        absPoints = buildAbsolutePoints(applications, DEFAULT_METERS_PER_PIXEL);
        if (absPoints.size() < 2) {
            return new PathGeometry(new float[0], new ArrayList<>(), new ArrayList<>());
        }

        List<List<Point>> parallelPoints = buildParallelPoints(absPoints, sectionStyles);
        normalizeTangents(absPoints);
        for (List<Point> line : parallelPoints) {
            normalizeTangents(line);
        }

        float[] mainVertices = buildBezierStrip(absPoints, DEFAULT_SAMPLES_PER_SEGMENT);
        List<float[]> leftVertices = new ArrayList<>();
        List<float[]> rightVertices = new ArrayList<>();
        int half = parallelPoints.size() / 2;
        for (int i = 0; i < parallelPoints.size(); i++) {
            float[] vertices = buildBezierStrip(parallelPoints.get(i), DEFAULT_SAMPLES_PER_SEGMENT);
            if (i < half) {
                leftVertices.add(vertices);
            } else {
                rightVertices.add(vertices);
            }
        }

        return new PathGeometry(mainVertices, leftVertices, rightVertices);
    }

    public List<Point> getAbsPoints() {
        return absPoints;
    }
    private List<Point> buildAbsolutePoints(List<ApplicationsData> applications, float metersPerPixel) {
        ApplicationsData firstApp = applications.get(0);
        ApplicationsData lastApp = applications.get(applications.size() - 1);
        double centerLat = lastApp.lat;
        double centerLng = lastApp.lng;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        List<Point> zeroPoints = new ArrayList<>();

        for (ApplicationsData appData : applications) {
            if (appData.speed == 0) {
                continue;
            }
            if (appData.lat == 0 || appData.lng == 0) {
                continue;
            }
            Point zpoint = XY(centerLat, centerLng, appData.lat, appData.lng, metersPerPixel, 0, 0);
            zeroPoints.add(zpoint);
            minX = Math.min(minX, zpoint.x);
            minY = Math.min(minY, zpoint.y);
            maxX = Math.max(maxX, zpoint.x);
            maxY = Math.max(maxY, zpoint.y);
        }

        if (zeroPoints.isEmpty()) {
            return new ArrayList<>();
        }

        float xOffset = minX < 0 ? Math.abs(minX) : -minX;
        float yOffset = minY < 0 ? Math.abs(minY) : -minY;

        List<Point> absPoints = new ArrayList<>();
        ApplicationsData lastPoint = new ApplicationsData();
        lastPoint.lat = 0d;
        lastPoint.lng = 0d;

        for (ApplicationsData appData : applications) {
            if (appData.speed == 0) {
                continue;
            }
            if (appData.lat == 0 || appData.lng == 0) {
                continue;
            }
            Point zpoint = XY(centerLat, centerLng, appData.lat, appData.lng, metersPerPixel, 0, 0);
            if (lastPoint.lat != appData.lat && lastPoint.lng != appData.lng) {
                Point absPoint = new Point();
                if (lastPoint.lat == 0 && lastPoint.lng == 0) {
                    absPoint.distance = distance(centerLat, centerLng, appData.lat, appData.lng) * 39370.1d;
                } else {
                    absPoint.distance = distance(lastPoint.lat, lastPoint.lng, appData.lat, appData.lng) * 39370.1d;
                }
                if (absPoint.distance > MIN_MOVEMENT_IN_INCHES) {
                    absPoint.x = zpoint.x + xOffset;
                    absPoint.y = zpoint.y + yOffset;
                    absPoint.z = 1f;
                    absPoint.bearing = appData.bearing;
                    absPoints.add(absPoint);
                    lastPoint = appData;
                }
            }
        }

        return absPoints;
    }

    private List<List<Point>> buildParallelPoints(List<Point> absPoints, List<SectionStyle> sectionStyles) {
        List<List<Point>> parallelPoints = new ArrayList<>();
        int sectionCount = Math.min(sectionStyles.size(), 16);
        float totalWidth = 0f;
        for (int s = 0; s < sectionCount; s++) {
            SectionStyle style = sectionStyles.get(s);
            float width = style.getWidth();
            totalWidth += width;
        }
        //Log.d("PathGeometryBuilder", "totalWidth:"+totalWidth);
        for (int i = 0; i < sectionCount * 2; i++) {
            parallelPoints.add(new ArrayList<>());
        }

        for (int i = 0; i < absPoints.size(); i++) {
            Point point = absPoints.get(i);
            double degrees;
            if (i == 0) {
                Point next = absPoints.get(i + 1);
                degrees = Math.toDegrees(Math.atan2(next.y - point.y, next.x - point.x));
            } else {
                Point prev = absPoints.get(i - 1);
                degrees = Math.toDegrees(Math.atan2(point.y - prev.y, point.x - prev.x));
            }
            int offsetAngle = (int) degrees + 90;
            float leftOffset = -totalWidth/2f;
            float rightOffset = -totalWidth/2f;
            for (int s = 0; s < sectionCount; s++) {
                SectionStyle style = sectionStyles.get(s);
                float width = style.getWidth();
                float leftCenter = leftOffset + width / 2f;
                float rightCenter = rightOffset + width / 2f;

                double[] leftPoints = rotatePointAboutPoint(point.x, point.y, leftCenter, offsetAngle);
                Point leftPoint = new Point();
                leftPoint.x = (float) leftPoints[2];
                leftPoint.y = (float) leftPoints[3];
                leftPoint.z = (float) s;
                parallelPoints.get(s).add(leftPoint);

                double[] rightPoints = rotatePointAboutPoint(point.x, point.y, rightCenter, offsetAngle);
                Point rightPoint = new Point();
                rightPoint.x = (float) rightPoints[0];
                rightPoint.y = (float) rightPoints[1];
                rightPoint.z = (float) s;
                parallelPoints.get(sectionCount + s).add(rightPoint);

                leftOffset += width;
                rightOffset += width;
            }
        }

        return parallelPoints;
    }

    private void normalizeTangents(List<Point> points) {
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            if (i == 0) {
                Point next = points.get(i + 1);
                point.dx = (next.x - point.x) / SMOOTH_VAL;
                point.dy = (next.y - point.y) / SMOOTH_VAL;
            } else if (i == points.size() - 1) {
                Point prev = points.get(i - 1);
                point.dx = (point.x - prev.x) / SMOOTH_VAL;
                point.dy = (point.y - prev.y) / SMOOTH_VAL;
            } else {
                Point next = points.get(i + 1);
                Point prev = points.get(i - 1);
                point.dx = (next.x - prev.x) / SMOOTH_VAL;
                point.dy = (next.y - prev.y) / SMOOTH_VAL;
            }
            points.set(i, point);
        }
    }

    private float[] buildBezierStrip(List<Point> points, int samples) {
        if (points.size() < 2) {
            return new float[0];
        }
        List<Float> vertices = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            Point prev = points.get(i - 1);
            Point point = points.get(i);
            sampleCubic(
                    prev.x,
                    prev.y,
                    prev.x + prev.dx,
                    prev.y + prev.dy,
                    point.x - point.dx,
                    point.y - point.dy,
                    point.x,
                    point.y,
                    point.z,
                    samples,
                    vertices
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
            float z,
            int samples,
            List<Float> outVertices
    ) {
        for (int i = 0; i <= samples; i++) {
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
            //Log.d(TAG, "z:"+z+1f);
            outVertices.add(x);
            outVertices.add(0f);
            outVertices.add(y);
        }
    }

    private Point XY(double centerLatitude, double centerLongitude, double latitude, double longitude,
                     double metersPerPixel, float windowWidth, float windowHeight) {
        double rto = 1 / metersPerPixel;
        double dLat = ((centerLatitude - latitude) / 0.00001) * rto;
        double dLng = -1 * ((centerLongitude - longitude) / 0.00001) * rto;
        int y = (int) Math.round(dLat);
        int x = (int) Math.round(dLng);
        Point crd = new Point();
        crd.x = x + (windowWidth / 2f);
        crd.y = y + (windowHeight / 2f);
        return crd;
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private double[] rotatePointAboutPoint(float x1, float y1, float shift, int angle) {
        double angleRad = Math.toRadians(angle);
        double cosAngle = Math.cos(angleRad);
        double sinAngle = Math.sin(angleRad);

        float x2 = x1 + shift;
        float y2 = y1;
        x2 -= x1;
        y2 -= y1;

        double rightX = x2 * cosAngle - y2 * sinAngle;
        double rightY = x2 * sinAngle + y2 * cosAngle;

        rightX += x1;
        rightY += y1;

        float x3 = x1 - shift;
        float y3 = y1;
        x3 -= x1;
        y3 -= y1;

        double leftX = x3 * cosAngle - y3 * sinAngle;
        double leftY = x3 * sinAngle + y3 * cosAngle;

        leftX += x1;
        leftY += y1;

        return new double[]{rightX, rightY, leftX, leftY};
    }
}
