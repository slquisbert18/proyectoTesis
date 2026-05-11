package com.example.prototipotesis.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.prototipotesis.R;
import com.example.prototipotesis.utils.HistorialUtils;

import java.util.List;

public class AdaptadorHistorial extends RecyclerView.Adapter<AdaptadorHistorial.ViewHolder>{

    private Context context;
    private List<ArchivoHistorial> lista;
    private OnArchivoClickListener listener; // variable para detectar toques dentro del historial
    // cuando tocas un archivo del historial, este es retornado
    public interface OnArchivoClickListener{
        void onArchivoClick(ArchivoHistorial archivo);
    }

    public AdaptadorHistorial(
            Context context,
            List<ArchivoHistorial> lista,
            OnArchivoClickListener listener
    ){
        this.context = context;
        this.lista = lista;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imgMiniatura;
        TextView txtNombre;
        ImageButton botonMenu;

        //TextView txtInfo;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            imgMiniatura = itemView.findViewById(R.id.imgMiniatura);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            botonMenu = itemView.findViewById(R.id.btnMenu);
            //txtInfo = itemView.findViewById(R.id.txtInfo);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ){

        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_historial,
                        parent,
                        false
                );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ){

        ArchivoHistorial item = lista.get(position);
        holder.txtNombre.setText(
                item.archivo.getName()
        );
        //holder.txtInfo.setText(
        //        (item.esVideo ? "Video" : "Imagen")
        //);

        // MINIATURAS
        Bitmap miniatura;
        if(item.esVideo){
            miniatura = ThumbnailUtils.createVideoThumbnail(
                            item.archivo.getAbsolutePath(),
                            MediaStore.Images.Thumbnails.MINI_KIND
                    );
        }
        else{
            miniatura = BitmapFactory.decodeFile(
                    item.archivo.getAbsolutePath()
            );
        }
        holder.imgMiniatura.setImageBitmap(miniatura);
        holder.itemView.setOnClickListener(v -> {
            if(listener != null){
                listener.onArchivoClick(item);
            }
        });

        // boton menu (3 puntitos)
        holder.botonMenu.setOnClickListener(v -> {
            PopupMenu popupMenu =
                    new PopupMenu(
                            v.getContext(),
                            holder.botonMenu
                    );

            popupMenu.inflate(R.menu.item_menu_historial);
            popupMenu.setOnMenuItemClickListener(itemM -> {
                int id = itemM.getItemId();
                if(id == R.id.menuCompartir){
                    HistorialUtils.compartirArchivo(context, item);
                    return true;
                }
                else if(id == R.id.menuRenombrar){
                    EditText editText = new EditText(context);
                    String nombreActual = item.archivo.getName();
                    int punto = nombreActual.lastIndexOf(".");

                    String nombreSinExtension = (punto > 0) ? nombreActual.substring(0, punto) : nombreActual;

                    editText.setText(nombreSinExtension);

                    new AlertDialog.Builder(context)
                            .setTitle("Renombrar archivo")
                            .setView(editText)
                            .setPositiveButton(
                                    "Renombrar",
                                    (dialog, which) -> {
                                        String nuevoNombre =
                                                editText.getText()
                                                        .toString()
                                                        .trim();

                                        boolean renombrado = HistorialUtils.renombrarArchivo(item, nuevoNombre);

                                        if(renombrado){
                                            notifyItemChanged(position);
                                            Toast.makeText(
                                                    context,
                                                    "Archivo renombrado",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    })

                            .setNegativeButton(
                                    "Cancelar",
                                    null
                            )
                            .show();
                    return true;
                }
                else if(id == R.id.menuEliminar){
                    new AlertDialog.Builder(context)
                            .setTitle("Eliminar archivo")
                            .setMessage("¿Deseas eliminar este archivo?")
                            .setPositiveButton(
                                    "Eliminar",
                                    (dialog, which) -> {

                                        boolean eliminado = HistorialUtils.eliminarArchivo(item);
                                        if(eliminado){
                                            lista.remove(position);
                                            notifyItemRemoved(position);
                                            Toast.makeText(
                                                    context,
                                                    "Archivo eliminado",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    })
                            .setNegativeButton(
                                    "Cancelar",
                                    null
                            )
                            .show();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}