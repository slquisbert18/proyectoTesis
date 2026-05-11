package com.example.prototipotesis.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.prototipotesis.R;

public class DialogVideoFullscreen extends DialogFragment {

    private String rutaVideo;
    private ExoPlayer player;

    public DialogVideoFullscreen(String rutaVideo){
        this.rutaVideo = rutaVideo;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ){
        return inflater.inflate(
                R.layout.dialog_video_fullscreen,
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
        PlayerView playerView = view.findViewById(R.id.playerFullscreen);
        ImageButton btnCerrar = view.findViewById(R.id.btnCerrarFullscreen);

        player = new ExoPlayer.Builder(
                requireContext()
        ).build();

        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(
                        rutaVideo
                );

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        btnCerrar.setOnClickListener(v -> dismiss());
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

    @Override
    public void onDestroyView(){
        super.onDestroyView();

        if(player != null){
            player.release();
            player = null;
        }
    }
}