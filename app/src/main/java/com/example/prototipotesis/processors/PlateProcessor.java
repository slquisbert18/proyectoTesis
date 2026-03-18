package com.example.prototipotesis.processors;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import org.tensorflow.lite.Interpreter;

import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.ml.DetectorPlacas;
import com.example.prototipotesis.ml.PlateTracker;
import com.example.prototipotesis.ml.TrackedPlate;
import com.example.prototipotesis.ocr.NormalizarPlaca;
import com.example.prototipotesis.ocr.OCRHelper;
import com.example.prototipotesis.ocr.OCRStabilizer;
import com.example.prototipotesis.utils.ImageUtils;

public class PlateProcessor {
    private DetectorPlacas detectorPlacas;
    private OCRHelper ocrHelper;
    private PlateTracker tracker;

    public PlateProcessor(Interpreter interprete){
        detectorPlacas = new DetectorPlacas(interprete);
        ocrHelper = new OCRHelper();
        tracker = new PlateTracker();
    }

    public List<TrackedPlate> procesarFrame(
            Bitmap bitmapOriginal,
            int anchoPreview,
            int altoPreview
    ){
        Log.d("PROCESADOR_PLACAS", "ProcesarFrame ejecutado");
        float[][][] salida =
                detectorPlacas.detectarPlacas(bitmapOriginal);

        List<BoundingBox> cajas = detectorPlacas.obtenerPlacas(salida);
        Log.d("DETECCION", "Cajas detectadas: " + cajas.size());

        if(cajas == null || cajas.isEmpty()){
            return new ArrayList<>();
        }
        List<RectF> recangulosPreview = new ArrayList<>();
        List<BoundingBox> cajasModelo = new ArrayList<>();

        // cpnvertomos cajas a coordenadas del preview
        for(BoundingBox caja : cajas){
            RectF rectanguloPreview = convertirCoordenadasAPreview(
                    caja,
                    bitmapOriginal.getWidth(),
                    bitmapOriginal.getHeight(),
                    anchoPreview,
                    altoPreview
            );
            recangulosPreview.add(rectanguloPreview);
            cajasModelo.add(caja);
        }

        // tracking
        List<TrackedPlate> placasTrackeadas = tracker.actualizar(recangulosPreview, cajasModelo);

        // OCR solo para placas sin texto
        for(TrackedPlate placaTrackeada : placasTrackeadas){
            placaTrackeada.framesDesdeUltimoOcr++;
                                          // solo ejecutar ocr cada 10 frames
            if(!placaTrackeada.ocrEnProceso && placaTrackeada.framesDesdeUltimoOcr > 10){
                placaTrackeada.ocrEnProceso = true;
                // como ya pasaron 10 frames, reiniciamos el contador
                placaTrackeada.framesDesdeUltimoOcr = 0;

                BoundingBox cajaModelo = placaTrackeada.cajaModelo;

                Bitmap placaRecortada = detectorPlacas.recortarPlaca(bitmapOriginal, cajaModelo);

                Bitmap placaOCR = ImageUtils.color2gray(
                        ImageUtils.escalar(placaRecortada, 300)
                );
                ocrHelper.reconocerTexto(
                        placaOCR,
                        new OCRHelper.ResultadoOCR() {
                            @Override
                            public void onResultado(List<String> posiblesPlacas) {
                                if (!posiblesPlacas.isEmpty()){
                                    String textoNormalizado =
                                            NormalizarPlaca.normalizar(posiblesPlacas.get(0));
                                    Log.d("TEXTO_EXTRAIDO", textoNormalizado);

                                    // enviamos el texto al overlay
                                    if(textoNormalizado != null) {
                                        //placaTrackeada.texto = textoNormalizado;
                                        // agregar nuevo resultado al historial
                                        placaTrackeada.historialOCR.add(textoNormalizado);

                                        // limitar tamanio del historial
                                        if(placaTrackeada.historialOCR.size() > TrackedPlate.MAX_HISTORIAL_OCR){
                                            placaTrackeada.historialOCR.remove(0);
                                        }

                                        // obtener el texto mas frecuente
                                        String stableText = OCRStabilizer.mostFrecuentText(placaTrackeada.historialOCR);

                                        placaTrackeada.texto = stableText;
                                    }
                                }

                                placaTrackeada.ocrEnProceso = false;
                            }

                            @Override
                            public void onError(Exception error) {
                                placaTrackeada.ocrEnProceso = false;
                                Log.e("OCR", "Error OCR", error);
                            }
                        });
            }
        }
        return placasTrackeadas;
    }

    private RectF convertirCoordenadasAPreview(
            BoundingBox caja,
            int anchoOriginal,
            int altoOriginal,
            int anchoPreview,
            int altoPreview
    ){
        // coordenadas normalizadas del modelo (0 a 1)
        float centroX = caja.centroX * anchoOriginal;
        float centroY = caja.centroY * altoOriginal;
        float anchoCaja = caja.ancho * anchoOriginal;
        float altoCaja = caja.alto * altoOriginal;

        // convertimos a coordenadas absolutas del bitmap
        float izquierda = centroX - (anchoCaja / 2f);
        float arriba = centroY - (altoCaja / 2f);
        float derecha = centroX + (anchoCaja / 2f);
        float abajo = centroY + (altoCaja / 2f);

        // ahora escalamos al tamaño real del PreviewView
        float escalaX = (float) anchoPreview / (float) anchoOriginal;
        float escalaY = (float) altoPreview / (float) altoOriginal;

        izquierda *= escalaX;
        derecha *= escalaX;
        arriba *= escalaY;
        abajo *= escalaY;

        return new RectF(izquierda, arriba, derecha, abajo);
    }
}
