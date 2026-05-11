package com.example.prototipotesis.camara;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;
import android.content.Context;


import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.prototipotesis.processors.CrosswalkProcessor;
import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.InfringmentZoneView;
import com.example.prototipotesis.utils.yuv2rgb;
import java.util.List;

public class AnalizadorFrames implements ImageAnalysis.Analyzer {
    //**************************************************
    // fragmento para recibir el PreviewView del MainActivity
    public interface EscucharDeteccion{
        void alDetectarRectangulo(List<TrackedVehicle> vehicles);
    }
    // listenere para enviar bitmaps renderizados
    public interface renderedFrameListener{
        void onRenderizedFrame(Bitmap bitmap);
    }

    private EscucharDeteccion escuchador;
    private renderedFrameListener listener;
    //**************************************************
    private boolean procesando = false; // Evita procesar varios frames al mismo tiempo
    private int anchoPreview;
    private int altoPreview;
    private yuv2rgb conversor;

    private VehicleProcessor vehicleProcessor;
    private CrosswalkProcessor crosswalkProcessor;
    private InfringmentZoneView viewZone;

    // limitamos los fps para no saturar CPU
    private long ultimoTiempoProcesado = 0;
    private int contadorFramesCruce = 0;
    private static final long INTERVALO_PROCESAMIENTO = 80; //ms (12 fps aprox)
    private boolean initializedZone = false;

    // objetos optimizados
    private final Matrix matrizRotacion = new Matrix();
    private final Paint paintCaja = new Paint();
    private final Paint paintTexto = new Paint();

    public AnalizadorFrames(
            Context context,
            VehicleProcessor vehicleProcessor,
            CrosswalkProcessor crosswalkProcessor,
            EscucharDeteccion escuchador,
            renderedFrameListener listener,
            int anchoPreview,
            int altoPreview,
            InfringmentZoneView viewZone
    ){
        conversor = new yuv2rgb(context);
        this.vehicleProcessor = vehicleProcessor;
        this.crosswalkProcessor = crosswalkProcessor;
        this.escuchador = escuchador;
        this.listener = listener;
        this.anchoPreview = anchoPreview;
        this.altoPreview = altoPreview;
        this.viewZone = viewZone;

        // Configuración inicial de herramientas de dibujo
        paintCaja.setStyle(Paint.Style.STROKE);
        paintCaja.setStrokeWidth(6);
        paintCaja.setColor(Color.GREEN);

        paintTexto.setColor(Color.RED);
        paintTexto.setTextSize(50);
        paintTexto.setStyle(Paint.Style.FILL);

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

        // actualizar zona desde la UI
        List<PointF> vertices = viewZone.getVertices();
        vehicleProcessor.setZone(vertices);

        try {
            Image imagen = imageProxy.getImage();
            if (imagen == null) return;

            // 1. convertir a Bitmap
            Bitmap bitmapOriginal = conversor.convertir(imagen);
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

            // 3. procesamiento de vehiculos
            List<TrackedVehicle> vehicles =
                    vehicleProcessor.processFrame(
                            bitmapRotado,
                            bitmapRotado.getWidth(),
                            bitmapRotado.getHeight()
                    );

            // 4. dibujo directo sobre el bitmapRotado
            Canvas canvas = new Canvas(bitmapRotado);

            if (vehicles != null) {
                for (TrackedVehicle vehicle : vehicles) {
                    RectF cajaBitmap = new RectF(
                            vehicle.boxModel.centroX - vehicle.boxModel.ancho / 2f,
                            vehicle.boxModel.centroY - vehicle.boxModel.alto / 2f,
                            vehicle.boxModel.centroX + vehicle.boxModel.ancho / 2f,
                            vehicle.boxModel.centroY + vehicle.boxModel.alto / 2f
                    );
                    // convertimos coordenadas normalizadas a pixeles reales
                    cajaBitmap.left *= bitmapRotado.getWidth();
                    cajaBitmap.right *= bitmapRotado.getWidth();

                    cajaBitmap.top *= bitmapRotado.getHeight();
                    cajaBitmap.bottom *= bitmapRotado.getHeight();

                    //********************
                    canvas.drawRect(cajaBitmap, paintCaja);

                    String text = "ID: " + vehicle.idVehicle;
                    if (vehicle.plateText != null && !vehicle.plateText.isEmpty()) {
                        text += "-" + vehicle.plateText;
                    }
                    if (vehicle.detectedInfringment) {
                        text += " INFRINGE";
                    }

                    float y = (cajaBitmap.top - 10 < 60) ? cajaBitmap.bottom + 60 : cajaBitmap.top - 10;
                    canvas.drawText(
                            text,
                            cajaBitmap.left,
                            y,
                            paintTexto);
                }
            }

            // 5. detectar cruce peatonal
            contadorFramesCruce++;
            if (contadorFramesCruce >= 20) {
                contadorFramesCruce = 0;
                List<PointF> zone = crosswalkProcessor.detectZone(
                        bitmapRotado,
                        bitmapRotado.getWidth(),
                        bitmapRotado.getHeight()
                );

                if (!initializedZone && zone != null) {
                    Log.d("CROSSWALK", "Cruce detectado");
                    initializedZone = true;
                    viewZone.post(() -> {
                        viewZone.setVertices(zone);
                        Log.d("CRUCE_DEBUG", "Seteando vertices");
                    });
                }
            }

            // 6. enviamos los resultados al mainActivity
            if(escuchador != null) escuchador.alDetectarRectangulo(vehicles);
            if(listener != null) listener.onRenderizedFrame(bitmapRotado);


        } catch (Exception e) {
            Log.e("ANALIZADOR", "Error procesando frame", e);
        } finally {
            procesando = false;
            imageProxy.close();
        }
    }

    public void actualizarDimensiones(int nuevoAncho, int nuevoAlto) {
        this.anchoPreview = nuevoAncho;
        this.altoPreview = nuevoAlto;
    }
}