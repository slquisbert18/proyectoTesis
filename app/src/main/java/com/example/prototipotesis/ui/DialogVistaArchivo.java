package com.example.prototipotesis.ui;
import android.os.Bundle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.prototipotesis.R;
import com.example.prototipotesis.utils.HistorialUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class DialogVistaArchivo extends DialogFragment {
    private ArchivoHistorial archivo;
    private OnArchivoActualizadoListener listener;
    private ExoPlayer player;
    private ImageButton botonCerrar;
    private ImageButton botonCompartir;
    private ImageButton botonEditar;
    private ImageButton botonEliminar;

    public DialogVistaArchivo(ArchivoHistorial archivo, OnArchivoActualizadoListener listener){
        this.listener = listener;
        this.archivo = archivo;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        return inflater.inflate(
                R.layout.dialog_vista_archivo,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ){
        super.onViewCreated(view, savedInstanceState);
        ImageView imgVista = view.findViewById(R.id.imgVista);
        PlayerView playerView = view.findViewById(R.id.playerView);
        TextView txtMetadata = view.findViewById(R.id.txtMetadata);
        ImageButton btnFullscreen = view.findViewById(R.id.btnFullscreen);
        botonCerrar = view.findViewById(R.id.btnCerrarDialog);
        botonCompartir = view.findViewById(R.id.btnCompartir);
        botonEditar = view.findViewById(R.id.btnEditarNombre);
        botonEliminar = view.findViewById(R.id.btnEliminar);

        botonCompartir.setOnClickListener(v -> {
            HistorialUtils.compartirArchivo(
                    requireContext(),
                    archivo
            );
        });
        botonEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar archivo")
                    .setMessage("¿Deseas eliminar este archivo?")

                    .setPositiveButton(
                            "Eliminar",
                            (dialog, which) -> {
                                boolean eliminado = HistorialUtils.eliminarArchivo(archivo);
                                if(eliminado){
                                    if(listener != null){
                                        listener.onArchivoEliminado(archivo);
                                    }

                                    Toast.makeText(
                                            requireContext(),
                                            "Archivo eliminado",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    dismiss(); // cerrar dialog
                                }
                            })
                    .setNegativeButton(
                            "Cancelar",
                            null
                    )
                    .show();
        });

        botonEditar.setOnClickListener(v -> {
            EditText editText = new EditText(requireContext());
            // nombre actual
            String nombreActual = archivo.archivo.getName();
            int punto = nombreActual.lastIndexOf(".");

            String nombreSinExtension = (punto > 0) ? nombreActual.substring(0, punto) : nombreActual;

            editText.setText(nombreSinExtension);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Renombrar archivo")
                    .setView(editText)
                    .setPositiveButton(
                            "Renombrar",
                            (dialog, which) -> {

                                String nuevoNombre =
                                        editText.getText()
                                                .toString()
                                                .trim();

                                boolean renombrado = HistorialUtils.renombrarArchivo(
                                                archivo,
                                                nuevoNombre
                                );

                                if(renombrado){
                                    if(listener != null){
                                        listener.onArchivoRenombrado(archivo);
                                    }
                                    Toast.makeText(
                                            requireContext(),
                                            "Archivo renombrado",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    // actualizar metadata
                                    txtMetadata.setText(
                                            "Nombre: " + archivo.archivo.getName()
                                    );
                                }
                            })
                    .setNegativeButton(
                            "Cancelar",
                            null
                    )
                    .show();
        });

        botonCerrar.setOnClickListener(v -> dismiss());

        // si es video
        if(archivo.esVideo){
            imgVista.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            // crear reproductor
            player = new ExoPlayer.Builder(requireContext()).build();

            // conectar player al PlayerView
            playerView.setPlayer(player);

            // crear media item
            MediaItem mediaItem = MediaItem.fromUri(
                    archivo.archivo.toURI().toString()
            );

            // mostramos boton para pantalla completa
            btnFullscreen.setVisibility(View.VISIBLE);
            btnFullscreen.setOnClickListener(v -> {
                DialogVideoFullscreen dialog =
                        new DialogVideoFullscreen(
                                archivo.archivo.getAbsolutePath()
                        );
                dialog.show(
                        getParentFragmentManager(),
                        "VIDEO_FULLSCREEN"
                );
            });

            // asignar video
            player.setMediaItem(mediaItem);

            // preparar reproductor
            player.prepare();

            // reproducir al abrir
            player.play();
        }
        else{
            playerView.setVisibility(View.GONE);
            imgVista.setVisibility(View.VISIBLE);
            btnFullscreen.setVisibility(View.GONE);

            Bitmap bitmap = BitmapFactory.decodeFile(
                    archivo.archivo.getAbsolutePath()
            );

            imgVista.setImageBitmap(bitmap);
            imgVista.setOnClickListener(v -> {
                DialogImagenFullscreen dialog =
                        new DialogImagenFullscreen(
                                archivo.archivo.getAbsolutePath()
                        );

                dialog.show(
                        getParentFragmentManager(),
                        "IMG_FULLSCREEN"
                );
            });
        }

        // metadata
        String metadata =
                "Nombre: " + archivo.archivo.getName() + "\n\n" +
                        "Tamaño: " + (archivo.archivo.length()/1024) + " KB\n\n" +
                        "Ruta: " + archivo.archivo.getAbsolutePath();

        txtMetadata.setText(metadata);
    }

    @Override
    public void onStart(){
        super.onStart();
        if(getDialog() != null){
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    // si se cierra el dialog el video deja de reproducirse
    @Override
    public void onPause(){
        super.onPause();
        if(player != null) player.pause();
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();

        if(player != null){
            player.release();
            player = null;
        }
    }

    public interface OnArchivoActualizadoListener{
        void onArchivoEliminado(ArchivoHistorial archivo);
        void onArchivoRenombrado(ArchivoHistorial archivo);
    }
}