package com.example.prototipotesis.processors;

import android.graphics.Bitmap;

import com.example.prototipotesis.render.RenderizadorDetecciones;
import com.example.prototipotesis.trackedObject.TrackedVehicle;

import java.util.List;

public class FrameProcessor {
    private VehicleProcessor vehicleProcessor;
    private RenderizadorDetecciones renderizador;

    public FrameProcessor(VehicleProcessor vp) {
        this.vehicleProcessor = vp;
        this.renderizador = new RenderizadorDetecciones();
    }

    public Bitmap process(Bitmap frame, int ancho, int alto) {
        List<TrackedVehicle> vehicles =
                vehicleProcessor.processFrame(frame, ancho, alto);

        return renderizador.dibujarDetecciones(frame, vehicles, true);
    }

    public List<TrackedVehicle> getVehicles(Bitmap frame, int w, int h) {
        return vehicleProcessor.processFrame(frame, w, h);
    }
}
