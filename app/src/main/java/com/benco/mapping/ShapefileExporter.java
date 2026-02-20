package com.benco.mapping;

import com.benco.mapping.data.ApplicationsData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ShapefileExporter {
    private static final int SHAPE_TYPE_POLYLINE = 3;

    private ShapefileExporter() {
    }

    public static void exportPolylineZip(ZipOutputStream zipOutputStream, String baseName, int aid,
                                         List<ApplicationsData> points) throws IOException {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 GPS points to export a polyline shapefile.");
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (ApplicationsData point : points) {
            double x = point.lng;
            double y = point.lat;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        byte[] shp = buildShp(points, minX, minY, maxX, maxY);
        byte[] shx = buildShx(points, minX, minY, maxX, maxY);
        byte[] dbf = buildDbf(aid, points.size());
        byte[] prj = buildPrj();

        writeZipEntry(zipOutputStream, baseName + ".shp", shp);
        writeZipEntry(zipOutputStream, baseName + ".shx", shx);
        writeZipEntry(zipOutputStream, baseName + ".dbf", dbf);
        writeZipEntry(zipOutputStream, baseName + ".prj", prj);
    }

    private static void writeZipEntry(ZipOutputStream zipOutputStream, String name, byte[] data) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(data);
        zipOutputStream.closeEntry();
    }

    private static byte[] buildShp(List<ApplicationsData> points, double minX, double minY, double maxX, double maxY) {
        int numPoints = points.size();
        int contentBytes = 48 + (16 * numPoints);
        int fileLengthWords = 50 + ((8 + contentBytes) / 2);

        ByteBuffer buffer = ByteBuffer.allocate(fileLengthWords * 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        writeMainHeader(buffer, fileLengthWords, minX, minY, maxX, maxY);

        buffer.putInt(1); // record number
        buffer.putInt(contentBytes / 2); // content length (16-bit words)

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(SHAPE_TYPE_POLYLINE);
        buffer.putDouble(minX);
        buffer.putDouble(minY);
        buffer.putDouble(maxX);
        buffer.putDouble(maxY);
        buffer.putInt(1); // numParts
        buffer.putInt(numPoints);
        buffer.putInt(0); // first part starts at point index 0

        for (ApplicationsData point : points) {
            buffer.putDouble(point.lng);
            buffer.putDouble(point.lat);
        }
        return buffer.array();
    }

    private static byte[] buildShx(List<ApplicationsData> points, double minX, double minY, double maxX, double maxY) {
        int numPoints = points.size();
        int contentBytes = 48 + (16 * numPoints);
        int shxLengthWords = 50 + 4; // one index record = 8 bytes = 4 words

        ByteBuffer buffer = ByteBuffer.allocate(shxLengthWords * 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        writeMainHeader(buffer, shxLengthWords, minX, minY, maxX, maxY);

        buffer.putInt(50); // offset (in 16-bit words) where first record starts in .shp
        buffer.putInt(contentBytes / 2); // content length (in 16-bit words)
        return buffer.array();
    }

    private static void writeMainHeader(ByteBuffer buffer, int fileLengthWords,
                                        double minX, double minY, double maxX, double maxY) {
        buffer.putInt(9994);
        for (int i = 0; i < 5; i++) {
            buffer.putInt(0);
        }
        buffer.putInt(fileLengthWords);

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(1000);
        buffer.putInt(SHAPE_TYPE_POLYLINE);
        buffer.putDouble(minX);
        buffer.putDouble(minY);
        buffer.putDouble(maxX);
        buffer.putDouble(maxY);
        buffer.putDouble(0); // zmin
        buffer.putDouble(0); // zmax
        buffer.putDouble(0); // mmin
        buffer.putDouble(0); // mmax
    }

    private static byte[] buildDbf(int aid, int pointCount) throws IOException {
        int fieldCount = 3;
        int headerLength = 32 + (32 * fieldCount) + 1;
        int recordLength = 1 + 10 + 10 + 19;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ByteBuffer header = ByteBuffer.allocate(32);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.put((byte) 0x03);
        Calendar now = Calendar.getInstance();
        header.put((byte) (now.get(Calendar.YEAR) - 1900));
        header.put((byte) (now.get(Calendar.MONTH) + 1));
        header.put((byte) now.get(Calendar.DAY_OF_MONTH));
        header.putInt(1); // number of records
        header.putShort((short) headerLength);
        header.putShort((short) recordLength);
        header.position(32);
        out.write(header.array());

        out.write(fieldDescriptor("AID", 'N', 10, 0));
        out.write(fieldDescriptor("POINTS", 'N', 10, 0));
        out.write(fieldDescriptor("EXPORT_TS", 'C', 19, 0));
        out.write(0x0D);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new java.util.Date());

        ByteBuffer record = ByteBuffer.allocate(recordLength);
        record.put((byte) 0x20);
        putFixed(record, String.format("%10d", aid), 10);
        putFixed(record, String.format("%10d", pointCount), 10);
        putFixed(record, String.format("%-19s", timestamp), 19);
        out.write(record.array());
        out.write(0x1A);

        return out.toByteArray();
    }

    private static byte[] fieldDescriptor(String name, char type, int length, int decimalCount) {
        ByteBuffer descriptor = ByteBuffer.allocate(32);
        byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
        int nameLength = Math.min(nameBytes.length, 11);
        descriptor.put(nameBytes, 0, nameLength);
        descriptor.position(11);
        descriptor.put((byte) type);
        descriptor.position(16);
        descriptor.put((byte) length);
        descriptor.put((byte) decimalCount);
        return descriptor.array();
    }

    private static void putFixed(ByteBuffer record, String value, int length) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        int copyLen = Math.min(bytes.length, length);
        record.put(bytes, 0, copyLen);
        for (int i = copyLen; i < length; i++) {
            record.put((byte) ' ');
        }
    }

    private static byte[] buildPrj() {
        String wgs84 = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563]]," +
                "PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]]";
        return wgs84.getBytes(StandardCharsets.UTF_8);
    }
}
