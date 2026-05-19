package com.example.prototipotesis.processors.vehicleDetection;

import android.graphics.Bitmap;

import com.example.prototipotesis.utils.BitmapUtils;
import com.example.prototipotesis.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;

public class VehicleDetector {
    private Interpreter interpreter;
    private static final int INPUT_SIZE = 320;
    private static final int COLOR_CHANNELS = 3;
    private static final float CONF_THRESOLD = 0.5f;

    public VehicleDetector(Interpreter interpreter){
        this.interpreter = interpreter;
    }

    public float[][][] detectarVehiculos(Bitmap bitmap){
        Bitmap bitmapRedimensionado =
                BitmapUtils.redimensionarBitmap(bitmap, INPUT_SIZE, INPUT_SIZE);

        // convertimos bitmap a byteBuffer
        ByteBuffer input =
                ImageUtils.bitmap2bytebuffer(bitmapRedimensionado, INPUT_SIZE, COLOR_CHANNELS);

        //salida tipica de YOLO: [1][cantidadDetecciones][atributos]
        float[][][] output = new float[1][6300][85];
        // 6300: numero de detecciones
        // 85 = (x, y, w, z, confianza, clases [en este caso tenemos 80 clases])

        // ejecutar inferencia
        interpreter.run(input, output);

        return output;
    }
}
