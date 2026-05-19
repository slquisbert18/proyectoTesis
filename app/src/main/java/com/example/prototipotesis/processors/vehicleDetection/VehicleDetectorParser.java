package com.example.prototipotesis.processors.vehicleDetection;

import com.example.prototipotesis.ml.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/*
* con esta clase obtendremos solo los vehiculos detectados por el modelo YOLO
* Salida: lista de bb (boundingBoxes) que pasaron las pruebas de validacion
* */
public class VehicleDetectorParser {

    private static final float CONF_THRESHOLD = 0.5f;

    public List<BoundingBox> obtenerVehiculos(float[][][] output){
        List<BoundingBox> cajas = new ArrayList<>();
        for(int i = 0; i < 6300 ; i++){
            float confianzaObjeto = output[0][i][4];

            if(confianzaObjeto < CONF_THRESHOLD) continue;

            int mejorClase = obtenerMejorClase(output, i);

            if(!esVehiculo(mejorClase)){
                continue;
            }

            float x = output[0][i][0];
            float y = output[0][i][1];
            float w = output[0][i][2];
            float h = output[0][i][3];

            float scoreClase =
                    output[0][i][mejorClase + 5];

            float scoreFinal =
                    confianzaObjeto * scoreClase;

            if(!esCajaValida(w, h, scoreFinal)){
                continue;
            }

            cajas.add(
                    new BoundingBox(
                            x,
                            y,
                            w,
                            h,
                            confianzaObjeto
                    )
            );
        }

        return cajas;
    }

    // analiza las 80 clases para encontrar la de mayor probabilidad.
    // devuelve el id de la mejor clase
    private int obtenerMejorClase(float[][][] output, int indiceDeteccion) {
        int mejorClase = -1;
        float mejorScore = 0;
        for(int c = 5 ; c < 85 ; c++){
            float score = output[0][indiceDeteccion][c];
            if(score > mejorScore){
                mejorScore = score;
                mejorClase = c - 5;
            }
        }

        return mejorClase;
    }

    private boolean esVehiculo(int clase){
        return clase == 2
                || clase == 3
                || clase == 5
                || clase == 7;
    }


    // verifica que una caja tenga condiciones validas: area, forma y confianza
    private boolean esCajaValida(float ancho, float alto, float scoreFinal){
        if(scoreFinal < CONF_THRESHOLD){
            return false;
        }

        float area = ancho * alto;

        if(area < 0.02f){
            return false;
        }

        float ratio = ancho / alto;

        return ratio >= 0.5f
                && ratio <= 3.5f;
    }
}