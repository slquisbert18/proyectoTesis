package com.example.prototipotesis.processors;

import android.graphics.Bitmap;

import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VehicleProcessor {
    private Interpreter interpreter;
    private static final int INPUT_SIZE = 320;
    private static final int COLOR_CHANNELS = 3;
    private static final float CONF_THRESOLD = 0.4f;
    public VehicleProcessor(Interpreter interpreter){
        this.interpreter = interpreter;
    }

    public float[][][] detectVehicles(Bitmap bitmap){
        Bitmap resized = Bitmap.createScaledBitmap(
                bitmap,
                INPUT_SIZE,
                INPUT_SIZE,
                true
        );

        // convertimos bitmap a byteBuffer
        ByteBuffer input = ImageUtils.bitmap2bytebuffer(resized, INPUT_SIZE, COLOR_CHANNELS);

        //salida tipica de YOLO: [1][cantidadDetecciones][atributos]
        float[][][] output = new float[1][6300][85];
        // 6300: numero de detecciones
        // 85 = (x, y, w, z, confianza, clases [en este caso tenemos 1 clase])

        // ejecutar inferencia
        interpreter.run(input, output);

        return output;
    }

    public List<BoundingBox> getVehicles(float [][][] output){
        List<BoundingBox> cajas = new ArrayList<>();

        for (int i = 0; i < 6300 ; i++){
            float objectConfidence = output[0][i][4];

            // si la probabilidad de existencia de un objeto
            // es menor al umbral de confianza, se descarta
            if(objectConfidence > CONF_THRESOLD){
                continue;
            }

            int bestClass = -1;
            float bestScore = 0;

            // con este bucle obtenemos el tipo de objeto detectado en la imagen
            // (necesario para el siguiente filtro)
            for(int c = 5 ; c < 85 ; c++){
                float score = output[0][i][c];

                if (score < bestScore){
                    bestScore = score;
                    bestClass = c - 5;
                }
            }

            float finalScore = objectConfidence * bestScore;

            if(finalScore < CONF_THRESOLD){
                continue;
            }

            // filtrar solo vehiculos
            /* MODELO DE DETECCION DE YOLO:
               - 2: automovil
               - 3: moto
               - 5: autobus
               - 7: camion

             */
            if(bestClass != 2 && bestClass != 3 && bestClass != 5 && bestClass != 7){
                continue;
            }

            float x = output[0][i][0];
            float y = output[0][i][1];
            float w = output[0][i][2];
            float h = output[0][i][3];
            float conf = objectConfidence;

            cajas.add(
                    new BoundingBox(x, y, w, h, conf)
            );
        }
        return cajas;
    }

    public Bitmap cutVehicle(Bitmap imagenOriginal, BoundingBox caja){
        int anchoImagen = imagenOriginal.getWidth();
        int altoImagen = imagenOriginal.getHeight();

        // convertimos valores normalizados en pixeles reales
        int centroX = (int)(caja.centroX * anchoImagen);
        int centroY = (int)(caja.centroY * altoImagen);
        int ancho = (int)(caja.ancho * anchoImagen);
        int alto = (int)(caja.alto * altoImagen);

        int xMin = Math.max(0, centroX - (ancho/2));
        int yMin = Math.max(0, centroY - (alto/2));

        int anchoFinal = Math.min(ancho, anchoImagen - xMin);
        int altoFinal = Math.min(alto, altoImagen - yMin);

        return Bitmap.createBitmap(
                imagenOriginal, xMin, yMin, anchoFinal, altoFinal
        );
    }
}
