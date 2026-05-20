package com.example.prototipotesis.overlay;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.prototipotesis.utils.Dibujo;

import java.util.ArrayList;
import java.util.List;

public class PolygonOverlay extends View {

    // vértices del polígono
    private List<PointF> vertices = new ArrayList<>();

    // clase dibujadora
    private Dibujo dibujo;

    // variables para poder mover los botones
    private int puntoSeleccionado = -1;
    private static final float RADIO_TOUCH = 60f;

    public PolygonOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.dibujo = new Dibujo();
    }

    // actualizar vertices
    public void setVertices(List<PointF> vertices){
        this.vertices = vertices;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(vertices == null) return;
        if(vertices.size() < 2) return;

        // dibujar poligono
        dibujo.dibujarPoligono(canvas, vertices, 0);

        // dibujar lineas
        dibujo.dibujarLineas(canvas, vertices, 0);

        // dibujar puntos
        dibujo.dibujarPuntos(canvas, vertices, 0);
    }

    public void clear(){
        vertices.clear();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                if(puntoSeleccionado != -1){
                    vertices.get(puntoSeleccionado).x = Math.max(0, Math.min(x, getWidth()));
                    vertices.get(puntoSeleccionado).y = Math.max(0, Math.min(y, getHeight()));
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                puntoSeleccionado = -1;
                break;
        }

        return true;
    }

    private int obtenerPuntoCercano(float x, float y) {
        for (int i = 0; i < vertices.size(); i++) {
            PointF p = vertices.get(i);
            float dx = p.x - x;
            float dy = p.y - y;
            float distancia = (float)Math.sqrt(dx * dx + dy * dy);

            if (distancia < RADIO_TOUCH) return i;
        }
        return -1;
    }
}