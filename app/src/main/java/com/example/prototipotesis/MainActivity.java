package com.example.prototipotesis;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.prototipotesis.camara.AnalizadorFrames;
import com.example.prototipotesis.managers.CamaraManager;
import com.example.prototipotesis.managers.GaleriaManager;
import com.example.prototipotesis.processors.CrosswalkProcessor;
import com.example.prototipotesis.processors.GaleriaProcessor;
import com.example.prototipotesis.processors.PlateProcessor;
import com.example.prototipotesis.ml.TFLiteHelper;
import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.ui.DialogPreProcesamiento;
import com.example.prototipotesis.ui.DialogProcesando;
import com.example.prototipotesis.ui.HistorialPagerAdapter;
import com.example.prototipotesis.utils.BoundingBoxOverlay;
import com.example.prototipotesis.managers.CaptureManager;
import com.example.prototipotesis.utils.InfringmentZoneView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TFLiteHelper vehicleHelper;
    private TFLiteHelper vehicleHelperGaleria;
    private TFLiteHelper plateHelper;
    private TFLiteHelper crosswalkHelper;
    //*************************************************
    private CamaraManager camaraManager;
    private AnalizadorFrames analizadorFrames;
    private CaptureManager captureManager;
    private GaleriaManager galeriaManager;
    //*************************************************
    private PlateProcessor plateProcessor;
    private VehicleProcessor vehicleProcessor;
    private VehicleProcessor vehicleProcessorGaleria;
    private CrosswalkProcessor crosswalkProcessor;
    private GaleriaProcessor galeriaProcessor;
    private DialogProcesando dialogProcesando;

    // ******************* WIDGETS BASE**********************
    private FrameLayout main;
    private PreviewView previewCamara;

    // capa donde se dibujaran las cajas sobre las detecciones
    private BoundingBoxOverlay olBoundingBox;

    // zona de infracciones
    private InfringmentZoneView infringmentZoneView;

    // ********************* botones ************************
    private ImageButton btnCapture;
    private ImageButton btnGalery;
    private ImageButton btnRecord;
    private ImageButton btnHistorial;

    // ********************* secciones *********************
    private DrawerLayout drawerLayout;
    private TabLayout tabLayoutHistorial;
    private ViewPager2 viewPagerHistorial;

    // variable para guardar el ultimo frame procesado
    private Bitmap ultimoFrameRenderizado;

    //
    private final Object frameLock = new Object();


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

                            boolean esVideo = tipo.startsWith("video/");
                            DialogPreProcesamiento dialog =
                                    new DialogPreProcesamiento(
                                            uri,
                                            esVideo,
                                            (uriSeleccionada, video) -> {
                                                iniciarProcesamiento(
                                                        uriSeleccionada,
                                                        video
                                                );
                                            }
                                    );
                            dialog.show(
                                    getSupportFragmentManager(),
                                    "preview"
                            );
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

        // inicializamos variables
        main = findViewById(R.id.main);
        previewCamara = findViewById(R.id.camara);
        olBoundingBox = findViewById(R.id.olBoundingBox);
        infringmentZoneView = findViewById(R.id.infringmentZone);
        drawerLayout = findViewById(R.id.drawerLayout);
        tabLayoutHistorial = findViewById(R.id.tabLayoutHistorial);
        viewPagerHistorial = findViewById(R.id.viewPagerHistorial);

        btnGalery = findViewById(R.id.btnGaleria);
        btnCapture = findViewById(R.id.btnCapture);
        btnRecord = findViewById(R.id.btnRecord);
        btnHistorial = findViewById(R.id.btnHistorial);

        // inicializamos los manager
        captureManager = new CaptureManager(this);
        galeriaManager = new GaleriaManager(selectorMedia);
        galeriaProcessor = new GaleriaProcessor(this);

        //*********************************************************
        HistorialPagerAdapter adapter =
                new HistorialPagerAdapter(this);
        viewPagerHistorial.setAdapter(adapter);

        new TabLayoutMediator(
                tabLayoutHistorial,
                viewPagerHistorial,
                (tab, position) -> {

                    if(position == 0){
                        tab.setText("Capturas");
                    }
                    else{
                        tab.setText("Videos");
                    }
                }
        ).attach();
        //*********************************************************

        // agregamos al boton capture la capacidad de capturar lo que esta en pantalla
        btnCapture.setOnClickListener(v -> captureManager.capturarImagen());
        btnRecord.setOnClickListener(v->{
            if(captureManager.estaGrabando()){
                btnRecord.setImageResource(R.drawable.grabar);
                captureManager.detenerGrabacion();
            }
            else{
                btnRecord.setImageResource(R.drawable.parar);
                captureManager.iniciarGrabacion();
            }
        });

        btnGalery.setOnClickListener(v -> galeriaManager.abrirGaleria());
        btnHistorial.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.END);
        });

        if (infringmentZoneView == null) {
            Log.e("ERROR_FATAL", "infringmentZoneView es NULL");
            return;
        }
        if (olBoundingBox == null) {
            Log.e("ERROR_FATAL", "olBoundingBox es NULL");
            return;
        }

        if (previewCamara == null) {
            Log.e("ERROR_FATAL", "preview es NULL");
            return;
        }


        try {
            // carga de modelos tflite
            plateHelper = new TFLiteHelper(
                    this,
                    "modelos/best_placas16.tflite"
            );
            plateProcessor = new PlateProcessor(plateHelper.getInterprete());

            // modelo VEHICULOS
            vehicleHelper = new TFLiteHelper(
                    this, "modelos/yoloDetector.tflite"
            );
            vehicleProcessor = new VehicleProcessor(vehicleHelper.getInterprete(), plateProcessor);

            vehicleHelperGaleria = new TFLiteHelper(this, "modelos/yoloDetector.tflite");
            vehicleProcessorGaleria =
                    new VehicleProcessor(
                            vehicleHelperGaleria.getInterprete(),
                            plateProcessor
                    );

            // modelo cruces
            crosswalkHelper = new TFLiteHelper(
                    this,
                    "modelos/crosswalkDetector16.tflite"
            );
            crosswalkProcessor = new CrosswalkProcessor(crosswalkHelper.getInterprete());

            // configuracion de la camara una vez que el view esta listo
            previewCamara.post(() -> {
                this.analizadorFrames =
                        new AnalizadorFrames(
                                this,
                                vehicleProcessor,
                                crosswalkProcessor,
                                vehicles -> {
                                    // as actualizaciones de UI (olBoundingBox) deben ir en el hilo principal siempre.
                                    runOnUiThread(() -> {
                                        if (vehicles != null) {
                                            olBoundingBox.updateVehicles(vehicles);
                                        } else {
                                            vehicleProcessor.resetTracker();
                                            olBoundingBox.limpiar();
                                        }
                                    });
                                },
                                bitmap -> {
                                    synchronized (frameLock) {
                                        if (ultimoFrameRenderizado != null &&
                                                !ultimoFrameRenderizado.isRecycled()) {
                                            ultimoFrameRenderizado.recycle(); // liberar el anterior
                                        }
                                        ultimoFrameRenderizado = bitmap;
                                        captureManager.actualizarFrame(bitmap);
                                    }
                                },
                                previewCamara.getWidth(),
                                previewCamara.getHeight(),
                                infringmentZoneView
                        );

                // Iniciar la camara
                camaraManager = new CamaraManager(
                        this,
                        previewCamara,
                        analizadorFrames
                );
                camaraManager.verificarPermisos();
            });


        } catch (IOException error) {
            Toast.makeText(this, "Error cargando el modelo", Toast.LENGTH_LONG).show();
            Log.e(
                    "PRUEBA_MODELO",
                    "Error al cargar el modelo TFLite",
                    error
            );
        }
    }

    private void iniciarProcesamiento(
            Uri uri,
            boolean esVideo
    ){
        dialogProcesando = new DialogProcesando(() -> {
            galeriaProcessor.cancelarProcesamiento();
            Toast.makeText(
                            this,
                            "Procesamiento cancelado",
                            Toast.LENGTH_SHORT
                    ).show();
                });

        dialogProcesando.show(
                getSupportFragmentManager(),
                "procesando"
        );

        // ================= VIDEO =================
        if(esVideo){
            galeriaProcessor.procesarVideo(
                    uri,
                    vehicleProcessorGaleria,
                    previewCamara.getWidth(),
                    previewCamara.getHeight(),
                    (bitmap, vehiculos) -> {

                        runOnUiThread(() -> {
                            if (vehiculos != null && !vehiculos.isEmpty()) {
                                olBoundingBox.updateVehicles(vehiculos);
                            } else {
                                olBoundingBox.limpiar();
                            }
                        });
                    },
                    () -> {
                            if(dialogProcesando != null && dialogProcesando.isAdded()){
                                dialogProcesando.dismissAllowingStateLoss();
                            }
                            Toast.makeText(
                                    this,
                                    "Video procesado",
                                    Toast.LENGTH_SHORT
                            ).show();
                    }
            );
        }
        // ================= IMAGEN =================
        else{
            galeriaProcessor.procesarImagen(
                    uri,
                    vehicleProcessor,
                    previewCamara.getWidth(),
                    previewCamara.getHeight(),
                    (bitmap, vehiculos) -> {
                        runOnUiThread(() -> {
                            if(vehiculos != null && !vehiculos.isEmpty()){
                                olBoundingBox.updateVehicles(vehiculos);
                            }
                            else{
                                olBoundingBox.limpiar();
                            }
                            if(dialogProcesando != null){
                                dialogProcesando.dismiss();
                            }
                        });
                    }
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo,
                                           @NonNull String[] permisos,
                                           @NonNull int[] resultados){
        super.onRequestPermissionsResult(codigo, permisos, resultados);
        camaraManager.manejarRespuestaPermiso(codigo, resultados);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (camaraManager != null) camaraManager.liberarRecursos();

        // para evitar fugas de memoria al cerrar la app
        if (ultimoFrameRenderizado != null) {
            ultimoFrameRenderizado.recycle();
            ultimoFrameRenderizado = null;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // esperamos a que el PreviewView se redibuje con nuevas medidas
        previewCamara.post(() -> {
            if (camaraManager != null) {
                // Obtenemos las nuevas dimensiones
                int nuevoAncho = previewCamara.getWidth();
                int nuevoAlto = previewCamara.getHeight();

                // Actualizamos el analizador para que los cálculos de coordenadas sean correctos
                analizadorFrames.actualizarDimensiones(nuevoAncho, nuevoAlto);

                // limpiamos overlay hasta que llegue el proximo frame
                olBoundingBox.limpiar();

                Log.d("ROTACION", "Nuevas dimensiones: " + nuevoAncho + "x" + nuevoAlto);
            }
        });
    }
}