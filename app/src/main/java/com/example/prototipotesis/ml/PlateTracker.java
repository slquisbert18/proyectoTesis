package com.example.prototipotesis.ml;

import android.graphics.RectF;

import com.example.prototipotesis.trackedObject.TrackedPlate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlateTracker {

    private List<TrackedPlate> placasActivas = new ArrayList<>();
    private int siguienteId = 1;
    private static final float UMBRAL_IOU = 0.3f;
    private static final int MAX_FRAMES_SIN_DETECTAR = 5;
    public List<TrackedPlate> actualizar(
            List<RectF> nuevasCajasPreview,
            List<BoundingBox> nuevasCajasModelo
    ){
        // lista para saber que placas ya se usaron en este frame
        List<TrackedPlate> placasUsadas = new ArrayList<>();

        // comparamos nuevas cajas con placas existentes
        for(int i = 0; i < nuevasCajasPreview.size() ; i++) {
            RectF nuevaCajaPreview = nuevasCajasPreview.get(i);
            BoundingBox nuevaCajaModelo = nuevasCajasModelo.get(i);

            TrackedPlate mejorMatch = null;
            float mejorIoU = 0f;

            for (TrackedPlate placaExistente : placasActivas) {
                // evitar usar la misma placa 2 veces
                if (placasUsadas.contains(placaExistente)) continue;

                float iou = calcularIoU(nuevaCajaPreview, placaExistente.caja);
                if (iou > UMBRAL_IOU && iou > mejorIoU) {
                    mejorIoU = iou;
                    mejorMatch = placaExistente;
                }
            }

            if (mejorMatch != null) {
                // actualizar caja  existente
                mejorMatch.actualizar(nuevaCajaPreview, nuevaCajaModelo);
                placasUsadas.add(mejorMatch);
            } else {
                // verificamos que existe una placa cercana existente
                boolean placaCercanaExiste = false;
                for (TrackedPlate placaExistente : placasActivas) {
                    float distanciaX = Math.abs(
                            placaExistente.caja.centerX() - nuevaCajaPreview.centerX()
                    );

                    float distanciaY = Math.abs(
                            placaExistente.caja.centerY() - nuevaCajaPreview.centerY()
                    );

                    if (distanciaX < 80 && distanciaY < 80) {
                        placaCercanaExiste = true;
                        break;
                    }
                }

                // en casom de no tener una placa cercana, tomamos una nueva
                if (!placaCercanaExiste) {
                    // nuevaPlaca detectada
                    TrackedPlate nuevaPlaca =
                            new TrackedPlate(siguienteId++, nuevaCajaPreview);

                    nuevaPlaca.cajaModelo = nuevaCajaModelo;

                    placasActivas.add(nuevaPlaca);
                    placasUsadas.add(nuevaPlaca);
                }
            }
        }

        // incrementar contador de placas no detectadas
        for(TrackedPlate placa : placasActivas){
            if(!placasUsadas.contains(placa)){
                placa.framesSinDetectar++;
            }
        }

        // eliminar placas que desaparecieron
        Iterator<TrackedPlate> iterador = placasActivas.iterator();

        while (iterador.hasNext()) {
            TrackedPlate placa = iterador.next();
            if (placa.framesSinDetectar > MAX_FRAMES_SIN_DETECTAR) {
                iterador.remove();
            }
        }

        return placasActivas;
    }
    private float calcularIoU(RectF a, RectF b){
        float interIzq = Math.max(a.left, b.left);
        float interArriba = Math.max(a.top, b.top);
        float interDer = Math.min(a.right, b.right);
        float interAbajo = Math.min(a.bottom, b.bottom);

        float anchoInter = Math.max(0, interDer - interIzq);
        float altoInter = Math.max(0, interAbajo - interArriba);

        float areaInter = anchoInter * altoInter;

        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        float areaUnion = areaA + areaB - areaInter;
        if(areaUnion <= 0) return 0f;

        return areaInter / areaUnion;
    }
}
