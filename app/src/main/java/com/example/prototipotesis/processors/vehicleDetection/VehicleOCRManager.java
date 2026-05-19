package com.example.prototipotesis.processors.vehicleDetection;

import android.graphics.Bitmap;
import android.util.Log;

import com.example.prototipotesis.ml.ocr.OCRStabilizer;
import com.example.prototipotesis.processors.PlateProcessor;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.BitmapUtils;

public class VehicleOCRManager {

    private PlateProcessor plateProcessor;

    public VehicleOCRManager(PlateProcessor plateProcessor){
        this.plateProcessor = plateProcessor;
    }

    public void procesarOCRVehiculo(
            TrackedVehicle vehicle,
            Bitmap bitmapOriginal
    ){
        vehicle.framesSinceLastOcr++;

        // timeout OCR
        if(vehicle.ocrInProcess){
            long tiempo =
                    System.currentTimeMillis() - vehicle.ocrStartTime;

            if(tiempo > 1500) vehicle.ocrInProcess = false;
        }

        // controlar frecuencia OCR
        if(vehicle.ocrInProcess
                || vehicle.framesSinceLastOcr <= 10){
            return;
        }

        vehicle.ocrInProcess = true;
        vehicle.ocrStartTime = System.currentTimeMillis();
        vehicle.framesSinceLastOcr = 0;

        Bitmap bitmapVehiculo =
                BitmapUtils.recortarBitmap(
                        bitmapOriginal,
                        vehicle.boxModel
                );

        vehicle.vehicleBitmap = bitmapVehiculo;

        plateProcessor.detectPlateTextAsync(
                bitmapVehiculo,
                texto -> {
                    if(texto != null){
                        vehicle.plateRecord.add(texto);

                        if(vehicle.plateRecord.size() > TrackedVehicle.MAX_OCR_RECORD){
                            vehicle.plateRecord.remove(0);
                        }

                        String textoEstable =
                                OCRStabilizer.mostFrecuentText(
                                        vehicle.plateRecord
                                );

                        vehicle.plateText = textoEstable;
                    }

                    Log.d(
                            "OCR_DEBUG",
                            "Vehiculo "
                                    + vehicle.idVehicle
                                    + ": "
                                    + texto
                    );

                    vehicle.ocrInProcess = false;
                }
        );
    }
}
