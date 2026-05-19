package com.example.prototipotesis.processors;
import android.graphics.Bitmap;

import com.example.prototipotesis.processors.vehicleDetection.VehicleDetector;
import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.ml.VehicleTracker;
import com.example.prototipotesis.processors.vehicleDetection.VehicleDetectorParser;
import com.example.prototipotesis.processors.vehicleDetection.VehicleOCRManager;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.CoordinateUtils;
import com.example.prototipotesis.utils.DetectionUtils;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.List;

import android.graphics.RectF;
import android.util.Log;

public class VehicleProcessor {
    private VehicleDetector detector;
    private VehicleDetectorParser parserVehiculos;
    private VehicleOCRManager ocrManager;
    private VehicleTracker tracker;
    private static final float NMS_THRESOLD = 0.5f;

    public VehicleProcessor(
            Interpreter interpreter,
            PlateProcessor plateProcessor
    ){
        this.detector = new VehicleDetector(interpreter);
        tracker = new VehicleTracker();
        parserVehiculos = new VehicleDetectorParser();
        ocrManager = new VehicleOCRManager(plateProcessor);
    }

    public List<TrackedVehicle> processFrame(
            Bitmap originalBitmap,
            int previewWidth,
            int previewHeight
    ){
        Log.d("VEHICLE_PROCESSOR", "Procesando frame");

        float [][][] output = detector.detectarVehiculos(originalBitmap);

        List<BoundingBox> boxes = parserVehiculos.obtenerVehiculos(output);

        // aplicamos NonMaximumSuppression para que el modelo se quede con
        // la mejor caja (una sola)
        boxes = DetectionUtils.nonMaxmimumSuppression(boxes, NMS_THRESOLD);

        if(boxes.isEmpty()){
            tracker.reset(); // reinicia todo si no hay vehiculos
            return new ArrayList<>();
        }

        List<RectF> previewBoxes = new ArrayList<>();

        for(BoundingBox box : boxes){
            previewBoxes.add(
                    CoordinateUtils.coordinates2preview(
                            box,
                            originalBitmap.getWidth(),
                            originalBitmap.getHeight(),
                            previewWidth,
                            previewHeight
                    )
            );
        }

        //  tracking
        List<TrackedVehicle> trackedVehicles = tracker.update(previewBoxes, boxes);

        // procesar cada vehiculo
        for(TrackedVehicle vehicle : trackedVehicles){
            // procesamiento ocr
            ocrManager.procesarOCRVehiculo(
                    vehicle,
                    originalBitmap
            );

        }

        return trackedVehicles;
    }
    public void resetTracker(){
        tracker.reset();
    }

}