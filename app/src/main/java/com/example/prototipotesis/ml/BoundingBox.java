package com.example.prototipotesis.ml;

import android.graphics.Bitmap;

/*
 * REPRESENTA una deteccion, no ejecuta el modelo
 */
public class BoundingBox {
    public float centroX;
    public float centroY;
    public float ancho;
    public float alto;
    public float confianza;

    public BoundingBox(
            float centroX, float centroY, float ancho, float alto, float confianza
    ){
        this.centroX = centroX;
        this.centroY = centroY;
        this.ancho = ancho;
        this.alto = alto;
        this.confianza = confianza;
    }
}
