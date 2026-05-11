package com.example.prototipotesis.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class InfringmentZoneView extends View {
    private List<PointF> vertices = new ArrayList<>();
    private Paint paintLine;
    private Paint paintPoint;

    private int activePoint = -1;

    public InfringmentZoneView(Context context){
        super(context);
        init();
    }

    public InfringmentZoneView(Context context, AttributeSet attrs){
        super(context, attrs);
        init();
    }

    public InfringmentZoneView(Context context, AttributeSet attrs, int style){
        super(context, attrs, style);
        init();
    }

    private void init(){
        paintLine = new Paint();
        paintLine.setColor(Color.RED);
        paintLine.setStrokeWidth(5f);
        paintLine.setStyle(Paint.Style.STROKE);

        paintPoint = new Paint();
        paintPoint.setColor(Color.YELLOW);
        paintPoint.setStyle(Paint.Style.FILL);

    }
    // posiciona la figura tomando en cuenta el tamanio de la pantalla
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight){
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        // iniciamos si aun no hay vertices
        if(vertices.isEmpty()){
            float top = height * 0.7f;
            float bottom = height * 0.9f;
            float left = width * 0.2f;
            float right = width * 0.8f;

            vertices.add(new PointF(left, top));
            vertices.add(new PointF(right, top));
            vertices.add(new PointF(right, bottom));
            vertices.add(new PointF(left, bottom));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(vertices.size() < 2) return;

        // dibujamos las lineas
        for(int i = 0 ; i < vertices.size() ; i++){
            PointF p1 = vertices.get(i);
            PointF p2 = vertices.get((i + 1) % vertices.size());

            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintLine);
        }

        // dibujamos los puntos (verticees)
        for(PointF p : vertices){
            canvas.drawCircle(p.x, p.y, 20f, paintPoint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float x = event.getX();
        float y = event.getY();

        switch ( event.getAction()){
            case MotionEvent.ACTION_DOWN:
                activePoint = getCloserPoint(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                if(activePoint != -1){

                    // limitamos el movimiento de los puntos (solo dentro la pantalla)
                    vertices.get(activePoint).x = Math.max(0, Math.min(x, getWidth()));
                    vertices.get(activePoint).y = Math.max(0, Math.min(y, getHeight()));

                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                activePoint = -1;
                break;
        }
        return true;
    }

    private int getCloserPoint(float x, float y){
        for(int i = 0 ; i < vertices.size() ; i++){
            PointF p = vertices.get(i);
            float dx = p.x - x;
            float dy = p.y - y;

            if(Math.sqrt(dx * dx + dy * dy) < 50){
                return i;
            }
        }
        return -1;
    }

    public List<PointF> getVertices(){
        return vertices;
    }

    public void setVertices(List<PointF> newVertices){
        newVertices = sortPolygons(newVertices);
        if(newVertices == null || newVertices.size() < 4){
            return;
        }

        vertices.clear();

        // copiamos los nuevos puntos
        for(PointF p : newVertices) {
            Log.d("VERTEX", "x: " + p.x + " y: " + p.y);
            vertices.add(new PointF(p.x, p.y));
        }

        invalidate();
    }

    // ayuda a evitar deformaciones raras
    private List<PointF> sortPolygons(List<PointF> puntos){

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
}
