package com.example.prototipotesis.processors;

import org.tensorflow.lite.Interpreter;
import android.graphics.Bitmap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.HashMap;
import com.example.prototipotesis.processors.segmentation.SegmentationResult;
import com.example.prototipotesis.utils.BitmapUtils;
public class SegmentationProcessor {
    // intérprete tensorflow lite
    private Interpreter interprete;

    // tamaño de entrada del modelo
    private static final int INPUT_SIZE = 640;

    // cantidad de protomasks
    private static final int PROTO_CHANNELS = 32;

    // tamaño de salida de máscara
    private static final int MASK_SIZE = 160;

    public SegmentationProcessor(Interpreter interprete) throws Exception {
        this.interprete = interprete;
    }

    // ===============================
    // EJECUTAR SEGMENTACIÓN
    // ===============================
    public SegmentationResult segment(Bitmap bitmap) {

        // convertir bitmap a tensor
        ByteBuffer input = BitmapUtils.bitmap2bytebuffer(bitmap, INPUT_SIZE, 3);

        // salida detecciones
        float[][][] detections = new float[1][37][8400];

        // salida protomasks
        float[][][][] protos =
                new float[1][MASK_SIZE][MASK_SIZE][PROTO_CHANNELS];

        // inputs
        Object[] inputs = {input};

        // outputs
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, detections);
        outputs.put(1, protos);

        // ejecutar inferencia
        interprete.runForMultipleInputsOutputs(
                inputs,
                outputs
        );

        return new SegmentationResult(
                detections,
                protos
        );
    }
}
