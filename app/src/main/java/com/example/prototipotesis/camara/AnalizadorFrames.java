package com.example.prototipotesis.camara;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;
import android.content.Context;


import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.prototipotesis.detectors.VehicleDetector;
import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.ml.VehicleTracker;
import com.example.prototipotesis.processors.PlateProcessor;
import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.trackedObject.TrackedPlate;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.yuv2rgb;
import java.util.List;

public class AnalizadorFrames implements ImageAnalysis.Analyzer {
    //**************************************************
    // fragmento para recibir el PreviewView del MainActivity
    public interface EscucharDeteccion{
        void alDetectarRectangulo(List<TrackedVehicle> vehicles);
    }
    private EscucharDeteccion escuchador;
    //**************************************************
    private boolean procesando = false; // Evita procesar varios frames al mismo tiempo
    private int anchoPreview;
    private int altoPreview;
    private yuv2rgb conversor;

    private VehicleProcessor vehicleProcessor;

    // limitamos los fps para no saturar CPU
    private long ultimoTiempoProcesado = 0;
    private static final long INTERVALO_PROCESAMIENTO = 30; //ms (30=33 fps aprox)
    public AnalizadorFrames(
            Context context,
            VehicleProcessor vehicleProcessor,
            EscucharDeteccion escuchador,
            int anchoPreview,
            int altoPreview
    ){
        conversor = new yuv2rgb(context);
        this.vehicleProcessor = vehicleProcessor;
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

            // procesamiento de vehiculos
            List<TrackedVehicle> vehicles =
                    vehicleProcessor.processFrame(
                            bitmapRotado,
                            anchoPreview,
                            altoPreview
            );

            // enviamos los resultados al mainActivity
            if(escuchador != null){
                escuchador.alDetectarRectangulo(vehicles);
            }

        } catch (Exception e) {
            Log.e("ANALIZADOR", "Error procesando frame", e);
        } finally {
            procesando = false;
            imageProxy.close();
        }
    }
}
