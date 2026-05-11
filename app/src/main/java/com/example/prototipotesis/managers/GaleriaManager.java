package com.example.prototipotesis.managers;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;

public class GaleriaManager {

    // launcher que recibe resultado
    private ActivityResultLauncher<Intent> launcher;

    // interfaz para devolver el archivo seleccionado
    public interface OnMediaSeleccionadaListener{
        void onMediaSeleccionada(
                Uri uri,
                boolean esVideo
        );
    }

    // constructor
    public GaleriaManager(
            ActivityResultLauncher<Intent> launcher
    ){
        this.launcher = launcher;
    }

    // abrir galeria
    public void abrirGaleria(){

        Intent intent = new Intent(
                Intent.ACTION_GET_CONTENT
        );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "image/*",
                        "video/*"
                }
        );

        launcher.launch(intent);
    }
}