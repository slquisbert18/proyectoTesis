package com.example.prototipotesis.processors;
import android.graphics.Bitmap;

import com.example.prototipotesis.detectors.VehicleDetector;
import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.ml.VehicleTracker;
import com.example.prototipotesis.ocr.OCRStabilizer;
import com.example.prototipotesis.trackedObject.TrackedPlate;
import com.example.prototipotesis.trackedObject.TrackedVehicle;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class VehicleProcessor {
    private VehicleDetector detector;
    private VehicleTracker tracker;
    private PlateProcessor plateProcessor;
    private static final float NMS_THRESOLD = 0.5f;
    private int framesWithoutVehicles = 0;

    // ************* PARA DETECCION DE INFRACCIONES ******************
    private List<PointF> verticesZone = new ArrayList<>();

    public VehicleProcessor(
            Interpreter interpreter,
            PlateProcessor plateProcessor){
        this.detector = new VehicleDetector(interpreter);
        this.plateProcessor = plateProcessor;
        tracker = new VehicleTracker();
    }

    public List<TrackedVehicle> processFrame(
            Bitmap originalBitmap,
            int previewWidth,
            int previewHeight
    ){
        Log.d("VEHICLE_PROCESSOR", "Procesando frame");

        float [][][] output = detector.detectVehicles(originalBitmap);

        List<BoundingBox> boxes = detector.getVehicles(output);

        // control de ausencia de vehiculos
        if(boxes == null || boxes.isEmpty()){
            framesWithoutVehicles++;

            if(framesWithoutVehicles > 5){
                tracker.reset();
            }
            return new ArrayList<>();
        }
        else{
            framesWithoutVehicles = 0;
        }

        // aplicamos NonMaximumSuppression para que el modelo se quede con
        // la mejor caja (una sola)
        boxes = nonMaxmimumSuppression(boxes);

        if(boxes.size() == 0){
            tracker.reset(); // reinicia todo si no hay vehiculos
            return new ArrayList<>();
        }

        List<RectF> previewBoxes = new ArrayList<>();
        List<BoundingBox> modelBoxes = new ArrayList<>();

        for(BoundingBox box : boxes){
            RectF previewRect = coordinates2preview(
                    box,
                    originalBitmap.getWidth(),
                    originalBitmap.getHeight(),
                    previewWidth,
                    previewHeight
            );

            previewBoxes.add(previewRect);
            modelBoxes.add(box);
        }

        //  tracking
        List<TrackedVehicle> trackedVehicles = tracker.update(previewBoxes, modelBoxes);

        if(trackedVehicles.isEmpty() && boxes.size() > 0){
            tracker.reset();
        }

        // procesar cada vehiculo
        for(TrackedVehicle vehicle : trackedVehicles){
            // ************** DETECCION DE INFRACCION ********************
            // usaremos la parte inferior del vehiculo
            float centroX = vehicle.box.centerX();
            float centroY = vehicle.box.bottom;

            // verificcar si el objeto esta dentro del poligono
            boolean isInZone = pointInsideZone(centroX, centroY);

            // detectar entrada a la zona (evento)
            if(!vehicle.inZone && isInZone){
                vehicle.detectedInfringment = true;
            }

            // actualizamos estado actual
            vehicle.inZone = isInZone;

            // ************** OPERACION OCR ********************
            vehicle.framesSinceLastOcr++;

            // control del timeout (evita bloqueo)
            if(vehicle.ocrInProcess){
                long time = System.currentTimeMillis() - vehicle.ocrStartTime;

                if(time > 1500){ // 1.5 segundos
                    vehicle.ocrInProcess = false;
                }
            }

            // ejecutaremos el ocr cada 10 frames
            // si hay vehiculo nuevo, se ejecuta el ocr
            // si hay un vehiculo con texto, hay ocr cada 10 rames
            // si el vehiculo no tiene texto, se insiste
            if(!vehicle.ocrInProcess &&
                    vehicle.framesSinceLastOcr > 10) {

                vehicle.ocrInProcess = true;
                vehicle.ocrStartTime = System.currentTimeMillis();
                vehicle.framesSinceLastOcr = 0;

                Bitmap croppedVehicle = detector.cutVehicle(
                        originalBitmap,
                        vehicle.boxModel
                );

                vehicle.vehicleBitmap = croppedVehicle;

                // ocr por vehiculo
                plateProcessor.detectPlateTextAsync(
                        croppedVehicle,
                        text -> {
                            if (text != null) {
                                // historial
                                vehicle.plateRecord.add(text);

                                if (vehicle.plateRecord.size() > TrackedVehicle.MAX_OCR_RECORD) {
                                    vehicle.plateRecord.remove(0);
                                }

                                // texto estable
                                String stable = OCRStabilizer.mostFrecuentText(vehicle.plateRecord);

                                vehicle.plateText = stable;
                            }
                            Log.d("DEBUG_OCR", "Vehiculo ID: " + vehicle.idVehicle + " Texto: " + text);
                            // liberar ocr
                            vehicle.ocrInProcess = false;
                        }
                );
            }
        }

        return trackedVehicles;
    }
    private List<BoundingBox> nonMaxmimumSuppression(List<BoundingBox> boxes){
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

                if(iou > NMS_THRESOLD){
                    removed[j] = true;
                }
            }
        }
        return result;
    }

    private float calculateIOU(BoundingBox a, BoundingBox b){
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

    public static RectF coordinates2preview(
            BoundingBox caja,
            int anchoOriginal,
            int altoOriginal,
            int anchoPreview,
            int altoPreview
    ){
        // coordenadas normalizadas del modelo (0 a 1)
        float centroX = caja.centroX * anchoOriginal;
        float centroY = caja.centroY * altoOriginal;
        float anchoCaja = caja.ancho * anchoOriginal;
        float altoCaja = caja.alto * altoOriginal;

        // convertimos a coordenadas absolutas del bitmap
        float izquierda = centroX - (anchoCaja / 2f);
        float arriba = centroY - (altoCaja / 2f);
        float derecha = centroX + (anchoCaja / 2f);
        float abajo = centroY + (altoCaja / 2f);

        // ahora escalamos al tamaño real del PreviewView
        float escalaX = (float) anchoPreview / (float) anchoOriginal;
        float escalaY = (float) altoPreview / (float) altoOriginal;

        izquierda *= escalaX;
        derecha *= escalaX;
        arriba *= escalaY;
        abajo *= escalaY;

        return new RectF(izquierda, arriba, derecha, abajo);
    }

    public void resetTracker(){
        tracker.reset();
    }
    public void setZone(List<PointF> vertices){
        this.verticesZone = vertices;
    }

    private boolean pointInsideZone(float x, float y){
        boolean dentro = false;

        if(verticesZone == null || verticesZone.size() < 3){
            return false;
        }

        for (int i = 0, j = verticesZone.size() - 1; i < verticesZone.size(); j = i++) {
            float xi = verticesZone.get(i).x;
            float yi = verticesZone.get(i).y;
            float xj = verticesZone.get(j).x;
            float yj = verticesZone.get(j).y;

            boolean intersecta = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if(intersecta) dentro = !dentro;
        }

        return dentro;
    }
}