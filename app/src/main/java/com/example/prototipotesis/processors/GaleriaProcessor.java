package com.example.prototipotesis.processors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.prototipotesis.processors.segmentation.MaskContourExtractor;
import com.example.prototipotesis.processors.segmentation.MaskUtils;
import com.example.prototipotesis.processors.segmentation.SegmentationResult;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.BitmapUtils;
import com.example.prototipotesis.utils.GuardarMedia;
import com.example.prototipotesis.render.RenderizadorDetecciones;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GaleriaProcessor {

    private Context context;
    private volatile boolean cancelado = false;
    private RenderizadorDetecciones renderizadorDetecciones;
    private SegmentationProcessor segmentationProcessor;

    // listener para devolver frames procesados
    public interface OnFrameProcesadoListener{
        void onFrameProcesado(
                Bitmap bitmap,
                List<TrackedVehicle> vehiculos,
                List<PointF> vertices
        );
    }

    public GaleriaProcessor(Context context, SegmentationProcessor segmentationProcessor){
        this.context = context;
        this.segmentationProcessor = segmentationProcessor;
    }

    // ================= IMAGEN =================
    public void procesarImagen(
            Uri uri,
            VehicleProcessor vehicleProcessor,
            int anchoPreview,
            int altoPreview,
            OnFrameProcesadoListener listener
    ){
        renderizadorDetecciones = new RenderizadorDetecciones();
        new Thread(() -> {
            try {
                Bitmap bitmap;

                // android 9+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    bitmap = ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                    context.getContentResolver(),
                                    uri
                            )
                    );
                }
                else {
                    bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(),
                            uri
                    );
                }

                Bitmap editable = BitmapUtils.copiarEditable(bitmap);

                // resetamos el tracker porsiacaso
                vehicleProcessor.resetTracker();

                List<TrackedVehicle> vehiculos =
                        vehicleProcessor.processFrame(
                                editable,
                                anchoPreview,
                                altoPreview
                        );

                // ejecutamos la inferencia
                SegmentationResult result = segmentationProcessor.segment(editable);
                int bestDetection = MaskUtils.obtenerMejorDeteccion(result);
                List<PointF> vertices = new ArrayList<>();
                if(bestDetection != -1) {
                    float[][] mask =
                            MaskUtils.crearMascaraFinal(result, bestDetection);

                            vertices = MaskContourExtractor.extraerVertices(mask);
                }

                Bitmap resultado = renderizadorDetecciones.dibujarDetecciones(
                        editable,
                        vehiculos,
                        vertices,
                        false
                );

                GuardarMedia.guardarImagenProcesada(
                        context,
                        resultado
                );

                // devolver resultado
                listener.onFrameProcesado(
                        resultado,
                        vehiculos,
                        vertices
                );
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(
                            context,
                            "Error abriendo imagen",
                            Toast.LENGTH_SHORT
                    ).show();
                });

                Log.e(
                        "GALERIA",
                        "Error procesando imagen",
                        e
                );
            }
        }).start();
    }

    // ================= VIDEO =================
    public void procesarVideo(
            Uri uri,
            VehicleProcessor vehicleProcessor,
            int anchoPreview,
            int altoPreview,
            OnFrameProcesadoListener listener,
            Runnable onFinalizado
    ){
        new Thread(() -> {
            MediaMetadataRetriever retriever =
                    new MediaMetadataRetriever();
            try{
                retriever.setDataSource(
                        context,
                        uri
                );
                long duracionMs =
                        Long.parseLong(
                                retriever.extractMetadata(
                                        MediaMetadataRetriever.METADATA_KEY_DURATION
                                )
                        );

                cancelado = false;
                for(long tiempo = 0; tiempo < duracionMs; tiempo += 250){ // procesar un frame cada 0.25 segundos
                    if (cancelado) {
                        break;
                    }

                    Bitmap frame = retriever.getFrameAtTime(
                                    tiempo * 1000,
                                    MediaMetadataRetriever.OPTION_CLOSEST
                            );

                    if(frame != null){
                        procesarFrame(
                                frame,
                                vehicleProcessor,
                                anchoPreview,
                                altoPreview,
                                listener
                        );
                        Thread.sleep(50);
                    }
                }
            }catch(Exception e){
                Log.e(
                        "VIDEO",
                        "Error procesando video",
                        e
                );
            }finally{
                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                new Handler(Looper.getMainLooper()).post(onFinalizado);
            }

        }).start();
    }

    // ================= FRAME =================
    private void procesarFrame(
            Bitmap frameOriginal,
            VehicleProcessor vehicleProcessor,
            int anchoPreview,
            int altoPreview,
            OnFrameProcesadoListener listener
    ){
        if(!BitmapUtils.bitmapValido(frameOriginal)){
            return;
        }
        Bitmap editable =
                BitmapUtils.copiarEditable(
                        frameOriginal
                );
        List<TrackedVehicle> vehiculos =
                vehicleProcessor.processFrame(
                        editable,
                        anchoPreview,
                        altoPreview
                );

        // ejecutamos la inferencia
        SegmentationResult result = segmentationProcessor.segment(editable);
        int bestDetection = MaskUtils.obtenerMejorDeteccion(result);
        List<PointF> vertices = new ArrayList<>();
        if(bestDetection != -1) {
            float[][] mask =
                    MaskUtils.crearMascaraFinal(result, bestDetection);

            vertices = MaskContourExtractor.extraerVertices(mask);
        }

        // para dibujar las detecciones sobre la imagen (proceso desde galeria)
        Bitmap bitmapResultado =
                renderizadorDetecciones.dibujarDetecciones(
                        editable,
                        vehiculos,
                        vertices,
                        false
                );
        listener.onFrameProcesado(
                bitmapResultado,
                vehiculos,
                vertices
        );
        // guardamos los frames procesados
        GuardarMedia.guardarImagenProcesada(
                context,
                bitmapResultado
        );

    }
    public void cancelarProcesamiento(){
        cancelado = true;
    }
}