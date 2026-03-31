package com.example.prototipotesis.ml;

import android.graphics.RectF;

import com.example.prototipotesis.trackedObject.TrackedVehicle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
* Tracker de vehiculos basado en:
* - distancia entre centros
* - interseccion iou
* - prediccion de movimiento (veelocidad)
*
* Flujo por frame:
* - predecir posicion de vehiculos existentes
* - asignacion global (algoritmo hungaro)
* - elimnar vehiculos perdidos
*
* */
public class VehicleTracker {
    private List<TrackedVehicle> activeVehicles = new ArrayList<>();
    private int nextId = 1;

    private static final float DISTANCE_THRESOLD = 300f;
    private static final int MAX_UNDETECTED_FRAMES = 2; // nro de frames que un vehiculo puede desaparecer

    private int framesWithoutDetection = 0; // control de frames sin detecciones

    public List<TrackedVehicle> update(
            List<RectF> newBoxesPreview,
            List<BoundingBox> newBoxesModel){

        // deteccion de cambio de escena para el reset automatico
        if(newBoxesPreview == null || newBoxesPreview.isEmpty()){
            framesWithoutDetection++;

            if (framesWithoutDetection > 5){
                reset(); // reinicio del tracker
            }

            return activeVehicles;
        }
        else{
            framesWithoutDetection = 0;
        }

        // prediccion de movimiento
        for(TrackedVehicle vehicle : activeVehicles){
            // clonamos la caja actual
            RectF predicted = new RectF(vehicle.box);

            // desplazamos segun velocidad
            predicted.offset(vehicle.xSpeed, vehicle.ySpeed);

            // guardamos la prediccion
            vehicle.predictedBox = predicted;
        }

        int n = activeVehicles.size();
        int m = newBoxesPreview.size();

        // creacion de matriz de costos (distancia)
        float[][] costs = new float[n][m];

        for(int i = 0 ; i < n ; i++){
            TrackedVehicle vehicle = activeVehicles.get(i);
            RectF reference = (vehicle.predictedBox != null)
                    ? vehicle.predictedBox
                    : vehicle.box;
            for(int j = 0 ; j < m ; j++){
                RectF detection = newBoxesPreview.get(j);
                float distance = calculateDistance(reference, detection);
                costs[i][j] = distance;
            }
        }

        // asignacion global (algoritmo hungaro)
        boolean[] vehicleAssigned = new boolean[n];
        boolean[] detectionAssigned = new boolean[m];

        for(int k = 0 ; k < Math.min(n, m) ; k++){
            float bestCost = Float.MAX_VALUE;
            int bestI = -1;
            int bestJ = -1;

            for(int i = 0; i < n ; i++){
                if(vehicleAssigned[i]){
                    continue;
                }

                for(int j = 0 ; j < m ; j++){
                    if(detectionAssigned[j]){
                        continue;
                    }

                    float cost = costs[i][j];

                    if(cost < bestCost){
                        bestCost = cost;
                        bestI = i;
                        bestJ = j;
                    }
                }
            }

            // filtro para evitar asignaciones innecesarias
            if(bestI != -1 && bestJ != -1 && bestCost < DISTANCE_THRESOLD){
                vehicleAssigned[bestI] = true;
                detectionAssigned[bestJ] = true;

                TrackedVehicle v = activeVehicles.get(bestI);

                RectF newBox = newBoxesPreview.get(bestJ);
                BoundingBox newModel = newBoxesModel.get(bestJ);

                v.update(newBox, newModel);
                v.undetectedFrames = 0;
            }
        }
        // crear nuevos vehiculos (no asignados)
        for(int j = 0 ; j < m ; j++){
            if(!detectionAssigned[j]){
                TrackedVehicle newV = new TrackedVehicle(
                        nextId++,
                        newBoxesPreview.get(j)
                );
                newV.boxModel = newBoxesModel.get(j);
                activeVehicles.add(newV);
            }
        }

        // aumentar frames sin detectar
        for(int i = 0 ; i < n ; i++){
            if(!vehicleAssigned[i]){
                activeVehicles.get(i).undetectedFrames++;
            }
        }

        // eliminar vehiculos perdidos
        Iterator<TrackedVehicle> iterator = activeVehicles.iterator();
        while(iterator.hasNext()){
            TrackedVehicle vehicle = iterator.next();
            if(vehicle.undetectedFrames > MAX_UNDETECTED_FRAMES){
                iterator.remove();
            }
        }

        return activeVehicles;
    }

    public void reset(){
        activeVehicles.clear(); // eliminamos todos los vehiculos
                                // para reiniciar los id al cambiar de imagen
        nextId = 1;
        framesWithoutDetection = 0;
    }

    // calcular la distancia entre los centros de 2 objetos
    private float calculateDistance(RectF a, RectF b){
        float axCenter = (a.left + a.right) / 2f;
        float ayCenter = (a.top + a.bottom) / 2f;

        float bxCenter = (b.left + b.right) / 2f;
        float byCenter = (b.top + b.bottom) / 2f;

        float dx = axCenter - bxCenter;
        float dy = ayCenter - byCenter;

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // IOU
    private float calculateIoU(RectF a, RectF b){
        float left = Math.max(a.left, b.left);
        float top = Math.max(a.top, b.top);
        float right = Math.min(a.right, b.right);
        float bottom = Math.min(a.bottom, b.bottom);

        float width = Math.max(0, right - left);
        float height = Math.max(0, bottom - top);

        float areaInter = width * height;

        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);

        float areaUnion = areaA + areaB - areaInter;

        if(areaUnion <= 0) return 0f;

        return areaInter / areaUnion;
    }

}
