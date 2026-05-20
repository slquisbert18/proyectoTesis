package com.example.prototipotesis.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;

import android.util.Log;

import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.CoordinateUtils;
import com.example.prototipotesis.utils.Dibujo;

import java.util.List;

/*
* RENDERIZADOR CENTRAL DEL PROYECTO
* trabaja para procesamiento en tiempo real, procesamiento desde galeria
* */

public class RenderizadorDetecciones {
    private static final Dibujo dibujo = new Dibujo();

    public RenderizadorDetecciones(){
    }

    // se recibe un bitmap y una lista de vehiculos (puntos)
    // para dibujar sobre el bitmap usando los datos de cada elemento de la lista

    /*
        * usarCoordenadasModelo:
        * true  -> usa boxModel (coordenadas normalizadas YOLO)
        * false -> usa box (coordenadas preview)
    */
    public static Bitmap dibujarDetecciones(
            Bitmap bitmapOriginal,
            List<TrackedVehicle> vehicles,
            List<PointF> vertices,
            boolean usarCoordenadasModelo
    ){
        // copia editable del bitmapOriginal
        Bitmap bitmapEditable = bitmapOriginal.copy(
                Bitmap.Config.ARGB_8888,
                true
        );

        Canvas canvas = new Canvas(bitmapEditable);

        for(TrackedVehicle vehicle : vehicles){
            RectF cajaFinal;

            // coordenadas normalizadas YOLO si
            if(usarCoordenadasModelo && vehicle.boxModel != null){
                cajaFinal = CoordinateUtils.modelo2Bitmap(
                        vehicle.boxModel,
                        bitmapEditable.getWidth(),
                        bitmapEditable.getHeight()
                );
            }
            // coordenadas preview
            else{
                cajaFinal = vehicle.box;
            }

            // dibujamos el boundingBox
            dibujo.dibujarRectangulo(canvas,cajaFinal, 0);

            // CONSTRUCCION DEL TEXTO
            String texto = "ID: " + vehicle.idVehicle;

            if(vehicle.plateText != null && !vehicle.plateText.isEmpty()){
                texto += " - " + vehicle.plateText;
            }

            if(vehicle.detectedInfringment){
                texto += " INFRINGE";
            }

            // calculamos su posicion
            // si el texto se sale de la imagen por arriba, es dibujado abajo del cuadro
            float y = (cajaFinal.top - 10 < 60)
                    ? cajaFinal.bottom + 60
                    : cajaFinal.top - 10;

            // dibujamos el texto
            dibujo.dibujarTexto(
                    canvas, texto, cajaFinal.left, y, 0);
        }

        // dibujamos los poligonos
        if(vertices != null && vertices.size() >= 4){
            dibujo.dibujarLineas(canvas, vertices, 0);
            dibujo.dibujarPuntos(canvas, vertices, 0);
        }

        // retornamos el bitmap Editable (copia del original)
        // con los dibujos encima
        return bitmapEditable;
    }
}

