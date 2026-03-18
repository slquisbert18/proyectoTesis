package com.example.prototipotesis.camara;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.util.Log;
import android.content.Context;


import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.prototipotesis.processors.PlateProcessor;
import com.example.prototipotesis.ml.TrackedPlate;
import com.example.prototipotesis.utils.yuv2rgb;
import java.util.List;

public class AnalizadorFrames implements ImageAnalysis.Analyzer {
    //**************************************************
    // fragmento para recibir el PreviewView del MainActivity
    public interface EscucharDeteccion{
        void alDetectarRectangulo(List<TrackedPlate> placas);
    }
    private EscucharDeteccion escuchador;
    //**************************************************
    private boolean procesando = false; // Evita procesar varios frames al mismo tiempo
    private int anchoPreview;
    private int altoPreview;
    private yuv2rgb conversor;

    // nueva clase central
    private PlateProcessor procesadorPlacas;

    // limitamos los fps para no saturar CPU
    private long ultimoTiempoProcesado = 0;
    private static final long INTERVALO_PROCESAMIENTO = 30; //ms (30=33 fps aprox)
    public AnalizadorFrames(
            Context context,
            PlateProcessor procesadorPlacas,
            EscucharDeteccion escuchador,
            int anchoPreview,
            int altoPreview
    ){
        conversor = new yuv2rgb(context);
        this.procesadorPlacas = procesadorPlacas;
        this.escuchador = escuchador;
        this.anchoPreview = anchoPreview;
        this.altoPreview = altoPreview;
    }

    @ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        long tiempoActual = System.currentTimeMillis();

        //  limitamos la cantidad de frames procesados
        if ((tiempoActual - ultimoTiempoProcesado) < INTERVALO_PROCESAMIENTO) {
            imageProxy.close();
            return;
        }

        // evita procesar varios frames a la vez
        if (procesando) {
            imageProxy.close();
            return;
        }

        procesando = true;
        ultimoTiempoProcesado = tiempoActual;

        try {
            Image imagen = imageProxy.getImage();
            if (imagen == null){
                imageProxy.close();
                return;
            }

            // convertimos la imagen a bitmap
            Bitmap bitmap = conversor.convertir(imagen);

            if (bitmap == null) {
                imageProxy.close();
                return;
            };

            // rotacion segun orientacion del dispositivo
            Matrix matrizRotacion = new Matrix();
            matrizRotacion.postRotate(
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            Bitmap bitmapRotado = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrizRotacion,
                    true
            );

            // enviamos el frame al procesador central
            List<TrackedPlate> placas = procesadorPlacas.procesarFrame(bitmapRotado, anchoPreview, altoPreview);

            // enviamos los resultados al mainActivity
            if(escuchador != null){
                escuchador.alDetectarRectangulo(placas);
            }

        } catch (Exception e) {
            Log.e("ANALIZADOR", "Error procesando frame", e);
        } finally {
            procesando = false;
            imageProxy.close();
        }
    }
}
