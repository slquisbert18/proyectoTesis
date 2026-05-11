package com.example.prototipotesis.ui;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.prototipotesis.R;

import java.io.IOException;

public class DialogPreProcesamiento extends DialogFragment {

    private Uri uri;
    private boolean esVideo;
    private ExoPlayer player;

    // listener procesar
    public interface OnProcesarListener{
        void onProcesar(
                Uri uri,
                boolean esVideo
        );
    }

    private OnProcesarListener listener;

    public DialogPreProcesamiento(
            Uri uri,
            boolean esVideo,
            OnProcesarListener listener
    ){
        this.uri = uri;
        this.esVideo = esVideo;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable android.view.ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){

        return inflater.inflate(
                R.layout.dialog_vista_previa,
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
        ImageView imgPreview = view.findViewById(R.id.imgPreview);
        PlayerView playerView = view.findViewById(R.id.playerViewPreview);
        Button btnCancelar = view.findViewById(R.id.btnCancelar);
        Button btnProcesar = view.findViewById(R.id.btnProcesar);

        // ================= VIDEO =================
        if(esVideo){
            imgPreview.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            player = new ExoPlayer.Builder(
                    requireContext()
            ).build();

            playerView.setPlayer(player);
            MediaItem mediaItem = MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

        }

        // ================= IMAGEN =================
        else{
            playerView.setVisibility(View.GONE);
            imgPreview.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try{
                    Bitmap bitmap;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                        bitmap = ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(
                                        requireContext().getContentResolver(),
                                        uri
                                )
                        );
                    }
                    else{
                        bitmap = MediaStore.Images.Media.getBitmap(
                                requireContext().getContentResolver(),
                                uri
                        );
                    }
                    requireActivity().runOnUiThread(() -> {
                        imgPreview.setImageBitmap(bitmap);
                    });

                }catch(IOException e){
                    e.printStackTrace();
                }
            }).start();

        }

        // cancelar
        btnCancelar.setOnClickListener(v -> dismiss());
        // procesar
        btnProcesar.setOnClickListener(v -> {
            if(listener != null){
                listener.onProcesar(
                        uri,
                        esVideo
                );
            }
            dismiss();
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(player != null){
            player.release();
            player = null;
        }
    }
}