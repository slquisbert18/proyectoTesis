package com.example.prototipotesis.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegateFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class TFLiteHelper {

    private Interpreter interprete; // interprete que ejecuta el modelo

    // constructor que recibe el contexto de la app
    public TFLiteHelper(Context contexto, String rutaModelo) throws IOException{
        CompatibilityList compatibilidad = new CompatibilityList();
        Interpreter.Options opciones = new Interpreter.Options();
        if (compatibilidad.isDelegateSupportedOnThisDevice()){
            GpuDelegateFactory.Options opcionesGpu =
                    compatibilidad.getBestOptionsForThisDevice();

            GpuDelegateFactory gpuDelegado = new GpuDelegateFactory(opcionesGpu);
            opciones.addDelegateFactory(gpuDelegado);
            Log.d("TFLITE", "GPU delegado activado");
        }
        else{
            Log.d("TFLITE", "GPU no soportado, usando CPU");
        }
        interprete = new Interpreter(
                loadModelFile(contexto.getAssets(), rutaModelo), opciones
        );
    }

    // carga el modelo desde assets como buffer de memoria
    private MappedByteBuffer loadModelFile(
            AssetManager assetManager,
            String rutaModelo
    ) throws IOException{

        // abrir el modelo
        AssetFileDescriptor descriptorArchivo = assetManager.openFd(rutaModelo);
        // flujo de lectura del archivo (para leer el archivo binario)
        FileInputStream flujoEntradaArchivo = new FileInputStream(descriptorArchivo.getFileDescriptor());
        // canal de lectura
        FileChannel canalArchivo = flujoEntradaArchivo.getChannel();
        //offset (desplazamiento) inicial del modelo dentro del apk
        long startOffset = descriptorArchivo.getStartOffset();
        // tamanio del modelo
        long declaredLength = descriptorArchivo.getDeclaredLength();

        // mapear el modelo completo en memoria
        return canalArchivo.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
        );
    }

    // devuelve el interprete para ejecutar inferencias
    public Interpreter getInterprete(){
        return interprete;
    }

    // liberar memoria cuando el modelo no se usa
    public void close(){
        if(interprete != null){
            interprete.close();
        }
    }
}
