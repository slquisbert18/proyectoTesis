package com.example.prototipotesis.camara;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.Image;
import android.util.Log;
import android.content.Context;


import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.prototipotesis.processors.SegmentationProcessor;
import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.processors.segmentation.MaskContourExtractor;
import com.example.prototipotesis.processors.segmentation.MaskUtils;
import com.example.prototipotesis.processors.segmentation.SegmentationResult;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.yuv2rgb;

import java.util.ArrayList;
import java.util.List;

public class AnalizadorFrames implements ImageAnalysis.Analyzer {
    //****************************LISTENERS**********************
    // fragmento para recibir el PreviewView del MainActivity
    public interface EscucharResultados{
        void alDetectar(
                Bitmap BitmapOriginal,
                List<TrackedVehicle> vehiculos,
                List<PointF> verticesCrice
        );
    }


    private final EscucharResultados escuchador;

    //************** CONTROL DE PROCESAMIENTO ***************************
    private boolean procesando = false; // Evita procesar varios frames al mismo tiempo
    private long ultimoTiempoProcesado = 0;
    private static final long INTERVALO_PROCESAMIENTO = 100; //ms (12 fps aprox)

    // helpers
    private final yuv2rgb conversor;

    // procesadores
    private final VehicleProcessor vehicleProcessor;
    private final SegmentationProcessor segmentationProcessor;

    // reutilizable
    private final Matrix matrizRotacion = new Matrix();

    // ultimo momento en que se ejecuto segmentacion
    private long ultimoTiempoSegmentacion = 0;

    // intervalo entre segmentaciones
    private static final long INTERVALO_SEGMENTACION = 800;

    // ultimo poligono estable detectado
    private List<PointF> ultimoPoligono = new ArrayList<>();


    public AnalizadorFrames(
            Context context,
            VehicleProcessor vehicleProcessor,
            SegmentationProcessor segmentationProcessor,
            EscucharResultados escuchador
    ){
        this.vehicleProcessor = vehicleProcessor;
        this.segmentationProcessor = segmentationProcessor;
        this.escuchador = escuchador;
        this.conversor = new yuv2rgb(context);
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

            if (imagen == null) return;

            // 1. convertir a Bitmap
            Bitmap bitmapOriginal = conversor.image2Bitmap(imagen);
            if (bitmapOriginal == null) return;

            // 2. Rotar imagen
            matrizRotacion.reset();
            matrizRotacion.postRotate(imageProxy.getImageInfo().getRotationDegrees());

            Bitmap bitmapRotado = Bitmap.createBitmap(
                    bitmapOriginal,
                    0,
                    0,
                    bitmapOriginal.getWidth(),
                    bitmapOriginal.getHeight(), matrizRotacion,
                    true
            );

            // liberar el original inmediatamente después de rotar
            if (bitmapOriginal != bitmapRotado) {
                bitmapOriginal.recycle();
            }

            // 3. procesamiento de vehiculos (deteccion)
            List<TrackedVehicle> vehiculos =
                    vehicleProcessor.processFrame(
                            bitmapRotado,
                            bitmapRotado.getWidth(),
                            bitmapRotado.getHeight()
                    );

            // 4. detectar cruce peatonal
            // vertices que mandaremos al mainActivity
            List<PointF> verticesActuales = new ArrayList<>(ultimoPoligono);

            // tiempo actual
            long tiempoSegmentacion = System.currentTimeMillis();

            // ejecutar segmentacion cada cierto tiempo
            if (tiempoSegmentacion - ultimoTiempoSegmentacion >= INTERVALO_SEGMENTACION){
                // actualizar tiempo
                ultimoTiempoSegmentacion = tiempoSegmentacion;

                // ejecutamos la inferencia
                SegmentationResult result = segmentationProcessor.segment(bitmapRotado);
                int bestDetection = MaskUtils.obtenerMejorDeteccion(result);

                if(bestDetection != -1){
                    float[][] mask =
                            MaskUtils.crearMascaraFinal(result, bestDetection);

                    List<PointF> vertices = MaskContourExtractor.extraerVertices(mask);

                    // validar poligono
                    if(vertices != null && vertices.size() >= 4){
                        // guardar poligono estable
                        ultimoPoligono = vertices;

                        //usar nuevos vertices
                        verticesActuales = vertices;

                        Log.d(
                                "CRUCE",
                                "Cruce peatonal detectado"
                        );
                    }
                    else {
                        Log.d(
                                "CRUCE",
                                "Contorno inválido"
                        );
                        ultimoPoligono.clear();
                        verticesActuales.clear();
                    }
                }
                else{
                    ultimoPoligono.clear();
                    verticesActuales.clear();
                }
            }

            // 5. enviamos los resultados al mainActivity
            if(escuchador != null){
                escuchador.alDetectar(
                        bitmapRotado, vehiculos, verticesActuales
                );
            }
        } catch (Exception e) {
            Log.e("ANALIZADOR", "Error procesando frame", e);
        } finally {
            procesando = false;
            imageProxy.close();
        }
    }
}