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

import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import org.tensorflow.lite.Interpreter;

import com.example.prototipotesis.camara.AnalizadorFrames;
import com.example.prototipotesis.camara.GestorCamara;
import com.example.prototipotesis.ml.BoundingBox;
import com.example.prototipotesis.ml.DetectorPlacas;
import com.example.prototipotesis.ml.TFLiteHelper;
import com.example.prototipotesis.ocr.NormalizarPlaca;
import com.example.prototipotesis.ocr.OCRHelper;
import com.example.prototipotesis.utils.BoundingBoxOverlay;
import com.example.prototipotesis.utils.ImageUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TFLiteHelper helperModelo;
    private DetectorPlacas detectorPlacas;
    private OCRHelper ocrHelper;
    private GestorCamara gestorCamara;
    //*************************************************
    private ImageView ivOriginal;
    private ImageView ivPlaca;
    private EditText etPlaca;
    private PreviewView previewCamara;

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
            // 1. Inicializar el helper del modelo
            helperModelo = new TFLiteHelper(
                    this,
                    "modelos/best_placas16.tflite"
            );

            // Mensaje de éxito
            Log.d(
                    "PRUEBA_MODELO",
                    "Modelo TFLite cargado correctamente"
            );

            // creacion del interprete
            Interpreter interprete = helperModelo.getInterprete();

            previewCamara.post(() -> {

                int anchoPreview = previewCamara.getWidth();
                int altoPreview = previewCamara.getHeight();
                Log.d("ANCHO_ALTO", "Ancho: " + anchoPreview + " Alto: " + altoPreview);

                AnalizadorFrames analizadorFrames =
                        new AnalizadorFrames(
                                this,
                                interprete,
                                rectangulo -> {
                                    if (rectangulo != null){
                                        olBoundingBox.actualizarCaja(rectangulo);
                                    }
                                    else{
                                        olBoundingBox.limpiar();
                                    }
                                },
                                olBoundingBox,
                                anchoPreview,
                                altoPreview
                        );
                gestorCamara = new GestorCamara(
                        this,
                        previewCamara,
                        analizadorFrames
                );
                gestorCamara.verificarPermisos();
            });

            // 2. creamos el detector de placas
            detectorPlacas = new DetectorPlacas(interprete);

            // 3. iniciar el ocrHelper
            ocrHelper = new OCRHelper();

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

        ivOriginal.setImageBitmap(frameOriginal);

        frameOriginal = ImageUtils.convertirABitmapEditable(frameOriginal);

        // 1. deteccion de placas
        float[][][] salida = detectorPlacas.detectarPlacas(frameOriginal);

        // 2. obtenemos la mejor boundingBox
        BoundingBox placa = detectorPlacas.obtenerMejorPlaca(salida);

        // 3. recortamos la placa
        if (placa == null){
            Toast.makeText(this, "No se detecto la placa", Toast.LENGTH_LONG).show();
            Log.d("DEBUG_FLUJO", "placa NO detectada");
            return;
        }

        Bitmap bitmapPlaca = detectorPlacas.recortarPlaca(frameOriginal, placa);
        ivPlaca.setImageBitmap(bitmapPlaca);

        // 4. pre procesamiento pcr
        Bitmap placaOCR = bitmapPlaca;
        placaOCR = ImageUtils.escalar(placaOCR, 300);
        placaOCR = ImageUtils.color2gray(placaOCR);

        // 5. OCR
        ocrHelper.reconocerTexto(
                placaOCR,
                new OCRHelper.ResultadoOCR() {
                    @Override
                    public void onResultado(List<String> posiblesPlacas) {
                        if(posiblesPlacas == null || posiblesPlacas.isEmpty()){
                            // ocr no encontro texto
                            etPlaca.setText("");
                            etPlaca.setHint("No se pudo reconocer la placa");
                            return;
                        }
                        // verificar las lineas devueltas
                        String cadenaFinal = null;
                        for(String linea : posiblesPlacas){
                            String texto = linea.replaceAll("\\s", "").toUpperCase(); // limpiamos espacios y saltos

                            // ignorar palabras como BOLIVIA
                            if (texto.contains("BOLIVIA") || texto.contains("BOL") || texto.contains("VIA")) continue;

                            // validar longitu tipica de placa
                            if (texto.length() >= 6 && texto.length() <= 7){
                                // validar que tenga letras y numeros
                                if (texto.matches("(?=.*[A-Z])(?=.*\\d)[A-Z0-9]+")) {
                                    cadenaFinal = texto;
                                    break;
                                }
                            }
                        }
                        if(cadenaFinal != null){
                            cadenaFinal = NormalizarPlaca.normalizar(cadenaFinal);
                            etPlaca.setText(cadenaFinal);
                            Log.d("DEBUG_FLUJO", "placa detectada: " + cadenaFinal);
                        }
                        else{
                            etPlaca.setText("");

                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e("OCR", "Error OCR", error);
                    }
                });

    }
}