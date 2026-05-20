package com.example.prototipotesis.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.List;

public class Dibujo {

    // pintura líneas
    private Paint paintLine;

    // pintura puntos
    private Paint paintPoint;

    // pintura relleno
    private Paint paintFill;

    // pintura cajas
    private Paint paintBox;

    // pintura texto
    private Paint paintText;

    public Dibujo(){
        // lineas
        paintLine = new Paint();
        paintLine.setColor(Color.GREEN);
        paintLine.setStrokeWidth(6f);
        paintLine.setStyle(Paint.Style.STROKE);

        // puntos
        paintPoint = new Paint();
        paintPoint.setColor(Color.YELLOW);
        paintPoint.setStyle(Paint.Style.FILL);

        // relleno
        paintFill = new Paint();
        paintFill.setColor(Color.YELLOW);
        paintFill.setAlpha(80);
        paintFill.setStyle(Paint.Style.FILL);

        // boundign boxes
        paintBox = new Paint();
        paintBox.setStrokeWidth(5.0f);
        paintBox.setColor(Color.DKGRAY);
        paintBox.setStyle(Paint.Style.STROKE);
        paintBox.setAntiAlias(true);


        // texto
        paintText = new Paint();
        paintText.setColor(Color.GREEN);
        paintText.setTextSize(50f);
        paintText.setStyle(Paint.Style.FILL);
        paintText.setFakeBoldText(true);

    }

    private Path creacionPath(List<PointF> vertices){
        Path path = new Path();
        PointF first = vertices.get(0);
        path.moveTo(first.x, first.y);

        for(int i = 1; i < vertices.size(); i++){
            PointF p = vertices.get(i);
            path.lineTo(p.x, p.y);
        }
        path.close();

        return path;
    }

    public void dibujarPoligono(Canvas canvas, List<PointF> vertices, int color){
        Path path = creacionPath(vertices);
        // si se tiene color enviado como parametro
        if(color != 0) paintFill.setColor(color);

        canvas.drawPath(path, paintFill);
    }

    public void dibujarLineas(Canvas canvas, List<PointF> vertices, int color){
        Path path = creacionPath(vertices);
        // si no se tiene color
        if(color != 0) paintLine.setColor(color);

        canvas.drawPath(path, paintLine);
    }

    public void dibujarPuntos(Canvas canvas, List<PointF> vertices, int color){
        if(color != 0) paintPoint.setColor(color);

        for(PointF p : vertices){
            canvas.drawCircle(
                    p.x,
                    p.y,
                    14f,
                    paintPoint
            );
        }
    }

    public void dibujarRectangulo(Canvas canvas, RectF vehiculo, int color){
        if (color != 0) paintBox.setColor(color);
        canvas.drawRect(vehiculo, paintBox);
    }

    public void dibujarTexto(Canvas canvas, String texto, float x, float y, int color){
        if(color != 0) paintText.setColor(color);

        canvas.drawText(texto, x, y, paintText);
    }
}
