package com.example.prototipotesis.managers;

import java.nio.ByteBuffer;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.example.prototipotesis.utils.Bitmap2yuv;

public class CaptureManager {
    private Context context;
    private Bitmap ultimoFrame;
    private boolean grabando = false;
    private File captures; // carpeta donde se guardaran las capturas
    private File recordings; // carpeta donde se guardaran las grabaciones
    private static final int FPS = 15;

    private MediaCodec encoder;
    private MediaMuxer muxer;
    private int trackIndex = -1;
    private boolean muxerStarted = false;
    private long tiempoInicioGrabacion = 0;

    public CaptureManager(Context context){
        this.context = context.getApplicationContext();
        // creamos las carpetas donde se guardaran capturas, temps y videos
        captures = new File(
            context.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
            ),
            "Capturas"
        );
        if(!captures.exists()) captures.mkdirs();

        recordings = new File(
            context.getExternalFilesDir(
                    Environment.DIRECTORY_MOVIES
            ),

            "Grabaciones"
        );
        if(!recordings.exists()) recordings.mkdirs();
    }
    /*
    * este metodo recibe los frames procesados desde Analizador
    * */
    public void actualizarFrame(Bitmap bitmap){
        // liberamos el frame anterior para evitar acumulacion de memoria
        if(ultimoFrame != null && ultimoFrame != bitmap && !ultimoFrame.isRecycled()){
            ultimoFrame.recycle();
        }

        // guardamos nuevo frame
        ultimoFrame = bitmap;

        // grabacion en tiempo real
        if(grabando){
            try{
                // bitmap -> yuv
                byte[] yuv =
                        Bitmap2yuv.bitmap2NV12(bitmap);
                // enviar directo al encoder
                encodeFrame(yuv);
            }catch(Exception e){
                Log.e(
                        "CAPTURE_MANAGER",
                        "Error grabando frame",
                        e
                );
            }
        }
    }

    public void capturarImagen(){
        // validamos que exista frame
        if(ultimoFrame == null){
            Toast.makeText(
                    context,
                    "Camara no lista",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // creamos copia del bitmap
        Bitmap copia = ultimoFrame.copy(
                Bitmap.Config.ARGB_8888,
                false
        );

        guardarImagen(copia);
    }

    private void guardarImagen(Bitmap bitmap){
        // nombre del archivo
        String nombre = "captura_" + System.currentTimeMillis() + ".jpg";

        File archivo = new File(captures, nombre);

        try{
            // flujo de salida
            FileOutputStream fos = new FileOutputStream(archivo);

            // guardamos bitmap en formato JPG
            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    100,
                    fos
            );

            fos.flush();
            fos.close();

            Toast.makeText(
                    context,
                    "Imagen guardada",
                    Toast.LENGTH_SHORT
            ).show();

        }catch(Exception e){

            Log.e("CAPTURE_MANAGER", "Error guardando imagen", e);

        }finally {
            // liberar bitmap copia
            if(!bitmap.isRecycled()) bitmap.recycle();
        }
    }

    public void iniciarGrabacion(){
        try{
            if(ultimoFrame == null){
                return;
            }
            tiempoInicioGrabacion = System.nanoTime();

            int width = ultimoFrame.getWidth();
            int height = ultimoFrame.getHeight();

            File archivoSalida = new File(
                    recordings,
                    "video_" +
                            System.currentTimeMillis() +
                            ".mp4"
            );

            MediaFormat format =
                    MediaFormat.createVideoFormat(
                            MediaFormat.MIMETYPE_VIDEO_AVC,
                            width,
                            height
                    );

            format.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities
                            .COLOR_FormatYUV420SemiPlanar
            );

            format.setInteger(
                    MediaFormat.KEY_BIT_RATE,
                    4000000
            );

            format.setInteger(
                    MediaFormat.KEY_FRAME_RATE,
                    FPS
            );

            format.setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL,
                    1
            );

            encoder =
                    MediaCodec.createEncoderByType(
                            MediaFormat.MIMETYPE_VIDEO_AVC
                    );

            encoder.configure(
                    format,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE
            );

            encoder.start();

            muxer = new MediaMuxer(
                    archivoSalida.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            );

            grabando = true;

            Toast.makeText(
                    context,
                    "Grabacion iniciada",
                    Toast.LENGTH_SHORT
            ).show();

        }catch(Exception e){
            Log.e(
                    "CAPTURE_MANAGER",
                    "Error iniciando grabacion",
                    e
            );
        }
    }

    public void detenerGrabacion(){
        if(!grabando) return;
        grabando = false;
        finalizarEncoder();;
        Toast.makeText(
                context,
                "Video guardado",
                Toast.LENGTH_LONG
        ).show();
    }

    public boolean estaGrabando(){
        return grabando;
    }

    private void encodeFrame(byte[] data){
        try{
            int inputBufferIndex = encoder.dequeueInputBuffer(10000);
            if(inputBufferIndex >= 0){
                ByteBuffer inputBuffer = encoder.getInputBuffer(
                        inputBufferIndex
                );

                inputBuffer.clear();
                inputBuffer.put(data);
                long pts = (System.nanoTime() - tiempoInicioGrabacion) / 1000L;

                encoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        data.length,
                        pts,
                        0
                );
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            int outputBufferIndex = encoder.dequeueOutputBuffer(
                    bufferInfo,
                    10000
            );

            while(outputBufferIndex >= 0){
                ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferIndex);

                if((bufferInfo.flags &
                        MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){

                    bufferInfo.size = 0;
                }

                if(bufferInfo.size != 0){
                    if(!muxerStarted){
                        MediaFormat newFormat = encoder.getOutputFormat();

                        trackIndex = muxer.addTrack(newFormat);

                        muxer.start();

                        muxerStarted = true;
                    }

                    outputBuffer.position(bufferInfo.offset);

                    outputBuffer.limit(
                            bufferInfo.offset +
                                    bufferInfo.size
                    );

                    muxer.writeSampleData(
                            trackIndex,
                            outputBuffer,
                            bufferInfo
                    );
                }

                encoder.releaseOutputBuffer(
                        outputBufferIndex,
                        false
                );

                outputBufferIndex = encoder.dequeueOutputBuffer(
                        bufferInfo,
                        0
                );
            }

        }catch(Exception e){
            Log.e(
                    "ENCODER",
                    "Error codificando frame",
                    e
            );
        }
    }

    //private long computePresentationTime(long frameIndex){
    //   return 132 + frameIndex * 1000000 / FPS;
    //}

    // limpieza de franes que estan en la carpeta temporal
    public void limpiarFramesTemporales(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete(); // borra cada frame individualmente
                }
            }
        }
    }

    private void finalizarEncoder(){
        try{
            if(encoder != null){
                encoder.stop();
                encoder.release();
                encoder = null;
            }
        }catch(Exception e){
            Log.e(
                    "ENCODER",
                    "Error liberando encoder",
                    e
            );
        }
        try{
            if(muxer != null){
                if(muxerStarted){
                    muxer.stop();
                }
                muxer.release();
                muxer = null;
            }
        }catch(Exception e){
            Log.e(
                    "MUXER",
                    "Error liberando muxer",
                    e
            );
        }
        muxerStarted = false;
    }
}
