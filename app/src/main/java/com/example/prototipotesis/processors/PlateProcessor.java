package com.example.prototipotesis.processors;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.List;
import org.tensorflow.lite.Interpreter;

import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.detectors.PlateDetector;
import com.example.prototipotesis.ml.ocr.NormalizarPlaca;
import com.example.prototipotesis.ml.ocr.OCRHelper;
import com.example.prototipotesis.processors.plateDetection.PlateDetectorParser;
import com.example.prototipotesis.utils.BitmapUtils;

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

        BoundingBox bestBox = PlateDetectorParser.seleccionarMejorPlaca(boxes);

        // si no hay una caja valida, salimos
        if(bestBox == null){
            Log.d("PLACA_DEBUG", "No hay caja válida");
            callback.onDetectedText("");
            return;
        }

        Bitmap croppedPlate =
                BitmapUtils.recortarBitmap(originalBitmap, bestBox);

        Bitmap ocrPlate = BitmapUtils.color2gray(
                BitmapUtils.escalar(croppedPlate, 300)
        );

        ocrHelper.reconocerTexto(
                ocrPlate,
                new OCRHelper.ResultadoOCR() {
                    @Override
                    public void onResultado(List<String> plateCandidates) {
                        if(plateCandidates == null || plateCandidates.isEmpty()) return;
                        String bestText = NormalizarPlaca.extraerMejor(plateCandidates);
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
