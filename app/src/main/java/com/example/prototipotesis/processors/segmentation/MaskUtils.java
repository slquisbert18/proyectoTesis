package com.example.prototipotesis.processors.segmentation;

import android.util.Log;

public class MaskUtils {
    // tamaño máscara
    private static final int MASK_SIZE = 160;

    // cantidad protos
    private static final int PROTO_CHANNELS = 32;

    // umbral de confianza minimo
    private static final float MIN_CONFIDENCE = 0.70f;

    /*
    * OBTENER MEJOR DETECCION
    * */
    public static int obtenerMejorDeteccion(
            SegmentationResult result
    ) {

        int best = -1;
        float bestConf = 0f;
        for (int i = 0; i < 8400; i++) {
            float conf =
                    result.detections[0][4][i];
            if (conf > bestConf && conf > MIN_CONFIDENCE) {
                bestConf = conf;
                best = i;
            }
        }

        Log.d("SEGMENTATION", "bestDetection=" + best + " conf=" + bestConf);

        return best;
    }

    // crear mascara final
    public static float[][] crearMascaraFinal(
            SegmentationResult result,
            int detectionIndex) {

        float[][] mask = new float[MASK_SIZE][MASK_SIZE];

        // recorrer máscara
        for (int y = 0; y < MASK_SIZE; y++) {
            for (int x = 0; x < MASK_SIZE; x++) {
                float sum = 0f;
                // combinar protos
                for (int c = 0; c < PROTO_CHANNELS; c++) {
                    float proto = result.protos[0][y][x][c];
                    float coef = result.detections[0][5 + c][detectionIndex];
                    sum += proto * coef;
                }

                // sigmoid
                mask[y][x] = (float)(1f / (1f + Math.exp(-sum)));
            }
        }
        return mask;
    }
}
