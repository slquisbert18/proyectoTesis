package com.example.prototipotesis;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import com.example.prototipotesis.camara.AnalizadorFrames;
import com.example.prototipotesis.camara.GestorCamara;
import com.example.prototipotesis.processors.PlateProcessor;
import com.example.prototipotesis.ml.TFLiteHelper;
import com.example.prototipotesis.ml.TrackedPlate;
import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.utils.BoundingBoxOverlay;
import com.example.prototipotesis.utils.ImageUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TFLiteHelper vehicleHelper;
    private TFLiteHelper plateHelper;
    private Interpreter vehiculeInterpreter;
    private Interpreter plateInterpreter;
    //*************************************************
    private GestorCamara gestorCamara;
    //*************************************************
    private PreviewView previewCamara;
    private PlateProcessor procesadorPlacas;
    private VehicleProcessor vehiculoProcessor;

    // capa donde se dibujaran las cajas sobre las detecciones
    private BoundingBoxOverlay olBoundingBox;

    // SELECTOR DE IMAGENES DE GALERIA
    private final ActivityResultLauncher<Intent> selectorMedia =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null){
                            Uri uri = resultado.getData().getData();
                            if (uri == null) return;

                            String tipo = getContentResolver().getType(uri);

                            if (tipo == null) return;

                            if (tipo.startsWith("image/")){
                                procesarImagenDeGaleria(uri);
                            } else if (tipo.startsWith("video/")) {

                                procesarVideo(uri);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // iniciar la camara
        previewCamara = findViewById(R.id.camara);

        olBoundingBox = findViewById(R.id.olBoundingBox);
        Button btnGaleria = findViewById(R.id.btnGaleria);

        try {
            // iniciamos los helpers
            // modelo VEHICULOS
            vehicleHelper = new TFLiteHelper(
                    this, "modelos/yoloDetector.tflite"
            );
            vehiculeInterpreter = vehicleHelper.getInterprete();
            vehiculoProcessor = new VehicleProcessor(vehiculeInterpreter);

            // modelo PLACAS
            plateHelper = new TFLiteHelper(
                    this,
                    "modelos/best_placas16.tflite"
            );
            plateInterpreter = plateHelper.getInterprete();
            procesadorPlacas = new PlateProcessor(plateInterpreter);

            // 2. Iniciar la camara

            previewCamara.post(() -> {

                int anchoPreview = previewCamara.getWidth();
                int altoPreview = previewCamara.getHeight();
                //Log.d("ANCHO_ALTO", "Ancho: " + anchoPreview + " Alto: " + altoPreview);

                AnalizadorFrames analizadorFrames =
                        new AnalizadorFrames(
                                this,
                                procesadorPlacas,
                                placas -> {
                                    if (placas != null){
                                        olBoundingBox.actualizarPlacas(placas);
                                        Log.d("OVERLAY", "Placas recibidas: "+ placas.size());
                                    }
                                    else{
                                        olBoundingBox.limpiar();
                                    }
                                },
                                anchoPreview,
                                altoPreview
                        );

                // Iniciar la camara
                gestorCamara = new GestorCamara(
                        this,
                        previewCamara,
                        analizadorFrames
                );
                gestorCamara.verificarPermisos();
            });


        } catch (IOException error) {
            Toast.makeText(this, "Error cargando el modelo", Toast.LENGTH_LONG).show();
            // Error al cargar el modelo
            Log.e(
                    "PRUEBA_MODELO",
                    "Error al cargar el modelo TFLite",
                    error
            );
        }

        btnGaleria.setOnClickListener(v -> abrirGaleria());

    }

    @Override
    public void onRequestPermissionsResult(int codigo,
                                           @NonNull String[] permisos,
                                           @NonNull int[] resultados){
        super.onRequestPermissionsResult(codigo, permisos, resultados);
        gestorCamara.manejarRespuestaPermiso(codigo, resultados);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        gestorCamara.liberarRecursos();
    }
    // ABRIR GALERIA
    private void abrirGaleria(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "image/*",
                "video/*"
        });
        selectorMedia.launch(intent);
    }

    // PROCESAR IMAGEN
    private void procesarImagenDeGaleria(Uri uri){
        try {
            Bitmap bitmap;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // API 28+
                bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(getContentResolver(), uri)
                );
            } else {
                // API 26–27
                bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                        getContentResolver(),
                        uri
                );
            }

            if (bitmap == null) {
                Toast.makeText(this, "bitmap nulo", Toast.LENGTH_LONG).show();
                return;
            }

            procesarFrame(bitmap);

        } catch (IOException e) {
            Toast.makeText(this, "Error abriendo imagen", Toast.LENGTH_SHORT).show();
            Log.e("IMG", "Error decodificando imagen", e);
        }

    }
    private void procesarVideo(Uri uri){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);

            long duracionMs = Long.parseLong(
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
            );

            for (long tiempo = 0; tiempo < duracionMs ; tiempo += 200){
                Bitmap frame = retriever.getFrameAtTime(
                        tiempo * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                );

                if (frame == null) continue;
                procesarFrame(frame);
            }
        }
        catch (Exception e){
            Toast.makeText(this, "Error procesando video", Toast.LENGTH_SHORT).show();
        }
        finally{
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void procesarFrame(Bitmap frameOriginal){
        if(!ImageUtils.bitmapValido(frameOriginal)){
            Toast.makeText(this, "Imagen invalida", Toast.LENGTH_LONG).show();
            return;
        }

        frameOriginal = ImageUtils.convertirABitmapEditable(frameOriginal);

        int anchoPreview = previewCamara.getWidth();
        int altoPreview = previewCamara.getHeight();

        List<TrackedPlate> placas = procesadorPlacas.procesarFrame(
                frameOriginal,
                anchoPreview,
                altoPreview
        );

        if(placas == null || placas.isEmpty()){
            olBoundingBox.limpiar();
            return;
        }

        olBoundingBox.actualizarPlacas(placas);
    }

}