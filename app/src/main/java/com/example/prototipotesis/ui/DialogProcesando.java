package com.example.prototipotesis.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.prototipotesis.R;

public class DialogProcesando extends DialogFragment {

    private TextView txtEstado;

    // listener cancelacion
    public interface OnCancelarListener{
        void onCancelar();
    }

    private OnCancelarListener listener;

    public DialogProcesando(OnCancelarListener listener){
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(
            @Nullable Bundle savedInstanceState
    ){
        View view = LayoutInflater.from(
                requireContext()
        ).inflate(
                R.layout.dialog_procesando,
                null
        );

        txtEstado = view.findViewById(R.id.txtEstado);
        Button btnCancelar = view.findViewById(R.id.btnCancelar);

        btnCancelar.setOnClickListener(v -> {
            if(listener != null){
                listener.onCancelar();
            }
            dismiss();
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
        dialog.setCancelable(false);
        return dialog;
    }

    // actualizar texto
    public void actualizarEstado(
            String texto
    ){
        if(txtEstado != null){
            txtEstado.setText(texto);
        }
    }
}