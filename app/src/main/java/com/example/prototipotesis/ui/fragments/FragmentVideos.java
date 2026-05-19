package com.example.prototipotesis.ui.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prototipotesis.R;
import com.example.prototipotesis.ui.AdaptadorHistorial;
import com.example.prototipotesis.ui.ArchivoHistorial;
import com.example.prototipotesis.ui.DialogVistaArchivo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FragmentVideos extends Fragment {
    private RecyclerView recyclerVideos;
    // adaptador global
    private AdaptadorHistorial adaptador;
    // lista global
    private List<ArchivoHistorial> lista = new ArrayList<>();

    public FragmentVideos() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_videos,
                container,
                false
        );
        recyclerVideos = view.findViewById(
                R.id.recyclerVideos
        );
        recyclerVideos.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        adaptador =
                new AdaptadorHistorial(
                        getContext(),
                        lista,
                        archivo -> {

                            DialogVistaArchivo dialog =
                                    new DialogVistaArchivo(
                                            archivo,
                                            new DialogVistaArchivo.OnArchivoActualizadoListener(){

                                                @Override
                                                public void onArchivoEliminado(
                                                        ArchivoHistorial archivo
                                                ){

                                                    lista.remove(archivo);

                                                    adaptador.notifyDataSetChanged();
                                                }

                                                @Override
                                                public void onArchivoRenombrado(
                                                        ArchivoHistorial archivo
                                                ){

                                                    adaptador.notifyDataSetChanged();
                                                }
                                            }
                                    );

                            dialog.show(
                                    getParentFragmentManager(),
                                    "dialogArchivo"
                            );
                        }
                );

        recyclerVideos.setAdapter(adaptador);
        // cargar videos
        cargarArchivos();

        return view;
    }

    // ================= CARGAR VIDEOS =================
    private void cargarArchivos(){
        lista.clear();
        File carpeta = new File(
                requireContext().getExternalFilesDir(
                        Environment.DIRECTORY_MOVIES
                ),
                "Grabaciones"
        );
        // crear carpeta si no existe
        if(!carpeta.exists()){
            carpeta.mkdirs();
        }
        File[] archivos = carpeta.listFiles();
        if(archivos != null){
            for(File archivo : archivos){
                lista.add(
                        new ArchivoHistorial(
                                archivo,
                                true
                        )
                );
            }
        }

        // refrescar recyclerview
        if(adaptador != null){
            adaptador.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // recargar videos automaticamente
        cargarArchivos();
    }
}