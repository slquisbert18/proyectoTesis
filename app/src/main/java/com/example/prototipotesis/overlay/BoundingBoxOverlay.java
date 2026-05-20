package com.example.prototipotesis.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;

import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.Dibujo;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlay extends View {

    // lista de placas trackeadas
    private List<TrackedVehicle> vehicles = new ArrayList<>();
    private final Dibujo dibujo = new Dibujo();


    public BoundingBoxOverlay(Context context){
        super(context);
        init();
    }

    public BoundingBoxOverlay(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public BoundingBoxOverlay(Context context, AttributeSet attrs, int style){
        super(context, attrs, style);
        init();
    }

    private void init(){
        // para que la vista se dibuje correctamente
        setWillNotDraw(false);

        // necesario para limpiar canvas
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

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

        if (vehicles == null || vehicles.isEmpty()) return;

        for (TrackedVehicle vehicle : vehicles) {
            // primero definimos el color de la caja
            int colorBB = 0;
            if (vehicle.inZone) {
                colorBB = Color.YELLOW; // amarillo si esta en zona de infraccion
            } else if (vehicle.detectedInfringment) {
                colorBB = Color.RED; // rojo si cometio la infraccion
            } else {
                colorBB = Color.DKGRAY; // gris todo posi
            }

            // dibujar rectangulo alrededor del vehiculo
            dibujo.dibujarRectangulo(canvas, vehicle.box, colorBB);

            // posiciones base
            float x = vehicle.box.left;
            float y = vehicle.box.top - 10;

            // evitamos que se salga de pantalla
            if (y < 60) {
                y = vehicle.box.bottom + 60;
            }

            // texto con ID + ocr
            String text = "ID: " + vehicle.idVehicle;

            // concatenamos si es que tenemos ocr
            if (vehicle.plateText != null && !vehicle.plateText.isEmpty()) {
                text += "-" + vehicle.plateText;
            }

            // si tenemos infraccion, concatenamos "INFRACCION" y cambiamos el color de la caja
            if (vehicle.detectedInfringment) {
                text += " INFRINGE";
            }
            // mostramos el texto final
            dibujo.dibujarTexto(canvas, text, x, y, colorBB);
        }
    }
}