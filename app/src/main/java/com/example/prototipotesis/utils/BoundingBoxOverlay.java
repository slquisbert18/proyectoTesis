package com.example.prototipotesis.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.prototipotesis.ml.TrackedPlate;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxOverlay extends View {

    private Paint pinturaCaja;
    private Paint pinturaTexto;

    // lista de placas trackeadas
    private List<TrackedPlate> placas = new ArrayList<>();

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
        pinturaTexto.setTextSize(60f);
        pinturaTexto.setStyle(Paint.Style.FILL);
        pinturaTexto.setFakeBoldText(true);
    }

    // actualizar lista de placas
    public void actualizarPlacas(List<TrackedPlate> nuevasPlacas) {
        if (nuevasPlacas != null) {
            this.placas = nuevasPlacas;
        } else {
            this.placas = new ArrayList<>();
        }
        invalidate();
    }

    // asociar texto OCR a una placa específica
    public void actualizarTexto(int idPlaca, String texto){
        invalidate();
    }

    public void limpiar(){
        this.placas = new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //limpiamos el canvas antes de dibujar
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        super.onDraw(canvas);

        if (placas == null || placas.isEmpty()) return;

        for (TrackedPlate placa : placas) {

            // dibujar rectangulo
            canvas.drawRect(placa.caja, pinturaCaja);

            // obtener texto del OCR estabilizado
            String texto = placa.texto;

            if (texto != null && !texto.isEmpty()) {

                float posicionX = placa.caja.left;
                float posicionY = placa.caja.top - 15;

                if (posicionY < 60) {
                    posicionY = placa.caja.bottom + 60;
                }

                canvas.drawText(texto, posicionX, posicionY, pinturaTexto);
            }
        }
    }
}