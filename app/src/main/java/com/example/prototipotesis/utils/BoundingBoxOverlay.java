package com.example.prototipotesis.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.example.prototipotesis.ml.BoundingBox;

import com.example.prototipotesis.ml.BoundingBox;

public class BoundingBoxOverlay extends View{
    private Paint pinturaCaja;
    private Paint pinturaTexto;
    private RectF cajaActual;
    private String textoPlaca = ""; // variable temporal para mostrar caracteres encima de la caja

    public BoundingBoxOverlay(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);

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

    public void actualizarCaja(RectF caja) {
        this.cajaActual = caja;
        invalidate(); // fuerza redibujado
    }

    public void limpiar(){
        this.cajaActual = null;
        this.textoPlaca = "";
        invalidate();
    }

    public void setTextoPlaca(String texto){
        this.textoPlaca = texto;
        invalidate(); // redibuja el overlay
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cajaActual != null) {
            // dibujar rectangulo
            canvas.drawRect(cajaActual, pinturaCaja);

            // dibujar texto encima de la caja
            if (textoPlaca != null && !textoPlaca.isEmpty()){
                float posicionX = cajaActual.left;
                float posicionY = cajaActual.top - 10;

                // si esta muy arriba lo bajamos
                if (posicionY < 60){
                    posicionY = cajaActual.top - 10;
                }

                canvas.drawText(textoPlaca, posicionX, posicionY, pinturaTexto);
            }
        }
    }

}
