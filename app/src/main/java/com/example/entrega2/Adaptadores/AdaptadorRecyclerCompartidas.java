
package com.example.entrega2.Adaptadores;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.entrega2.Actividades.CompartidasActivity;
import com.example.entrega2.Actividades.InfoFotoActivity;
import com.example.entrega2.Actividades.MainActivity;
import com.example.entrega2.Dialogos.DialogoCompartirFoto;
import com.example.entrega2.Dialogos.DialogoEliminarCompartida;
import com.example.entrega2.Dialogos.DialogoEnviarComentario;
import com.example.entrega2.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Adaptador para el RecyclerView de fotos compartidas en la actividad CompartidasActivity
public class AdaptadorRecyclerCompartidas extends RecyclerView.Adapter<ViewHolderCompartidas> {

    // Datos que se quieren mostrar
    private String[] ids;
    private String[] usuarios;
    private String[] titulos;
    private boolean[] seleccionados;            // Array de booleanos para indicar qué elementos se han elegido

    private String usuario;                     // Nombre de usuario actual

    private CompartidasActivity contexto;       // Contexto de la actividad que va a mostrar el ListView personalizado: CompartidasActivity

    // Constructor del adaptador
    public AdaptadorRecyclerCompartidas(CompartidasActivity pContexto, String pUsuario, String[] pIds, String[] pUsuarios, String[] pTitulos) {
        contexto = pContexto;
        usuario = pUsuario;
        ids = pIds;
        usuarios = pUsuarios;
        titulos = pTitulos;
        seleccionados = new boolean[titulos.length];
    }

    // 'Infla' el layout definido para cada elemento (item_layout_compartidas.xml) y crea y devuelve una instancia de ViewHolder
    @NonNull
    @Override
    public ViewHolderCompartidas onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutDeCadaItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout_compartidas, parent, false);
        ViewHolderCompartidas viewHolder = new ViewHolderCompartidas(layoutDeCadaItem);
        viewHolder.seleccion = seleccionados;
        return viewHolder;
    }

    // Asigna a los atributos del ViewHolder los valores a mostrar para una posición concreta
    @Override
    public void onBindViewHolder(@NonNull ViewHolderCompartidas holder, int position) {
        holder.usuario = usuario;
        holder.id = ids[position];
        holder.titulo.setText(titulos[position]);
        holder.usuarioFoto.setText(contexto.getString(R.string.CompartidaPor) + " " +  usuarios[position]);

        // Se descarga la imagen del almacenamiento Firebase Cloud Storage y se muestra en el ImageView
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child(ids[position]);
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(contexto).load(uri).into(holder.imagen);
            }
        });

        // Listener 'onLongClick' para cada View
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            // Se ejecuta al pulsar prolongadamente en un itemView (cuando se pulsa prolongadamente en un CardView que contiene la información de una foto)
            @Override
            public boolean onLongClick(View view) {
                // Se crea un diálogo DialogoEliminarCompartida para confirmar que el usuario quiere eliminar la foto compartida
                DialogFragment dialogoEliminarCompartida = new DialogoEliminarCompartida(holder.usuarioFoto.getText().toString().split(" ")[1], holder.id, usuario);
                dialogoEliminarCompartida.show(contexto.getSupportFragmentManager(), "eliminar_compartida");
                return false;
            }
        });

        // Listener 'onClick' para cada View
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            // Se ejecuta al pulsar en un itemView (cuando se pulsa en un CardView que contiene la información de una foto)
            @Override
            public void onClick(View view) {
                // Se crea un diálogo DialogoEnviarComentario que deja escribir un comentario para enviar al usuario propietario de la foto compartida
                DialogFragment dialogoEnviarComentario = new DialogoEnviarComentario(holder.usuarioFoto.getText().toString().split(" ")[1], holder.titulo.getText().toString());
                dialogoEnviarComentario.show(contexto.getSupportFragmentManager(), "enviar_comentario");
            }
        });
    }

    // Devuelve la cantidad de datos total a mostrar
    @Override
    public int getItemCount() {
        return titulos.length;
    }

}