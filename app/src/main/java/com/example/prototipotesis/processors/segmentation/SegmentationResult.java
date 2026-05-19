package com.example.prototipotesis.processors.segmentation;

/*
* RESULTADO DE LA SEGMENTACION
* */
public class SegmentationResult {

    // detecciones yolo
    public float[][][] detections;

    // protomasks
    public float[][][][] protos;

    public SegmentationResult(
            float[][][] detections,
            float[][][][] protos
    ) {

        this.detections = detections;
        this.protos = protos;
    }
}