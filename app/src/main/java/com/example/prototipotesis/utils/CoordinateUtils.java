package com.example.prototipotesis.utils;

import android.graphics.RectF;

import com.example.prototipotesis.ml.BoundingBox;

public class CoordinateUtils {
    public static RectF coordinates2preview(
            BoundingBox caja,
            int anchoOriginal,
            int altoOriginal,
            int anchoPreview,
            int altoPreview
    ){
        // coordenadas normalizadas del modelo (0 a 1)
        float centroX = caja.centroX * anchoOriginal;
        float centroY = caja.centroY * altoOriginal;
        float anchoCaja = caja.ancho * anchoOriginal;
        float altoCaja = caja.alto * altoOriginal;

        // convertimos a coordenadas absolutas del bitmap
        float izquierda = centroX - (anchoCaja / 2f);
        float arriba = centroY - (altoCaja / 2f);
        float derecha = centroX + (anchoCaja / 2f);
        float abajo = centroY + (altoCaja / 2f);

        // ahora escalamos al tamaño real del PreviewView
        float escalaX = (float) anchoPreview / (float) anchoOriginal;
        float escalaY = (float) altoPreview / (float) altoOriginal;

        izquierda *= escalaX;
        derecha *= escalaX;
        arriba *= escalaY;
        abajo *= escalaY;

        return new RectF(izquierda, arriba, derecha, abajo);
    }
}
