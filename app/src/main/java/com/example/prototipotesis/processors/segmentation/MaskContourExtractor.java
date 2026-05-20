package com.example.prototipotesis.processors.segmentation;

import android.graphics.PointF;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class MaskContourExtractor {

    private static final float THRESHOLD = 0.75f;

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
                double pixel = (value > THRESHOLD ? 255 : 0.0);
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
        MatOfPoint mejorContorno = null;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // ignorar ruido pequeño
            if (area < 300) continue;

            // rectangulo del contorno
            Rect rect = Imgproc.boundingRect(contour);

            // proporcion ancho/alto
            float ratio = rect.width / (float) rect.height;

            // filtros
            boolean anchoValido = rect.width > 120;
            boolean altoValido = rect.height > 50;
            boolean ratioValido = ratio > 1.2f;

            Log.d(
                    "CONTOUR_DEBUG",
                    "area=" + area +
                            " width=" + rect.width +
                            " height=" + rect.height +
                            " ratio=" + ratio
            );

            // si no cumple filtros
            if (!anchoValido || !altoValido || !ratioValido) {
                continue;
            }

            if (area > maxArea) {
                maxArea = area;
                mejorContorno = contour;
            }
        }
        if (mejorContorno == null) return null;

        // aproximar poligono
        MatOfPoint2f contour2f = new MatOfPoint2f(mejorContorno.toArray());

        // obtener rectangulo rotado minimo
        RotatedRect rect = Imgproc.minAreaRect(contour2f);

        // obtenemos 4 esquinas
        Point[] puntos = new Point[4];

        rect.points(puntos);

        // convertir a pointF
        List<PointF> vertices = new ArrayList<>();

        for (Point p : puntos) {
            vertices.add(
                    new PointF(
                            (float) p.x,
                            (float) p.y
                    )
            );
        }

        return ordenarVertices(vertices);
    }

    private static List<PointF> ordenarVertices(List<PointF> puntos) {
        List<PointF> ordenados = new ArrayList<>();
        PointF centro = new PointF();

        // calcular centro
        for (PointF p : puntos) {
            centro.x += p.x;
            centro.y += p.y;
        }

        centro.x /= puntos.size();
        centro.y /= puntos.size();

        // ordenar por angulo
        puntos.sort((a, b) -> {
            double anguloA =
                    Math.atan2(a.y - centro.y, a.x - centro.x);

            double anguloB =
                    Math.atan2(b.y - centro.y, b.x - centro.x);

            return Double.compare(anguloA, anguloB
            );
        });
        ordenados.addAll(puntos);

        return ordenados;
    }
}