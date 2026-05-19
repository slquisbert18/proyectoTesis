package com.example.prototipotesis.managers;

import android.graphics.PointF;

import java.util.List;

public class InfringmentManager {
    /*
    // usaremos la parte inferior del vehiculo
    float centroX = vehicle.box.centerX();
    float centroY = vehicle.box.bottom;

    // verificcar si el objeto esta dentro del poligono
    boolean isInZone = pointInsideZone(centroX, centroY);

    // detectar entrada a la zona (evento)
            if(!vehicle.inZone && isInZone){
        vehicle.detectedInfringment = true;
    }

    // actualizamos estado actual
    vehicle.inZone = isInZone;



    public void setZone(List<PointF> vertices){
        this.verticesZone = vertices;
    }

    private boolean pointInsideZone(float x, float y){
        boolean dentro = false;

        if(verticesZone == null || verticesZone.size() < 3){
            return false;
        }

        for (int i = 0, j = verticesZone.size() - 1; i < verticesZone.size(); j = i++) {
            float xi = verticesZone.get(i).x;
            float yi = verticesZone.get(i).y;
            float xj = verticesZone.get(j).x;
            float yj = verticesZone.get(j).y;

            boolean intersecta = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if(intersecta) dentro = !dentro;
        }

        return dentro;
    }

     */
}
