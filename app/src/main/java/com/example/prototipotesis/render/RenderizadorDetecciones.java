package com.example.prototipotesis.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import android.util.Log;

import com.example.prototipotesis.trackedObject.TrackedVehicle;

import java.util.List;

/*
* RENDERIZADOR CENTRAL DEL PROYECTO
* trabaja para procesamiento en tiempo real, procesamiento desde galeria
* */
public class RenderizadorDetecciones {
    // herramientas reutilizables de dibujo
    private final Paint paintCaja = new Paint();
    private final Paint paintTexto = new Paint();

    public RenderizadorDetecciones(){
        paintCaja.setStyle(Paint.Style.STROKE);
        paintCaja.setStrokeWidth(6);
        paintCaja.setColor(Color.GREEN);

        paintTexto.setColor(Color.GREEN);
        paintTexto.setTextSize(50);
        paintTexto.setStyle(Paint.Style.FILL);
    }

    // se recibe un bitmap y una lista de vehiculos (puntos)
    // para dibujar sobre el bitmap usando los datos de cada elemento de la lista

    /*
        * usarCoordenadasModelo:
        * true  -> usa boxModel (coordenadas normalizadas YOLO)
        * false -> usa box (coordenadas preview)
    */
    public Bitmap dibujarDetecciones(
            Bitmap bitmapOriginal,
            List<TrackedVehicle> vehicles,
            boolean usarCoordenadasModelo
    ){
        if(bitmapOriginal == null || vehicles == null){
            Log.d("PARAMETROS_VACIOS", "Bitmap y list<trackedVehicles> == null");
            return null;
        }
        else if(bitmapOriginal == null){
            Log.d("PARAMETROS_VACIOS", "Bitmap == null");
            return null;
        }
        else if(vehicles == null){
            Log.d("PARAMETROS_VACIOS", "list<trackedVehicles> == null");
            return bitmapOriginal;
        }

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
                cajaFinal = new RectF(
                    // se calculan las coordenadas relativas de la caja usando el centro, ancho y alto
                    vehicle.boxModel.centroX - vehicle.boxModel.ancho / 2f,
                    vehicle.boxModel.centroY - vehicle.boxModel.alto / 2f,
                    vehicle.boxModel.centroX + vehicle.boxModel.ancho / 2f,
                    vehicle.boxModel.centroY + vehicle.boxModel.alto / 2f
                );

                // escalar las coordenadas relativas al tamanio de la imagen
                cajaFinal.left *= bitmapEditable.getWidth();
                cajaFinal.right *= bitmapEditable.getWidth();
                cajaFinal.top *= bitmapEditable.getHeight();
                cajaFinal.bottom *= bitmapEditable.getHeight();
            }
            // coordenadas preview
            else{
                if(vehicle.box == null) continue;
                cajaFinal = vehicle.box;
            }

            // dibujamos el boundingBox
            canvas.drawRect(cajaFinal, paintCaja);

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
            canvas.drawText(
                    texto,
                    cajaFinal.left,
                    y,
                    paintTexto
            );
        }
        // retornamos el bitmap Editable (copia del original)
        // con los dibujos encima
        return bitmapEditable;
    }
}

