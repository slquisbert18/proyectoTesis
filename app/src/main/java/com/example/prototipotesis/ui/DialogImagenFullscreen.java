package com.example.prototipotesis.ui;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.prototipotesis.R;

public class DialogImagenFullscreen extends DialogFragment {

    private String rutaImagen;
    private ImageButton botonCerrar;

    public DialogImagenFullscreen(String rutaImagen){
        this.rutaImagen = rutaImagen;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        return inflater.inflate(
                R.layout.dialog_imagen_fullscreen,
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

        ImageView imgFullscreen = view.findViewById(R.id.imgFullscreen);
        botonCerrar = view.findViewById(R.id.btnCerrarImagen);

        botonCerrar.setOnClickListener(v -> dismiss());

        imgFullscreen.setImageBitmap(
                BitmapFactory.decodeFile(rutaImagen)
        );

        // cerrar al tocar
        imgFullscreen.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart(){
        super.onStart();

        if(getDialog() != null){
            Window window = getDialog().getWindow();
            if(window != null){
                window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                window.setBackgroundDrawableResource(
                        android.R.color.black
                );
            }
        }
    }
}