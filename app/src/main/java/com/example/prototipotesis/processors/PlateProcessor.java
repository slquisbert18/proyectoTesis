package com.example.prototipotesis.processors;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import org.tensorflow.lite.Interpreter;

import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.detectors.PlateDetector;
import com.example.prototipotesis.ocr.NormalizarPlaca;
import com.example.prototipotesis.ocr.OCRHelper;
import com.example.prototipotesis.utils.ImageUtils;

public class PlateProcessor {
    private PlateDetector plateDetector;
    private OCRHelper ocrHelper;

    public PlateProcessor(Interpreter interprete){
        plateDetector = new PlateDetector(interprete);
        ocrHelper = new OCRHelper();
    }

    public void detectPlateTextAsync(
            Bitmap originalBitmap,
            OCRHelper.SimpleResult callback
    ){
        float[][][] output =
                plateDetector.detectarPlacas(originalBitmap);

        List<BoundingBox> boxes =
                plateDetector.obtenerPlacas(output);

        if(boxes == null || boxes.isEmpty()){
            Log.d("OCR_DEBUG", "No se detectaron placas");
            callback.onDetectedText("");
            return;
        }

        Log.d("PLACA_DEBUG", "Placas detectadas en vehiculo:" + boxes.size());

        BoundingBox bestBox = null;
        float bestScore = 0f;

        // elegimos la mejor caja
        for(BoundingBox box : boxes) {
            // filtramos la deteccion si esta es muy pequenia
            float area = box.ancho * box.alto;
            if (area < 0.005f) { // ignoramos cajas muy peques
                continue;
            }

            // score: priorizamos tamanio + confianza
            float score = box.confianza * area;
            if (score > bestScore) {
                bestScore = score;
                bestBox = box;
        }

        // si no hay una caja valida, salimos
        if(bestBox == null){
            Log.d("PLACA_DEBUG", "No hay caja válida");
            callback.onDetectedText("");
            return;
        }

        Bitmap croppedPlate = plateDetector.recortarPlaca(
                originalBitmap,
                bestBox
        );

        Bitmap ocrPlate = ImageUtils.color2gray(
                ImageUtils.escalar(croppedPlate, 300)
        );

        ocrHelper.reconocerTexto(
                ocrPlate,
                new OCRHelper.ResultadoOCR() {
                    @Override
                    public void onResultado(List<String> plateCandidates) {
                        Log.d("DETECCION", "Cajas detectadas: " + boxes.size());
                        if(plateCandidates == null || plateCandidates.isEmpty()){
                            return;
                        }
                        String bestText = "";

                        for(String candidate : plateCandidates){
                            String clean = NormalizarPlaca.normalizar(candidate);
                            Log.d("OCR_DEBUG", "Raw: " + candidate + " -> " + clean);
                            if(clean != null && clean.length() >= 3){
                                bestText = clean;
                                break;
                            }
                        }
                        callback.onDetectedText(bestText);
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e("OCR", "Error OCR", error);
                        callback.onDetectedText("");
                    }
                });
        }
    }

    private RectF coordenates2preview(
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
