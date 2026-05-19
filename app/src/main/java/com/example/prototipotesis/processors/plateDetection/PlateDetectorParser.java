package com.example.prototipotesis.processors.plateDetection;

import com.example.prototipotesis.ml.BoundingBox;

import java.util.List;

public class PlateDetectorParser {

    private static final float MIN_AREA = 0.005f;

    public static BoundingBox seleccionarMejorPlaca(List<BoundingBox> boxes) {
        if (boxes == null || boxes.isEmpty()) return null;

        BoundingBox mejor = null;
        float mejorScore = 0f;

        for (BoundingBox box : boxes) {
            // filtrar cajas demasiado pequeñas
            float area = box.ancho * box.alto;
            if (area < MIN_AREA) {
                continue;
            }

            // score combinado: confianza + tamaño
            float score = box.confianza * area;

            if (score > mejorScore) {
                mejorScore = score;
                mejor = box;
            }
        }
        return mejor;
    }
}