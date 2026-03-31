package com.example.prototipotesis.trackedObject;

import android.graphics.RectF;

import com.example.prototipotesis.ml.BoundingBox;

import java.util.LinkedList;
import java.util.List;

public class TrackedPlate {
    public int id;
    public BoundingBox cajaModelo; // caja original
    public RectF caja;
    public int framesSinDetectar = 0;
    public String text = "";
    public boolean ocrEnProceso = false;
    public int framesDesdeUltimoOcr = 0;

    // variables para majority voting (el sistema elige la placa que mas veces se repite)
    public List<String> historialOCR = new LinkedList<>();
    public static final int MAX_HISTORIAL_OCR = 6; // cantidad de resultados para votar

    public TrackedPlate(int id, RectF caja){
        this.id = id;
        this.caja = caja;
    }

    // metodo para actualizar la posición del track cuando se detecta nuevamente
    public void actualizar(RectF nuevaCaja, BoundingBox nuevaCajaModelo){

        this.caja = nuevaCaja; // actualiza posición en pantalla

        this.cajaModelo = nuevaCajaModelo; // actualiza caja del modelo

        this.framesSinDetectar = 0; // reinicia contador
    }
}
