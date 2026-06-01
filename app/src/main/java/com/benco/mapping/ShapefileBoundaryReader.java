package com.benco.mapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ShapefileBoundaryReader {
    private static final int SHAPE_TYPE_POLYLINE = 3;
    private static final int SHAPE_TYPE_POLYGON = 5;

    private ShapefileBoundaryReader() {
    }

    public static List<List<double[]>> readBoundaryParts(InputStream inputStream, boolean isZip) throws IOException {
        byte[] shpBytes = isZip ? extractShpFromZip(inputStream) : readAllBytes(inputStream);
        if (shpBytes == null || shpBytes.length < 100) {
            throw new IOException("Invalid shapefile content.");
        }
        return parseShp(shpBytes);
    }

    private static List<List<double[]>> parseShp(byte[] shpBytes) throws IOException {
        ByteBuffer header = ByteBuffer.wrap(shpBytes, 0, 100);
        header.order(ByteOrder.BIG_ENDIAN);
        int fileCode = header.getInt();
        if (fileCode != 9994) {
            throw new IOException("Not a valid shapefile (.shp).");
        }

        ByteBuffer littleHeader = ByteBuffer.wrap(shpBytes, 28, 72).order(ByteOrder.LITTLE_ENDIAN);
        int version = littleHeader.getInt();
        int shapeType = littleHeader.getInt();
        if (version != 1000 || (shapeType != SHAPE_TYPE_POLYGON && shapeType != SHAPE_TYPE_POLYLINE)) {
            throw new IOException("Only Polygon or Polyline shapefiles are supported.");
        }

        List<List<double[]>> parts = new ArrayList<>();
        int offset = 100;
        while (offset + 8 <= shpBytes.length) {
            ByteBuffer recHeader = ByteBuffer.wrap(shpBytes, offset, 8).order(ByteOrder.BIG_ENDIAN);
            recHeader.getInt(); // record number
            int contentLengthWords = recHeader.getInt();
            int contentBytes = contentLengthWords * 2;
            int contentStart = offset + 8;
            if (contentStart + contentBytes > shpBytes.length || contentBytes < 4) {
                break;
            }

            ByteBuffer rec = ByteBuffer.wrap(shpBytes, contentStart, contentBytes).order(ByteOrder.LITTLE_ENDIAN);
            int recType = rec.getInt();
            if (recType == 0) {
                offset = contentStart + contentBytes;
                continue;
            }
            if (recType != SHAPE_TYPE_POLYGON && recType != SHAPE_TYPE_POLYLINE) {
                offset = contentStart + contentBytes;
                continue;
            }

            rec.getDouble(); // xmin
            rec.getDouble(); // ymin
            rec.getDouble(); // xmax
            rec.getDouble(); // ymax
            int numParts = rec.getInt();
            int numPoints = rec.getInt();
            if (numParts <= 0 || numPoints <= 1) {
                offset = contentStart + contentBytes;
                continue;
            }

            int[] partStarts = new int[numParts];
            for (int i = 0; i < numParts; i++) {
                partStarts[i] = rec.getInt();
            }

            double[][] points = new double[numPoints][2];
            for (int i = 0; i < numPoints; i++) {
                points[i][0] = rec.getDouble(); // x (lng)
                points[i][1] = rec.getDouble(); // y (lat)
            }

            for (int i = 0; i < numParts; i++) {
                int start = partStarts[i];
                int end = (i == numParts - 1) ? numPoints : partStarts[i + 1];
                if (start < 0 || end > numPoints || start >= end) {
                    continue;
                }
                List<double[]> part = new ArrayList<>();
                for (int p = start; p < end; p++) {
                    part.add(new double[]{points[p][0], points[p][1]});
                }
                if (part.size() > 1) {
                    parts.add(part);
                }
            }

            offset = contentStart + contentBytes;
        }

        if (parts.isEmpty()) {
            throw new IOException("No boundary geometry found in shapefile.");
        }
        return parts;
    }

    private static byte[] extractShpFromZip(InputStream inputStream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".shp")) {
                    return readAllBytes(zipInputStream);
                }
            }
        }
        throw new IOException("ZIP does not contain a .shp file.");
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = inputStream.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }
}
