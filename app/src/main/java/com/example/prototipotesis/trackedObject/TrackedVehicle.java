package com.example.prototipotesis.trackedObject;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.example.prototipotesis.ml.BoundingBox;

import java.util.LinkedList;
import java.util.List;

public class TrackedVehicle {
    public int idVehicle; // identificador unico del vehiculo
    public BoundingBox boxModel; // caja para la captura
    public RectF box; // caja para la preview
    public int undetectedFrames = 0; // contador de frames donde no aparece el vehiculo trackeado

    // ocr de la placa
    public String plateText;
    public boolean ocrInProcess = false;
    public int framesSinceLastOcr = 0;
    public List<String> plateRecord = new LinkedList<>(); // historial ocr
    public static final int MAX_OCR_RECORD = 6;
    public long ocrStartTime = 0;

    // para debug
    public Bitmap vehicleBitmap = null;
    public Bitmap plateBitmap = null;

    // para predecir el movimiento del vehiculo
    public float xSpeed = 0;
    public float ySpeed = 0;
    public RectF predictedBox = null;

    // *************** PARA LOGICA DE COLISIONES ***********************
    public boolean inZone = false;
    public boolean detectedInfringment = false;
    public RectF prevBox = null;

    public TrackedVehicle(int id, RectF box){
        this.idVehicle = id;
        this.box = box;
    }

    public void update(RectF newBox, BoundingBox newBoxModel){
        this.box = newBox;
        this.boxModel = newBoxModel;
        this.undetectedFrames = 0;

        // calculamos centro anterior
        float oldCenterX = box.centerX();
        float oldCenterY = box.centerY();

        // actualizamos la caja
        this.box = newBox;

        // calculamos el nuevo centro
        float newCenterX = newBox.centerX();
        float newCenterY = newBox.centerY();

        // calculamos la velocidad
        xSpeed = newCenterX - oldCenterX;
        ySpeed = newCenterY - oldCenterY;
    }

}
