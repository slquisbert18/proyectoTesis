package com.example.prototipotesis;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PointF;
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
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.prototipotesis.camara.AnalizadorFrames;
import com.example.prototipotesis.managers.CamaraManager;
import com.example.prototipotesis.managers.GaleriaManager;
import com.example.prototipotesis.overlay.PolygonOverlay;
import com.example.prototipotesis.processors.GaleriaProcessor;
import com.example.prototipotesis.processors.PlateProcessor;
import com.example.prototipotesis.ml.TFLiteHelper;
import com.example.prototipotesis.processors.SegmentationProcessor;
import com.example.prototipotesis.processors.VehicleProcessor;
import com.example.prototipotesis.processors.segmentation.MaskScaleUtils;
import com.example.prototipotesis.render.RenderizadorDetecciones;
import com.example.prototipotesis.trackedObject.TrackedVehicle;
import com.example.prototipotesis.ui.DialogPreProcesamiento;
import com.example.prototipotesis.ui.DialogProcesando;
import com.example.prototipotesis.ui.HistorialPagerAdapter;
import com.example.prototipotesis.overlay.BoundingBoxOverlay;
import com.example.prototipotesis.managers.CaptureManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TFLiteHelper vehicleHelper;
    private TFLiteHelper vehicleHelperGaleria;
    private TFLiteHelper plateHelper;
    private TFLiteHelper crucesHelper;
    //*************************************************
    private CamaraManager camaraManager;
    private AnalizadorFrames analizadorFrames;
    private CaptureManager captureManager;
    private GaleriaManager galeriaManager;
    //*************************************************
    private PlateProcessor plateProcessor;
    private VehicleProcessor vehicleProcessor;
    private VehicleProcessor vehicleProcessorGaleria;
    private GaleriaProcessor galeriaProcessor;
    private DialogProcesando dialogProcesando;
    private SegmentationProcessor segmentacionProcessor;

    // ******************* WIDGETS BASE**********************
    private PreviewView previewCamara;
    private BoundingBoxOverlay olBoundingBox; // capa donde se dibujaran las cajas sobre las detecciones
    private PolygonOverlay polygonOverlay;
    // ********************* botones ************************
    private ImageButton btnCapture;
    private ImageButton btnGalery;
    private ImageButton btnRecord;
    private ImageButton btnHistorial;

    // ********************* secciones *********************
    private DrawerLayout drawerLayout;
    private TabLayout tabLayoutHistorial;
    private ViewPager2 viewPagerHistorial;
    private Bitmap ultimoFrameRenderizado; // variable para guardar el ultimo frame procesado
    private final Object frameLock = new Object();
    //********************************************************
    private Bitmap ultimoFrameLimpio;
    private List<PointF> ultimosVerticesDetectados = new ArrayList<>();
    private List<TrackedVehicle> ultimosVehiculosDetectados = new ArrayList<>();


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

        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "No se pudo cargar la librería");
        } else {
            Log.d("OpenCV", "OpenCV cargado correctamente");
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarViews();

        inicializarManagers();

        configurarHistorial();

        configurarBotones();

        inicializarModelos();

        configurarCamara();
    }

    private void inicializarViews() {
        previewCamara = findViewById(R.id.camara);
        olBoundingBox = findViewById(R.id.olBoundingBox);
        polygonOverlay = findViewById(R.id.polygonOverlay);
        drawerLayout = findViewById(R.id.drawerLayout);
        tabLayoutHistorial = findViewById(R.id.tabLayoutHistorial);
        viewPagerHistorial = findViewById(R.id.viewPagerHistorial);

        btnGalery = findViewById(R.id.btnGaleria);
        btnCapture = findViewById(R.id.btnCapture);
        btnRecord = findViewById(R.id.btnRecord);
        btnHistorial = findViewById(R.id.btnHistorial);

    }

    private void inicializarManagers() {
        captureManager = new CaptureManager(this);
        galeriaManager = new GaleriaManager(selectorMedia);
    }

    private void configurarHistorial() {
        HistorialPagerAdapter adapter =
                new HistorialPagerAdapter(this);
        viewPagerHistorial.setAdapter(adapter);

        new TabLayoutMediator(
                tabLayoutHistorial,
                viewPagerHistorial,
                (tab, position) -> {

                    if (position == 0) {
                        tab.setText("Capturas");
                    } else {
                        tab.setText("Videos");
                    }
                }
        ).attach();
    }

    private void configurarBotones() {
        // agregamos al boton capture la capacidad de capturar lo que esta en pantalla
        btnCapture.setOnClickListener(v -> {
            if (ultimoFrameLimpio == null) {
                Toast.makeText(this, "Cámara no lista", Toast.LENGTH_SHORT).show();
                return;
            }

            // Si vehiculos o vertices son null, pasamos una lista vacía para evitar crash
            List<TrackedVehicle> vh = ultimosVehiculosDetectados != null ? ultimosVehiculosDetectados : new ArrayList<>();
            List<PointF> vt = ultimosVerticesDetectados != null ? ultimosVerticesDetectados : new ArrayList<>();

            Bitmap bitmapFinal = RenderizadorDetecciones.dibujarDetecciones(
                    ultimoFrameLimpio, vh, vt, true
            );
            captureManager.guardarImagen(bitmapFinal);

            // actualizamos historial
            actualizarHistorial();
        });

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
    }

    private void inicializarModelos() {
        try {
            // placas
            plateHelper = new TFLiteHelper(
                    this,
                    "modelos/modeloPlacas16.tflite"
            );
            plateProcessor = new PlateProcessor(plateHelper.getInterprete());

            // vehiculos
            vehicleHelper = new TFLiteHelper(
                    this, "modelos/yoloDetector.tflite"
            );
            vehicleProcessor = new VehicleProcessor(vehicleHelper.getInterprete(), plateProcessor);

            // vehiculos (para galeria)
            vehicleHelperGaleria = new TFLiteHelper(
                    this, "modelos/yoloDetector.tflite"
            );
            vehicleProcessorGaleria =
                    new VehicleProcessor(vehicleHelperGaleria.getInterprete(), plateProcessor);

            // cruces peatonales
            crucesHelper = new TFLiteHelper(
                    this,
                    "modelos/modeloCruces16.tflite"
            );
            segmentacionProcessor = new SegmentationProcessor(crucesHelper.getInterprete());

        } catch (Exception e) {
            Toast.makeText(this, "Error cargando el modelo", Toast.LENGTH_LONG).show();
            Log.e(
                    "PRUEBA_MODELO",
                    "Error al cargar el modelo TFLite",
                    e
            );
        }
    }

    private void configurarCamara(){
        previewCamara.post(() -> {
            this.analizadorFrames =
                    new AnalizadorFrames(
                            this,
                            vehicleProcessor,
                            segmentacionProcessor,
                            (
                                    bitmapOriginal,
                                    vehiculos,
                                    verticesCruce
                            ) -> {
                                // almacenamos los resultados para su uso en la captura de imagenes
                                this.ultimoFrameLimpio = bitmapOriginal;
                                this.ultimosVerticesDetectados = verticesCruce;
                                this.ultimosVehiculosDetectados = vehiculos;

                                runOnUiThread(() -> {
                                    // vehiculos
                                    if (vehiculos != null && !vehiculos.isEmpty()) {
                                        olBoundingBox.updateVehicles(vehiculos);
                                    }
                                    else {
                                        olBoundingBox.limpiar();
                                        vehicleProcessor.resetTracker();
                                    }
                                    // poligono
                                    if(verticesCruce != null && !verticesCruce.isEmpty()){
                                        polygonOverlay.post(()->{
                                            List<PointF> verticesEscalados =
                                                    MaskScaleUtils
                                                            .escalarVertices(
                                                                    verticesCruce,
                                                                    160,
                                                                    160,
                                                                    polygonOverlay.getWidth(),
                                                                    polygonOverlay.getHeight()
                                                            );
                                            if(verticesEscalados != null && verticesEscalados.size() >= 4){
                                                polygonOverlay.setVertices(verticesEscalados);
                                            }
                                            else{
                                                polygonOverlay.clear();
                                            }
                                        });
                                    }
                                    else{
                                        polygonOverlay.clear();
                                    }
                                });

                                // SOLO grabar si esta grabando
                                if(captureManager.estaGrabando()){

                                    Bitmap bitmapGrabacion =
                                            RenderizadorDetecciones
                                                    .dibujarDetecciones(
                                                            bitmapOriginal,
                                                            vehiculos,
                                                            verticesCruce,
                                                            true
                                                    );

                                    captureManager.actualizarFrame(bitmapGrabacion);
                                }

                            }
                    );

            // Iniciar la camara
            camaraManager = new CamaraManager(
                    this,
                    previewCamara,
                    analizadorFrames
            );
            camaraManager.verificarPermisos();
        });
    }

    // galeria
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
                    (bitmap, vehiculos, vertices) -> {

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
                    (bitmap, vehiculos, vertices) -> {
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

        if(captureManager != null) captureManager.liberarRecursos();

        // para evitar fugas de memoria al cerrar la app
        if (ultimoFrameRenderizado != null) {
            ultimoFrameRenderizado.recycle();
            ultimoFrameRenderizado = null;
        }

        // liberar modelos
        if(vehicleHelper != null) vehicleHelper.close();

        if(vehicleHelperGaleria != null) vehicleHelperGaleria.close();

        if(plateHelper != null) plateHelper.close();

        if(crucesHelper != null) crucesHelper.close();
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

                // limpiamos overlay hasta que llegue el proximo frame
                olBoundingBox.limpiar();

                Log.d("ROTACION", "Nuevas dimensiones: " + nuevoAncho + "x" + nuevoAlto);
            }
        });
    }

    private void actualizarHistorial(){
        // Forzar actualización del historial
        runOnUiThread(() -> {
            if (viewPagerHistorial.getAdapter() != null) {
                viewPagerHistorial.getAdapter().notifyDataSetChanged();
                // Opcional: mover a la pestaña de capturas para ver el resultado
                viewPagerHistorial.setCurrentItem(0, true);
            }
        });
    }
}