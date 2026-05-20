package com.example.prototipotesis.processors.segmentation;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

public class MaskScaleUtils {

    public static List<PointF> escalarVertices(
            List<PointF> vertices,
            int maskWidth,
            int maskHeight,
            int targetWidth,
            int targetHeight
    ){

        List<PointF> resultado = new ArrayList<>();

        float scaleX = targetWidth / (float) maskWidth;

        float scaleY = targetHeight / (float) maskHeight;

        for(PointF p : vertices){
            resultado.add(
                    new PointF(
                            p.x * scaleX,
                            p.y * scaleY
                    )
            );
        }

        return resultado;
    }
}