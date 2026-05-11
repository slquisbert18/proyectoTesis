package com.example.prototipotesis.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.prototipotesis.trackedObject.TrackedVehicle;

import java.util.List;

public class RenderizadorDetecciones {
    public static Bitmap dibujarDetecciones(Bitmap bitmapOriginal, List<TrackedVehicle> vehiculos){
        // copiar bitmap para editarlo
        Bitmap bitmapEditable = bitmapOriginal.copy(
                Bitmap.Config.ARGB_8888,
                true
        );

        // linezo
        Canvas canvas = new Canvas(bitmapEditable);

        // pintura para cajas
        Paint pinturaCaja = new Paint();
        pinturaCaja.setColor(Color.GREEN);
        pinturaCaja.setStyle(Paint.Style.STROKE);
        pinturaCaja.setStrokeWidth(6f);

        // pintura para texto
        Paint pinturaTexto = new Paint();
        pinturaTexto.setColor(Color.GREEN);
        pinturaTexto.setTextSize(30f);
        pinturaTexto.setStyle(Paint.Style.FILL);

        for(TrackedVehicle vehiculo : vehiculos){
            RectF caja = vehiculo.box;

            // dibujar bounding box
            canvas.drawRect(caja, pinturaCaja);

            // texto OCR
            String placa = "";

            if(vehiculo.plateText != null){
                placa = vehiculo.plateText;
            }

            // dibujar texto
            canvas.drawText(
                    placa,
                    caja.left,
                    caja.top - 10,
                    pinturaTexto
            );
        }

        return bitmapEditable;
    }
}
