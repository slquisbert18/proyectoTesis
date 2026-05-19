package com.example.prototipotesis.utils;

import com.example.prototipotesis.ml.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class DetectionUtils {
    public static List<BoundingBox> nonMaxmimumSuppression(List<BoundingBox> boxes, float nmsThresold){
        List<BoundingBox> result = new ArrayList<>();

        // ordenamos por confianza
        boxes.sort((a, b) -> Float.compare(b.confianza, a.confianza));

        boolean[] removed = new boolean[boxes.size()];

        for(int i = 0 ; i < boxes.size() ; i++){
            if(removed[i]){
                continue;
            }
            // current = actual
            BoundingBox current = boxes.get(i);
            result.add(current);

            for(int j = i + 1 ; j < boxes.size() ; j++){
                if(removed[j]){
                    continue;
                }

                BoundingBox other = boxes.get(j);

                float iou = calculateIOU(current, other);

                if(iou > nmsThresold){
                    removed[j] = true;
                }
            }
        }
        return result;
    }

    public static float calculateIOU(BoundingBox a, BoundingBox b){
        float ax1 = a.centroX - a.ancho/2;
        float ay1 = a.centroY - a.alto/2;
        float ax2 = a.centroX + a.ancho/2;
        float ay2 = a.centroY + a.alto/2;

        float bx1 = b.centroX - b.ancho/2;
        float by1 = b.centroY - b.alto/2;
        float bx2 = b.centroX + b.ancho/2;
        float by2 = b.centroY + b.alto/2;

        float interX1 = Math.max(ax1, bx1);
        float interY1 = Math.max(ay1, by1);
        float interX2 = Math.min(ax2, bx2);
        float interY2 = Math.min(ay2, by2);

        float interW = Math.max(0, interX2 - interX1);
        float interH = Math.max(0, interY2 - interY1);

        float areaInter = interW * interH;

        float areaA = (ax2 - ax1) * (ay2 - ay1);
        float areaB = (bx2 - bx1) * (by2 - by1);

        float areaUnion = areaA + areaB - areaInter;

        if(areaUnion <= 0) return 0f;

        return areaInter / areaUnion;
    }
}
