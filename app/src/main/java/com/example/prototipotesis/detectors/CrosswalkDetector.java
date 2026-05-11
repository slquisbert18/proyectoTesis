package com.example.prototipotesis.detectors;

import android.graphics.Bitmap;
import android.graphics.PointF;

import com.example.prototipotesis.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class CrosswalkDetector {

    private Interpreter interpreter;
    private static final int INPUT_SIZE = 640;
    private static final int COLOR_CHANNELS = 3;
    private static final int DETECTIONS = 8400;
    private static final int ATTRIBUTES = 37;
    public static final float CONF_THRESOLD = 0.6f;

    public CrosswalkDetector(Interpreter interpreter){
        this.interpreter = interpreter;
    }

    public float[][][] detectCrosswalk(Bitmap bitmap){
        Bitmap resized = Bitmap.createScaledBitmap(
                bitmap,
                INPUT_SIZE,
                INPUT_SIZE,
                true
        );

        // convertimos bitmap a bytebuffer
        ByteBuffer input = ImageUtils.bitmap2bytebuffer(
                resized,
                INPUT_SIZE,
                COLOR_CHANNELS
        );

        float[][][] output = new float[1][ATTRIBUTES][DETECTIONS];

        // ejecucion de inferencis
        interpreter.run(input, output);
        return output;
    }

    public List<List<PointF>> getPolygons(float[][][] output){

        Log.d("CROSSWALK_RAW_MODEL", "x ejemplo: " + output[0][5][0]);
        Log.d("CROSSWALK_RAW_MODEL", "y ejemplo: " + output[0][6][0]);

        List<List<PointF>> polygons = new ArrayList<>();

        for(int i = 0 ; i < DETECTIONS ; i++) {

            float conf = sigmoid(output[0][4][i]);

            Log.d("DEBUG", "conf ejemplo: " + conf);

            if (conf < CONF_THRESOLD) {
                continue;
            }

            List<PointF> polygon = new ArrayList<>();


            for (int j = 5; j < ATTRIBUTES; j += 2) {
                float x = sigmoid(output[0][j][i]);
                float y = sigmoid(output[0][j + 1][i]);

                polygon.add(new PointF(x, y));
            }
            polygons.add(polygon);
        }
        return polygons;
    }

    public List<PointF> escalatePolygon(List<PointF> polygon, int width, int height){
        List<PointF> escalated = new ArrayList<>();

        for(PointF p : polygon){
            escalated.add(new PointF(
                    p.x * width,
                    p.y * height
            ));
        }

        return escalated;
    }

    public List<PointF> getBestCrosswalk(List<List<PointF>> polygons){
        if(polygons.isEmpty()){
            return null;
        }
        return polygons.get(0);
    }
    private float sigmoid(float x){
        return (float)(1.0 / (1.0 + Math.exp(-x)));
    }

}
