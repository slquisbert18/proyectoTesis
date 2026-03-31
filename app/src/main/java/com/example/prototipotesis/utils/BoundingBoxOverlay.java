package com.example.prototipotesis.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.prototipotesis.trackedObject.TrackedPlate;
import com.example.prototipotesis.trackedObject.TrackedVehicle;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlay extends View {

    private Paint pinturaCaja;
    private Paint pinturaTexto;

    // *********************** PLACAS *****************************
    // lista de placas trackeadas
    private List<TrackedVehicle> vehicles = new ArrayList<>();

    // ****************** ZONA DE INFRACCION **********************
    private RectF infringmentZone = new RectF(300, 800, 900, 1000);
    private Paint zonePaint;

    public BoundingBoxOverlay(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);

        // para que la vista se dibuje correctamente
        setWillNotDraw(false);

        // necesario para limpiar canvas
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        pinturaCaja = new Paint();
        pinturaCaja.setStyle(Paint.Style.STROKE);
        pinturaCaja.setStrokeWidth(5f);
        pinturaCaja.setColor(Color.GREEN);

        pinturaTexto = new Paint();
        pinturaTexto.setColor(Color.GREEN);
        pinturaTexto.setTextSize(50f);
        pinturaTexto.setStyle(Paint.Style.FILL);
        pinturaTexto.setFakeBoldText(true);

        zonePaint = new Paint();
        zonePaint.setColor(Color.RED);
        zonePaint.setStyle(Paint.Style.STROKE);
        zonePaint.setStrokeWidth(6f);
    }

    // actualizar lista de placas
    public void updateVehicles(List<TrackedVehicle> newVehicles) {
        if (newVehicles != null) {
            this.vehicles = newVehicles;
        } else {
            this.vehicles = new ArrayList<>();
        }
        invalidate();
    }

    // asociar texto OCR a una placa específica
    public void actualizarTexto(int idPlaca, String texto){
        invalidate();
    }

    public void limpiar(){
        this.vehicles = new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //limpiamos el canvas antes de dibujar
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        super.onDraw(canvas);

        // dibujamos zona de infraccion
        canvas.drawRect(infringmentZone, zonePaint);

        if (vehicles == null || vehicles.isEmpty()) return;

        for (TrackedVehicle vehicle : vehicles) {

            // dibujar rectangulo
            canvas.drawRect(vehicle.box, pinturaCaja);

            // posiciones base
            float x = vehicle.box.left;
            float y = vehicle.box.top - 10;

            // evitamos que se salga de pantalla
            if (y < 60) {
                y = vehicle.box.bottom + 60;
            }

            // texto 1: id del vehiculo
            String text = "ID: " + vehicle.idVehicle;
            canvas.drawText(text, x, y, pinturaTexto);

            // dibujar el texto de la placa
            if(vehicle.plateText != null && !vehicle.plateText.isEmpty()){
                text += "-" + vehicle.plateText;
            }

            if(vehicle.detectedInfringment){
                text += " INFRINGE";
            }

            canvas.drawText(
                    vehicle.plateText,
                    x,
                    y + 60,
                    pinturaTexto);

            // cambiar de color la zona cuando haya infraccion
            if(vehicle.detectedInfringment){
                pinturaCaja.setColor(Color.RED);
            }
            else{
                pinturaCaja.setColor(Color.GREEN);
            }
        }
    }
}