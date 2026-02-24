package com.example.prototipotesis.ml;
import static com.example.prototipotesis.utils.ImageUtils.convertirABitmapEditable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
* la clase recibe una imagen (bitmap)
* prepara la imagen para el modelo (preprocesamiento)
* ejecuta la inferencia
* devuelve los resultados del modelo
* */

public class DetectorPlacas {
    private Interpreter interprete;

    // tamanio esperado de la imagen de entrada del modelo
    private static final int TAMANIO_ENTRADA = 960;
    // cantidad de canales de color (RGB=3)
    private static final int CANALES_COLOR = 3;

    public DetectorPlacas(Interpreter interprete){
        this.interprete = interprete;
    }

    /**
     * ejecuta la inferencia del modelo yolo
     */
    public float[][][] detectarPlacas(Bitmap imagen){
        // 1. redimension de la imagen
        Bitmap imagenResize = Bitmap.createScaledBitmap(
                imagen, TAMANIO_ENTRADA, TAMANIO_ENTRADA, true
        );

        // 2. convertir Bitmap a ByteBuffer
        ByteBuffer entradaModelo = bitmap2bytebuffer(imagenResize);

        // 3. crear estructura de salida
        /*
         * salida tipica de YOLO: [1][cantidadDetecciones][atributos]
         * */
        float[][][] salidaModelo = new float[1][5][18900];
        // 25200: numero de detecciones
        // 85 = (x, y, w, z, confianza, clases [en este caso tenemos 1 clase])

        // 4. ejecutar inferencia
        interprete.run(entradaModelo, salidaModelo);

        return salidaModelo;
    }

    /*
    * preprocesamiento de la imagen: redimensiona, normaliza pixeles y convierte a byteBuffer
    * */

    private ByteBuffer bitmap2bytebuffer(Bitmap bitmapOriginal){
        // conversion degura del bitmap
        Bitmap bitmapSeguro = convertirABitmapEditable(bitmapOriginal);

        // 4 bits por float
        int bytesPorCanal = 4;

        // tamanio total del buffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(
                TAMANIO_ENTRADA * TAMANIO_ENTRADA * CANALES_COLOR * bytesPorCanal
        );

        // usar el orden de bytes nativo del dispositivo
        buffer.order(ByteOrder.nativeOrder());

        // obtener los pixeles de la imagen en un array
        int[] pixeles = new int[TAMANIO_ENTRADA * TAMANIO_ENTRADA];
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
        for(int fila = 0; fila < TAMANIO_ENTRADA; fila++){
            for(int columna = 0; columna < TAMANIO_ENTRADA; columna++){
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

    public BoundingBox obtenerMejorPlaca(float [][][] salidaModelo){
        BoundingBox mejorCaja = null;
        float mejorConfianza = 0f;

        for (int i = 0; i < 18900 ; i++){
            float centroX = salidaModelo[0][0][i];
            float centroY = salidaModelo[0][1][i];
            float ancho = salidaModelo[0][2][i];
            float alto = salidaModelo[0][3][i];
            float conf = salidaModelo[0][4][i];

            if (conf > 0.1f && conf > mejorConfianza){
                mejorConfianza = conf;
                mejorCaja = new BoundingBox(
                        centroX, centroY, ancho, alto, conf
                );
            }

        }

        return mejorCaja;
    }

    public Bitmap recortarPlaca(Bitmap imagenOriginal, BoundingBox caja){
        int anchoImagen = imagenOriginal.getWidth();
        int altoImagen = imagenOriginal.getHeight();

        // convertimos valores normalizados en pixeles reales
        int centroX = (int)(caja.centroX * anchoImagen);
        int centroY = (int)(caja.centroY * altoImagen);
        int ancho = (int)(caja.ancho * anchoImagen);
        int alto = (int)(caja.alto * altoImagen);

        int xMin = Math.max(0, centroX - (ancho/2));
        int yMin = Math.max(0, centroY - (alto/2));

        int anchoFinal = Math.min(ancho, anchoImagen - xMin);
        int altoFinal = Math.min(alto, altoImagen - yMin);

        return Bitmap.createBitmap(
                imagenOriginal, xMin, yMin, anchoFinal, altoFinal
        );
    }

    // dibujamos una bounding box sobre la imagen para ver que la deteccion es correcta
    public Bitmap dibujarCaja(Bitmap imagenOriginal, BoundingBox caja){
        // creamos una copia alterable del bitmap
        Bitmap copiaImagen = imagenOriginal.copy(
                Bitmap.Config.ARGB_8888,
                true
        );

        Canvas lienzo = new Canvas(copiaImagen);
        Paint pintura = new Paint();
        pintura.setColor(Color.RED); // color del borde
        pintura.setStyle(Paint.Style.STROKE); // solo contorno
        pintura.setStrokeWidth(5); // grosor de la linea

        int anchoImagen = imagenOriginal.getWidth();
        int altoImagen = imagenOriginal.getHeight();

        // convertir coordenadas normalizadas  a pixeles
        float centroX = caja.centroX * anchoImagen;
        float centroY = caja.centroY * altoImagen;
        float ancho = caja.ancho * anchoImagen;
        float alto = caja.alto * altoImagen;

        float izquierda = centroX - ancho/2;
        float arriba = centroY - alto/2;
        float derecha = centroX + ancho/2;
        float abajo = centroY + alto/2;

        // dibujamos el rectangulo
        lienzo.drawRect(
                izquierda, arriba, derecha, abajo, pintura
        );

        return copiaImagen;

    }

}
