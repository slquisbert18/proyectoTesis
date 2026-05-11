package com.example.prototipotesis.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.example.prototipotesis.ui.ArchivoHistorial;

import java.io.File;

// funciones de borrado, editado y compartir
public class HistorialUtils {

    public static void compartirArchivo(Context context, ArchivoHistorial archivo){
        try{
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    archivo.archivo
            );
            Intent intentCompartir = new Intent(
                    Intent.ACTION_SEND
            );

            // tipo MIME
            if(archivo.esVideo){
                intentCompartir.setType("video/*");
            }
            else{
                intentCompartir.setType("image/*");
            }

            intentCompartir.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            intentCompartir.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            context.startActivity(
                    Intent.createChooser(
                            intentCompartir,
                            "Compartir archivo"
                    )
            );
        }catch(Exception e){
            Toast.makeText(
                    context,
                    "Error compartiendo archivo",
                    Toast.LENGTH_SHORT
            ).show();
            e.printStackTrace();
        }
    }

    // ELIMINAR ARCHIVO
    public static boolean eliminarArchivo(ArchivoHistorial archivo) {
        return archivo.archivo.delete();
    }

    // RENOBRAR ARCHIVO
    public static boolean renombrarArchivo(ArchivoHistorial archivo, String nuevoNombre){
        // nombre actual SIN extension
        String nombreActual = archivo.archivo.getName();
        int punto = nombreActual.lastIndexOf(".");

        String extension = (punto > 0) ? nombreActual.substring(punto) : "";

        // agregar extension original
        nuevoNombre = nuevoNombre + extension;

        File nuevoArchivo = new File(
                archivo.archivo
                        .getParent(),
                nuevoNombre
        );

        boolean renombrado = archivo.archivo.renameTo(nuevoArchivo);

        if(renombrado) {
            // actualizar referencia
            archivo.archivo = nuevoArchivo;
        }
        return renombrado;
    }
}
