package com.example.prototipotesis.ml;

import android.graphics.Bitmap;

import com.example.prototipotesis.utils.ImageUtils;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
        ByteBuffer entradaModelo = ImageUtils.bitmap2bytebuffer(imagenResize, TAMANIO_ENTRADA, CANALES_COLOR);

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
    public List<BoundingBox> obtenerPlacas(float [][][] salidaModelo){
        List<BoundingBox> cajas = new ArrayList<>();

        for (int i = 0; i < 18900 ; i++){
            float centroX = salidaModelo[0][0][i];
            float centroY = salidaModelo[0][1][i];
            float ancho = salidaModelo[0][2][i];
            float alto = salidaModelo[0][3][i];
            float conf = salidaModelo[0][4][i];

            if (conf > 0.65f){
                cajas.add(new BoundingBox(
                        centroX, centroY, ancho, alto, conf
                ));
            }
        }

        return cajas;
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

}
