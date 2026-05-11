package com.example.prototipotesis.ui;

import java.io.File;

public class ArchivoHistorial {
    public File archivo;
    public boolean esVideo;

    public ArchivoHistorial(File archivo, boolean esVideo){
        this.archivo = archivo;
        this.esVideo = esVideo;
    }
}
