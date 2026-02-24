package com.example.prototipotesis.utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class yuv2rgb {
    private final Context contexto;

    public yuv2rgb(Context contexto){
        this.contexto = contexto;
    }

    public Bitmap convertir(@NonNull Image image){
        int ancho = image.getWidth();
        int alto = image.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(
                ancho,
                alto,
                Bitmap.Config.ARGB_8888
        );

        formatoYuv2rgb(image, bitmap);

        return bitmap;
    }

    private void formatoYuv2rgb(Image image, Bitmap bitmap){
        Image.Plane[] planos = image.getPlanes();
        ByteBuffer bufferY = planos[0].getBuffer();
        ByteBuffer bufferU = planos[1].getBuffer();
        ByteBuffer bufferV = planos[2].getBuffer();

        int strideY = planos[0].getRowStride();
        int strideUV = planos[1].getRowStride();
        int pixelStrideUV = planos[1].getPixelStride();

        int ancho = image.getWidth();
        int alto = image.getHeight();

        int[] pixeles = new int[ancho * alto];

        byte[] yBytes = new byte[bufferY.remaining()];
        byte[] uBytes = new byte[bufferU.remaining()];
        byte[] vBytes = new byte[bufferV.remaining()];

        bufferY.get(yBytes);
        bufferU.get(uBytes);
        bufferV.get(vBytes);

        int indice = 0;

        for(int fila = 0; fila < alto; fila++){

            int filaY = strideY * fila;
            int filaUV = strideUV * (fila / 2);

            for(int columna = 0; columna < ancho; columna++){

                int y = yBytes[filaY + columna] & 0xFF;

                int uvOffset = filaUV + (columna / 2) * pixelStrideUV;

                int u = uBytes[uvOffset] & 0xFF;
                int v = vBytes[uvOffset] & 0xFF;

                int r = (int)(y + 1.370705f * (v - 128));
                int g = (int)(y - 0.337633f * (u - 128) - 0.698001f * (v - 128));
                int b = (int)(y + 1.732446f * (u - 128));

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                pixeles[indice++] =
                        0xFF000000 |
                                (r << 16) |
                                (g << 8) |
                                b;
            }
        }

        bitmap.setPixels(
                pixeles,
                0,
                ancho,
                0,
                0,
                ancho,
                alto
        );
    }
}
