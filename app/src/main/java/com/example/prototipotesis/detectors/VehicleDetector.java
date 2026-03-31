package com.example.prototipotesis.detectors;

import android.graphics.Bitmap;

import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VehicleDetector {
    private Interpreter interpreter;
    private static final int INPUT_SIZE = 320;
    private static final int COLOR_CHANNELS = 3;
    private static final float CONF_THRESOLD = 0.5f;
    public VehicleDetector(Interpreter interpreter){
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
        // 85 = (x, y, w, z, confianza, clases [en este caso tenemos 80 clases])

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
            if(objectConfidence < CONF_THRESOLD){
                continue;
            }

            int bestClass = -1;
            float bestScore = 0;

            // con este bucle obtenemos el tipo de objeto detectado en la imagen
            // (necesario para el siguiente filtro)
            for(int c = 5 ; c < 85 ; c++){
                float score = output[0][i][c];

                if (score > bestScore){
                    bestScore = score;
                    bestClass = c - 5;
                }
            }

            float finalScore = objectConfidence * bestScore;
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

            if(finalScore > CONF_THRESOLD){
                float area = w * h;
                if(area < 0.02f) continue;

                float ratio = w / h;
                if(ratio < 0.5f || ratio > 3.5f) continue;

                cajas.add(
                        new BoundingBox(x, y, w, h, conf)
                );
            }
        }
        return cajas;
    }

    public Bitmap cutVehicle(Bitmap originalBitmap, BoundingBox box){
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        // convertimos valores normalizados en pixeles reales
        int x = (int)((box.centroX - box.ancho / 2f) * width);
        int y = (int)((box.centroY - box.alto / 2f) * height);
        int w = (int)(box.ancho * width);
        int h = (int)(box.alto * height);

        x = Math.max(0, x);
        y = Math.max(0, y);
        w = Math.min(width - x, w);
        h = Math.min(height - y, h);

        return Bitmap.createBitmap(
                originalBitmap, x, y, w, h
        );
    }

}
