package com.example.prototipotesis.processors;

import android.graphics.Bitmap;
import android.graphics.PointF;

import com.example.prototipotesis.detectors.CrosswalkDetector;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.List;

public class CrosswalkProcessor {
    private CrosswalkDetector cwDetector;
    private boolean initializedZone = false; // para evitar que tiemble

    public CrosswalkProcessor(Interpreter interpreter){
        this.cwDetector = new CrosswalkDetector(interpreter);
    }

    public List<PointF> detectZone(
           Bitmap bitmap,
           int widthPrev,
           int heightPrev
    ){
        // si ya esta definida la zona, no calculamos de nuevo
        if(initializedZone){
            return null;
        }

        // inferencia
        float[][][] output = cwDetector.detectCrosswalk(bitmap);

        // obtener poligonos
        List<List<PointF>> polygons = cwDetector.getPolygons(output);

        if(polygons == null || polygons.isEmpty()){
            return null;
        }

        // elegir mejor poligono
        List<PointF> bestPolygon = getLargestPolygon(polygons);

        // limpieza de puntos basura
        List<PointF> cleanPolygon = new ArrayList<>();

        for(PointF p : bestPolygon){
            if(p.x > 0.05f && p.y > 0.05f){
                cleanPolygon.add(p);
            }
        }

        // limitar cantidad de puntos
        if(cleanPolygon.size() > 4){
            cleanPolygon = cleanPolygon.subList(0, 4);
        }

        // ordenar puntos
        cleanPolygon = sortPolygon(cleanPolygon);

        // escalar a tamanio preview
        List<PointF> escalatedPolygon = escalatePolygon(
                cleanPolygon,
                widthPrev,
                heightPrev
        );

        if(escalatedPolygon != null && validPolygon(escalatedPolygon)){
            initializedZone = true;
        }

        return escalatedPolygon;
    }

    private List<PointF> escalatePolygon(
            List<PointF> polygon,
            int widthPrev,
            int heightPrev
    ){
        List<PointF> escalated = new ArrayList<>();

        for(PointF p : polygon){
            float x = p.x * widthPrev;
            float y = p.y * heightPrev;

            // usaremos el parche CLAMP
            x = Math.max(0, Math.min(x, widthPrev));
            y = Math.max(0, Math.min(y, heightPrev));

            escalated.add(new PointF(x, y));
        }
        return escalated;
    }
    private List<PointF> getLargestPolygon(List<List<PointF>> polygons){

        float maxArea = 0;
        List<PointF> best = null;

        for(List<PointF> poly : polygons){

            float area = calculateArea(poly);

            if(area > maxArea){
                maxArea = area;
                best = poly;
            }
        }

        return best;
    }
    private float calculateArea(List<PointF> poly){
        float area = 0;
        int n = poly.size();

        for(int i = 0; i < n; i++){
            PointF p1 = poly.get(i);
            PointF p2 = poly.get((i + 1) % n);

            area += (p1.x * p2.y) - (p2.x * p1.y);
        }

        return Math.abs(area) / 2f;
    }

    private List<PointF> sortPolygon(List<PointF> puntos){

        PointF centro = new PointF(0,0);

        for(PointF p : puntos){
            centro.x += p.x;
            centro.y += p.y;
        }

        centro.x /= puntos.size();
        centro.y /= puntos.size();

        puntos.sort((a, b) -> {
            double angA = Math.atan2(a.y - centro.y, a.x - centro.x);
            double angB = Math.atan2(b.y - centro.y, b.x - centro.x);
            return Double.compare(angA, angB);
        });

        return puntos;
    }
    private boolean validPolygon(List<PointF> poly){

        for(PointF p : poly){
            if(p.x < 0 || p.y < 0){
                return false;
            }
        }

        return true;
    }

}
