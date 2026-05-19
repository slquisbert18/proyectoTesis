package com.example.prototipotesis.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
* clase utilitaria para operaciones sobre imagenes
* antes de pasarlas al ocr
* */
public class ImageUtils {

    // byteBuffer esla estructura de enr
    public static ByteBuffer bitmap2bytebuffer(Bitmap bitmapOriginal, int inputSize, int colorChannels){
        // conversion segura del bitmap
        Bitmap bitmapSeguro = BitmapUtils.copiarEditable(bitmapOriginal);

        // 4 bits por float
        int bytesPorCanal = 4;

        // tamanio total del buffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(
                inputSize * inputSize * colorChannels * bytesPorCanal
        );

        // usar el orden de bytes nativo del dispositivo
        buffer.order(ByteOrder.nativeOrder());

        // obtener los pixeles de la imagen en un array
        int[] pixeles = new int[inputSize * inputSize];
        bitmapSeguro.getPixels(
                pixeles,
                0,
                bitmapSeguro.getWidth(),
                0,
                0,
                bitmapSeguro.getWidth(),
                bitmapSeguro.getHeight()
        );

        int indicePixel = 0;

        // recorrer cada pixel y normalizarlo
        for(int fila = 0; fila < inputSize; fila++){
            for(int columna = 0; columna < inputSize; columna++){
                int pixel = pixeles[indicePixel++];

                // extraer componentes RGB y normalizar (0-1)
                float rojo = ((pixel >> 16) & 0xFF) / 255.0f;
                float verde = ((pixel >> 8) & 0xFF) / 255.0f;
                float azul = (pixel & 0xFF) / 255.0f;

                buffer.putFloat(rojo);
                buffer.putFloat(verde);
                buffer.putFloat(azul);
            }
        }

        return buffer;
    }

}
