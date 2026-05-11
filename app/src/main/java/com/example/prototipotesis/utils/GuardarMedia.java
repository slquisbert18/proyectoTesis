package com.example.prototipotesis.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

public class GuardarMedia {

    public static File guardarImagenProcesada(Context context, Bitmap bitmap){
        try{
            File carpeta = new File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "Capturas"
            );
            if(!carpeta.exists()) carpeta.mkdirs();

            String nombre = "GALERIA_IMG_" + System.currentTimeMillis() + ".jpg";
            File archivo = new File(carpeta, nombre);
            FileOutputStream fos = new FileOutputStream(archivo);

            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    95,
                    fos
            );

            fos.flush();
            fos.close();
            return archivo;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
