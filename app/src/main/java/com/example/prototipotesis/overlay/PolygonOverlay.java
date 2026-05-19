package com.example.prototipotesis.overlay;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PolygonOverlay extends View {

    // vértices del polígono
    private List<PointF> vertices = new ArrayList<>();

    // pintura líneas
    private Paint paintLine;

    // pintura puntos
    private Paint paintPoint;

    // pintura relleno
    private Paint paintFill;

    public PolygonOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

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

        // rellenar poligono
        Path path = new Path();

        PointF first = vertices.get(0);

        path.moveTo(first.x, first.y);

        for(int i = 1; i < vertices.size(); i++){
            PointF p = vertices.get(i);
            path.lineTo(p.x, p.y);
        }
        path.close();

        canvas.drawPath(path, paintFill);

        // dibujar lineas
        canvas.drawPath(path, paintLine);

        // dibujar puntos
        for(PointF p : vertices){
            canvas.drawCircle(
                    p.x,
                    p.y,
                    14f,
                    paintPoint
            );
        }
    }
}