package com.example.prototipotesis.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
* clase utilitaria para operaciones sobre imagenes
* antes de pasarlas al ocr
* */
public class ImageUtils {
    // 1. validar bitmap
    public static boolean bitmapValido(Bitmap bitmap){
        return bitmap != null
                && bitmap.getWidth() > 0
                && bitmap.getHeight() > 0;
    }

    // 2. rotar imagen segun su exif
    public static Bitmap corregirOrientiacion(String rutaImagen, Bitmap bitmap){
        try{
            ExifInterface exif = new ExifInterface(rutaImagen);
            int orientacion = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            int grados = obtenerGradosDeExif(orientacion);
            return rotarBitmap(bitmap, grados);

        }
        catch(IOException e){
            Log.e("imageUtils", "Error leyendo Exif", e);
            return bitmap;
        }
    }

    private static int obtenerGradosDeExif(int orientacion){
        switch(orientacion){
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }
    private static Bitmap rotarBitmap(Bitmap bitmap, int grados){
        if (grados == 0) return bitmap;
        Matrix matriz = new Matrix();
        matriz.postRotate(grados);

        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matriz, true
        );
    }

    // convertir la imagen a escala de grises
    public static Bitmap color2gray(Bitmap imagenOriginal){
        Bitmap imagenGris = Bitmap.createBitmap(
                imagenOriginal.getWidth(),
                imagenOriginal.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas lienzo = new Canvas(imagenGris);
        Paint pincel = new Paint();

        // matriz que elimina la saturacion (color)
        ColorMatrix matrizColor = new ColorMatrix();
        matrizColor.setSaturation(0);

        ColorMatrixColorFilter filtroGris = new ColorMatrixColorFilter(matrizColor);
        pincel.setColorFilter(filtroGris);
        lienzo.drawBitmap(imagenOriginal, 0, 0, pincel);

        return imagenGris;
    }
    /*
    * aumenta el contraste de la imagen (valor > 1.0 = mas contraste)
    * */
    public static Bitmap aumentarContraste(
            Bitmap imagenOriginal,
            float nivelContraste
    ){
        Bitmap imagenContraste = Bitmap.createBitmap(
                imagenOriginal.getWidth(),
                imagenOriginal.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas lienzo = new Canvas(imagenContraste);
        Paint pincel = new Paint();

        float escala = nivelContraste;
        float desplazamiento = (-0.5f * escala + 0.5f) * 255;

        ColorMatrix matrizContraste = new ColorMatrix(new float[]{
                escala, 0, 0, 0, desplazamiento,
                0, escala, 0, 0, desplazamiento,
                0, 0, escala, 0, desplazamiento,
                0, 0, 0, 1, 0
        });
        pincel.setColorFilter(new ColorMatrixColorFilter(matrizContraste));

        lienzo.drawBitmap(imagenOriginal, 0, 0, pincel);
        return imagenContraste;
    }

    /**
     * Binariza la imagen (blanco y negro)
     * -> util cuando el fondo es claro y los caracteres ocultos
     */
    public static Bitmap binarizar(Bitmap imagenOriginal){
        int ancho = imagenOriginal.getWidth();
        int alto = imagenOriginal.getHeight();

        Bitmap imagenBinaria = Bitmap.createBitmap(
                ancho, alto, Bitmap.Config.ARGB_8888
        );

        for(int y = 0; y < alto ; y++){
            for (int x = 0 ; x < ancho ; x++){
                int pixel = imagenOriginal.getPixel(x, y);

                int rojo = Color.red(pixel);
                int verde = Color.green(pixel);
                int azul = Color.blue(pixel);

                int gris = (rojo + verde + azul) / 3;

                // umbral (threshold)
                if(gris > 150){
                    imagenBinaria.setPixel(x, y, Color.WHITE);
                }
                else{
                    imagenBinaria.setPixel(x, y, Color.BLACK);
                }
            }
        }
        return imagenBinaria;
    }

    /**
     * escala la imagen SOLO si es pequena
     * evita degradacion innecesaria
     */
    public static Bitmap escalar(Bitmap imagen, int anchoMinimo){
        if (!bitmapValido(imagen)){
            return imagen;
        }
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();

        if(ancho >= anchoMinimo){
            return imagen; // no escalar
        }
        float factorEscala = (float)anchoMinimo/ancho;

        int nuevoAncho = anchoMinimo;
        int nuevoAlto = Math.round(alto * factorEscala);

        return Bitmap.createScaledBitmap(
                imagen, nuevoAncho, nuevoAlto, true
        );
    }

    public static Bitmap convertirABitmapEditable(Bitmap bitmap){
        // Si ya es editable, lo devolvemos tal cual
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            return bitmap;
        }

        // Creamos un bitmap nuevo en ARGB_8888
        Bitmap bitmapEditable = bitmap.copy(
                Bitmap.Config.ARGB_8888,
                true // editable
        );

        return bitmapEditable;
    }

    public static ByteBuffer bitmap2bytebuffer(Bitmap bitmapOriginal, int inputSize, int colorChannels){
        // conversion segura del bitmap
        Bitmap bitmapSeguro = convertirABitmapEditable(bitmapOriginal);

        // 4 bits por float
        int bytesPorCanal = 4;

        // tamanio total del buffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(
                inputSize * inputSize * colorChannels * bytesPorCanal
        );

        // usar el orden de bytes nativo del dispositivo
        buffer.order(ByteOrder.nativeOrder());

        // obtener los pixeles de la imagen en un array
        int[] pixeles = new int[inputSize * inputSize];
        bitmapSeguro.getPixels(
                pixeles,
                0,
                bitmapSeguro.getWidth(),
                0,
                0,
                bitmapSeguro.getWidth(),
                bitmapSeguro.getHeight()
        );

        int indicePixel = 0;

        // recorrer cada pixel y normalizarlo
        for(int fila = 0; fila < inputSize; fila++){
            for(int columna = 0; columna < inputSize; columna++){
                int pixel = pixeles[indicePixel++];

                // extraer componentes RGB y normalizar (0-1)
                float rojo = ((pixel >> 16) & 0xFF) / 255.0f;
                float verde = ((pixel >> 8) & 0xFF) / 255.0f;
                float azul = (pixel & 0xFF) / 255.0f;

                buffer.putFloat(rojo);
                buffer.putFloat(verde);
                buffer.putFloat(azul);
            }
        }

        return buffer;
    }

}
