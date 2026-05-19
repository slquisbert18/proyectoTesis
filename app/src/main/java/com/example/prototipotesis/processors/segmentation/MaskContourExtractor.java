package com.example.prototipotesis.processors.segmentation;

import android.graphics.PointF;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MaskContourExtractor {

    private static final float THRESHOLD = 0.95f;

    public static List<PointF> extraerVertices(
            float[][] mask
    ) {

        int rows = mask.length;
        int cols = mask[0].length;

        // imagen binaria para openCV
        Mat binary = new Mat(rows, cols, CvType.CV_8UC1);

        // convertimos la máscara float en una imagen blanco/negro
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                float value = mask[y][x];
                double pixel = (value < THRESHOLD ? 255 : 0.0);
                binary.put(y, x, pixel);
            }
        }

        // ****************************************************
        // LIMPIEZA MORFOLOGICA

        // limipieza de ruido
        Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT,
                new Size(5, 5)
        );
        // eliminar manchas pequeñas
        Imgproc.morphologyEx(
                binary,
                binary,
                Imgproc.MORPH_OPEN,
                kernel
        );

        // unir regiones cercanas
        Imgproc.morphologyEx(
                binary,
                binary,
                Imgproc.MORPH_CLOSE,
                kernel
        );
        // ****************************************************

        // buscamos contornos externos
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                binary,
                contours,
                new Mat(),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
        );

        Log.d("CONTOURS", "Encontrados: " + contours.size());

        if (contours.isEmpty()) return null;

        // buscamos el mejor contorno
        double maxArea = 0;
        MatOfPoint biggest = null;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // ignorar ruido pequeño
            if (area < 200) continue;

            if (area > maxArea) {
                maxArea = area;
                biggest = contour;
            }
        }
        if (biggest == null) return null;

        // aproximar poligono
        MatOfPoint2f contour2f = new MatOfPoint2f(biggest.toArray());

        double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);

        MatOfPoint2f approx = new MatOfPoint2f();

        Imgproc.approxPolyDP(
                contour2f,
                approx,
                epsilon,
                true
        );

        // convertir a pointF
        List<PointF> vertices = new ArrayList<>();

        for (Point p : approx.toArray()) {
            vertices.add(
                    new PointF(
                            (float) p.x,
                            (float) p.y
                    )
            );
        }

        return vertices;
    }
}