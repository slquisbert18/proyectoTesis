package com.example.prototipotesis.camara;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GestorCamara {
    private AppCompatActivity actividad; // activity que usa la camara
    private PreviewView vistaPrevia; // aca se mostrara las imagenes recogidas por la camara
    private ProcessCameraProvider proveedorCamara; // administra el ciclo de vida de la camara
    private ExecutorService ejecutorAnalisis;

    private AnalizadorFrames analizadorFrames;

    public GestorCamara(AppCompatActivity actividad,
                        PreviewView vistaPrevia,
                        AnalizadorFrames analizadorFrames){
        this.actividad = actividad;
        this.vistaPrevia = vistaPrevia;
        this.analizadorFrames = analizadorFrames;
        this.ejecutorAnalisis = Executors.newSingleThreadExecutor();
    }

    // verificamos permisos
    public void verificarPermisos(){
        if (ContextCompat.checkSelfPermission(
                actividad,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            iniciarCamara();
        }
        else{
            ActivityCompat.requestPermissions(
                    actividad,
                    new String[]{Manifest.permission.CAMERA},
                    100
            );
        }
    }

    public void manejarRespuestaPermiso(int codigo,
                                        @NonNull int[] resultados){
        if (codigo == 100 &&
            resultados.length > 0 &&
            resultados[0] == PackageManager.PERMISSION_GRANTED){

            iniciarCamara();
        }
    }

    // iniciar camaraX
    private void iniciarCamara(){
        ListenableFuture<ProcessCameraProvider> futuroProveedor = ProcessCameraProvider.getInstance(actividad);
        futuroProveedor.addListener(() -> {
            try{
                proveedorCamara = futuroProveedor.get();
                mostrarPreview();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(actividad));
    }

    // mostrar preview
    private void mostrarPreview(){
        Preview preview = new Preview.Builder().build();
        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

        preview.setSurfaceProvider(vistaPrevia.getSurfaceProvider());

        // analisis de imagenes mostradas en el previewView
        ImageAnalysis analisisImagen =
                new ImageAnalysis.Builder().
                        setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();

        analisisImagen.setAnalyzer(
                ejecutorAnalisis,
                analizadorFrames
        );
        proveedorCamara.unbindAll();

        proveedorCamara.bindToLifecycle(
                actividad,
                selector,
                preview,
                analisisImagen
        );
    }

    public void liberarRecursos(){
        if(ejecutorAnalisis != null){
            ejecutorAnalisis.shutdown();
        }
    }
}
