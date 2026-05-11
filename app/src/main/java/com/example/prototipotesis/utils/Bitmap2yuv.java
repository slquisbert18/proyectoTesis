package com.example.prototipotesis.utils;

import android.graphics.Bitmap;

public class Bitmap2yuv { // BITMAP -> I420 (YUV420)
    public static byte[] bitmap2I420(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int frameSize = width * height;
        int[] argb = new int[frameSize];

        bitmap.getPixels(
                argb,
                0,
                width,
                0,
                0,
                width,
                height
        );

        byte[] yuv = new byte[frameSize + (frameSize / 4) + (frameSize / 4)];
        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + (frameSize / 4);

        int R, G, B, Y, U, V;

        int index = 0;

        for(int j = 0; j < height; j++){
            for(int i = 0; i < width; i++){
                int color = argb[index++];

                R = (color >> 16) & 0xff;
                G = (color >> 8) & 0xff;
                B = color & 0xff;

                // RGB -> YUV
                Y = ( (66 * R + 129 * G + 25 * B + 128) >> 8 ) + 16;
                U = ( (-38 * R - 74 * G + 112 * B + 128) >> 8 ) + 128;
                V = ( (112 * R - 94 * G - 18 * B + 128) >> 8 ) + 128;

                Y = clamp(Y);
                U = clamp(U);
                V = clamp(V);

                // Y
                yuv[yIndex++] = (byte) Y;

                // U y V cada 2x2
                if(j % 2 == 0 && i % 2 == 0){
                    yuv[uIndex++] = (byte) U;
                    yuv[vIndex++] = (byte) V;
                }
            }
        }

        return yuv;
    }

    private static int clamp(int value){
        return Math.max(
                0,
                Math.min(255, value)
        );
    }

    // CONVERSION RGB -> YUV420
    private static void encodeYUV420SP(
            byte[] yuv420sp,
            int[] argb,
            int width,
            int height
    ){

        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int R, G, B, Y, U, V;

        int index = 0;

        for(int j = 0; j < height; j++){

            for(int i = 0; i < width; i++){

                int color = argb[index++];

                R = (color >> 16) & 0xff;
                G = (color >> 8) & 0xff;
                B = color & 0xff;

                // conversion YUV
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // limitar rango
                Y = Math.max(0, Math.min(255, Y));
                U = Math.max(0, Math.min(255, U));
                V = Math.max(0, Math.min(255, V));

                // componente Y
                yuv420sp[yIndex++] = (byte) Y;

                // UV cada 2x2 pixeles
                if(j % 2 == 0 && i % 2 == 0){

                    yuv420sp[uvIndex++] = (byte) U;
                    yuv420sp[uvIndex++] = (byte) V;
                }
            }
        }
    }

    public static byte[] bitmap2NV12(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int frameSize = width * height;

        int[] argb = new int[frameSize];

        bitmap.getPixels(
                argb,
                0,
                width,
                0,
                0,
                width,
                height
        );

        byte[] yuv = new byte[frameSize + (frameSize / 2)];

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;

        int index = 0;

        for(int j = 0; j < height; j++){
            for(int i = 0; i < width; i++){
                int color = argb[index++];

                R = (color >> 16) & 0xff;
                G = (color >> 8) & 0xff;
                B = color & 0xff;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                Y = clamp(Y);
                U = clamp(U);
                V = clamp(V);

                yuv[yIndex++] = (byte) Y;

                // NV12 = UVUVUV
                if(j % 2 == 0 && i % 2 == 0){

                    yuv[uvIndex++] = (byte) U;
                    yuv[uvIndex++] = (byte) V;
                }
            }
        }
        return yuv;
    }
}
