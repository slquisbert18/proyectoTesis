package com.example.prototipotesis.ml.ocr;
import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
public class OCRHelper {
    public interface SimpleResult{
        void onDetectedText(String texto);
    }
    /*
    procesa una imagen y devuelve lista de posibles textos detectados
    * */
    public void reconocerTexto(
            Bitmap imagenPlaca,
            ResultadoOCR callback
    ){
        // convertir Bitmap a InputImage (formato necesario para mlkit)
        InputImage imagenEntrada = InputImage.fromBitmap(imagenPlaca, 0); // 0 = sin rotacion

        // creamos reconocedor de texto en alfabeto latino
        TextRecognition.getClient(
                new TextRecognizerOptions.Builder().build()
        ).process(imagenEntrada).addOnSuccessListener(resultado -> {
            List<String> posiblesPlacas = new ArrayList<>();

            // recorrer bloques de texto detectados
            for(Text.TextBlock bloque : resultado.getTextBlocks()){
                for(Text.Line linea : bloque.getLines()){
                    String textoDetectado = linea.getText();
                    // limpieza basica del texto
                    textoDetectado = textoDetectado.replaceAll("[^A-Z0-9]", "").toUpperCase();

                    if(textoDetectado.length() >= 5){
                        posiblesPlacas.add(textoDetectado);
                    }
                }
            }

            callback.onResultado(posiblesPlacas);
        }).addOnFailureListener(error -> {
            callback.onError(error);
        });
    }

    /*
    * interfaz para devolver resultados asincronos
    * */
    public interface ResultadoOCR{
        void onResultado(List<String> posiblesPlacas);
        void onError(Exception error);
    }
}
