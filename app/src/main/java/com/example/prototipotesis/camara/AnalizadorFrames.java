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

import org.tensorflow.lite.Interpreter;
import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.ml.DetectorPlacas;
import com.example.prototipotesis.ocr.NormalizarPlaca;
import com.example.prototipotesis.ocr.OCRHelper;
import com.example.prototipotesis.utils.yuv2rgb;
import com.example.prototipotesis.utils.BoundingBoxOverlay;

import java.util.List;

public class AnalizadorFrames implements ImageAnalysis.Analyzer {
    //**************************************************
    // fragmento para recibir el PreviewView del MainActivity
    public interface EscucharDeteccion{
        void alDetectarRectangulo(RectF rectangulo);
    }
    private EscucharDeteccion escuchador;
    //**************************************************
    private boolean procesando = false; // Evita procesar varios frames al mismo tiempo
    private DetectorPlacas detectorPlacas;
    private yuv2rgb conversor;

    private BoundingBoxOverlay overlay;

    // limitamos los fps para no saturar CPU
    private long ultimoTiempoProcesado = 0;
    private static final long INTERVALO_PROCESAMIENTO = 30; //ms (5 fps aprox)
    private OCRHelper ocrHelper = new OCRHelper();
    // variables para obtener el alto y ancho visible del previewView
    private int anchoPreview;
    private int altoPreview;
    public AnalizadorFrames(Context context,
                            Interpreter interprete,
                            EscucharDeteccion escuchador,
                            BoundingBoxOverlay overlay,
                            int anchoPreview,
                            int altoPreview){
        conversor = new yuv2rgb(context);
        detectorPlacas = new DetectorPlacas(interprete);
        this.escuchador = escuchador;
        this.overlay = overlay;

        this.anchoPreview = anchoPreview;
        this.altoPreview = altoPreview;
    }

    @ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        long tiempoActual = System.currentTimeMillis();
        //Log.d("ANALISIS_CAMARA", "Frame recibido");

        //  limitamos la cantidad de frames procesados
        if((tiempoActual - ultimoTiempoProcesado) < INTERVALO_PROCESAMIENTO){
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
            if(imagen == null) return;

            // convertimos la imagen a bitmap
            Bitmap bitmap = conversor.convertir(imagen);
            if(bitmap == null) return;


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

            //Log.d("FRAME", "Bit generado correctamente");
            procesarFrame(bitmapRotado);
        }
        catch (Exception e) {
            Log.e("ANALIZADOR", "Error procesando frame", e);
        }
        finally{
            procesando = false;
            imageProxy.close();
        }
    }

    private void procesarFrame(Bitmap bitmapOriginal){
        // deteccion de placas
        float[][][] salida = detectorPlacas.detectarPlacas(bitmapOriginal);

        BoundingBox caja = detectorPlacas.obtenerMejorPlaca(salida);

        if(caja != null){
            // convertimos las coordenadas del modelo al tamanio original
            RectF rectanguloPreview = convertirCoordenadasAPreview(
                    caja,
                    bitmapOriginal.getWidth(),
                    bitmapOriginal.getHeight()
            );

            // enviamos el rectangulo escalado al MainActiivity
            if (escuchador != null){
                escuchador.alDetectarRectangulo(rectanguloPreview);
            }

            Log.d("YOLO", "placa detectada");

            // recortamos la placa
            Bitmap placaRecortada = detectorPlacas.recortarPlaca(bitmapOriginal, caja);

            // ejecutamos el ocr
            ocrHelper.reconocerTexto(placaRecortada, new OCRHelper.ResultadoOCR() {
                @Override
                public void onResultado(List<String> posiblesPlacas) {
                    if (!posiblesPlacas.isEmpty()){
                        String textoNormalizado =
                                NormalizarPlaca.normalizar(posiblesPlacas.get(0));
                        Log.d("TEXTO_EXTRAIDO", textoNormalizado);
                        // enviamos el texto al overlay
                        if(overlay != null){
                            overlay.setTextoPlaca(textoNormalizado);
                        }
                        else{
                            overlay.setTextoPlaca("");
                        }
                    }
                }

                @Override
                public void onError(Exception error) {
                    Log.e("OCR", "Error OCR", error);
                }
            });
        }
        else{
            // si no hay deteccion, enviar null
            if (escuchador != null){
                escuchador.alDetectarRectangulo(null);
            }
        }
    }

    /* convierte las coordenadas del modelo 960x960 (boundingBox)
    a coordenadas del previewView
    * */
    private RectF convertirCoordenadasAPreview(
            BoundingBox caja,
            int anchoOriginal,
            int altoOriginal
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
