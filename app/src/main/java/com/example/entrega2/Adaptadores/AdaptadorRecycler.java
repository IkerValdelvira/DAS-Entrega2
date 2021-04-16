
package com.example.entrega2.Adaptadores;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.entrega2.Actividades.InfoFotoActivity;
import com.example.entrega2.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;

// Adaptador para el RecyclerView del catálogo de películas
public class AdaptadorRecycler extends RecyclerView.Adapter<ViewHolder> {

    // Datos que se quieren mostrar
    private String[] ids;
    private String[] titulos;
    private boolean[] seleccionados;            // Array de booleanos para indicar qué elementos se han elegido

    private String usuario;                     // Nombre de usuario actual

    private Context contexto;

    // Constructor del adaptador
    public AdaptadorRecycler(Context pContexto, String pUsuario, String[] pIds, String[] pTitulos) {
        contexto = pContexto;
        usuario = pUsuario;
        ids = pIds;
        titulos = pTitulos;
        seleccionados = new boolean[titulos.length];
    }

    // 'Infla' el layout definido para cada elemento (item_layout.xml) y crea y devuelve una instancia de ViewHolder
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutDeCadaItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(layoutDeCadaItem);
        viewHolder.seleccion = seleccionados;
        return viewHolder;
    }

    // Asigna a los atributos del ViewHolder los valores a mostrar para una posición concreta
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.usuario = usuario;
        holder.id = ids[position];
        holder.titulo.setText(titulos[position]);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child(ids[position]);
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(contexto).load(uri).into(holder.imagen);
            }
        });

        // Listener 'onClick' para cada View
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            // Se ejecuta al pulsar en un itemView (cuando se pulsa en un CardView que contiene la información de una película)
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(contexto, InfoFotoActivity.class);
                intent.putExtra("usuario", usuario);
                intent.putExtra("foto", holder.id);
                contexto.startActivity(intent);
            }
        });
    }

    // Devuelve la cantidad de datos total a mostrar
    @Override
    public int getItemCount() {
        return titulos.length;
    }

}