package com.example.prototipotesis.processors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.utils.GuardarMedia;
import com.example.prototipotesis.utils.ImageUtils;
import com.example.prototipotesis.utils.RenderizadorDetecciones;

import java.io.IOException;
import java.util.List;

public class GaleriaProcessor {

    private Context context;
    private volatile boolean cancelado = false;

    // listener para devolver frames procesados
    public interface OnFrameProcesadoListener{
        void onFrameProcesado(
                Bitmap bitmap,
                List<TrackedVehicle> vehiculos
        );
    }

    public GaleriaProcessor(Context context){
        this.context = context;
    }

    // ================= IMAGEN =================
    public void procesarImagen(
            Uri uri,
            VehicleProcessor vehicleProcessor,
            int anchoPreview,
            int altoPreview,
            OnFrameProcesadoListener listener
    ){
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

                Bitmap editable = ImageUtils.convertirABitmapEditable(bitmap);

                // resetamos el tracker porsiacaso
                vehicleProcessor.resetTracker();

                List<TrackedVehicle> vehiculos =
                        vehicleProcessor.processFrame(
                                editable,
                                anchoPreview,
                                altoPreview
                        );

                Bitmap resultado = RenderizadorDetecciones.dibujarDetecciones(
                        editable,
                        vehiculos
                );

                GuardarMedia.guardarImagenProcesada(
                        context,
                        resultado
                );

                // devolver resultado
                listener.onFrameProcesado(
                        resultado,
                        vehiculos
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
        if(!ImageUtils.bitmapValido(frameOriginal)){
            return;
        }
        Bitmap editable =
                ImageUtils.convertirABitmapEditable(
                        frameOriginal
                );
        List<TrackedVehicle> vehiculos =
                vehicleProcessor.processFrame(
                        editable,
                        anchoPreview,
                        altoPreview
                );

        // para dibujar las detecciones sobre la imagen (proceso desde galeria)
        Bitmap bitmapResultado =
                RenderizadorDetecciones.dibujarDetecciones(
                        editable,
                        vehiculos
                );
        listener.onFrameProcesado(
                bitmapResultado,
                vehiculos
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