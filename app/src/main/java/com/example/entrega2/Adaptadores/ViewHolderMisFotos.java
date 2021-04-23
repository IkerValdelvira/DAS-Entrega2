package com.example.entrega2.Adaptadores;

import android.content.Intent;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.entrega2.Actividades.MainActivity;
import com.example.entrega2.R;

// ViewHolder para el RecyclerView que muestra las fotos propias del usuario en la actividad MainActivity
public class ViewHolderMisFotos extends RecyclerView.ViewHolder {

    public String usuario;          // Nombre de usuario actual

    // Atributos públicos para acceder a ellos desde el adaptador (AdaptadorRecyclerMisFotos)
    public String id;               // id de la foto

    // Elementos del CardView que representa una foto
    public ImageView imagen;
    public TextView titulo;
    public boolean[] seleccion;     // Array de booleanos para indicar qué elementos se han elegido

    // Contructor del ViewHolder
    public ViewHolderMisFotos(@NonNull View itemView){
        super(itemView);

        // Inicialización de los elementos definidos en el layout CardView (item_layout_mis_fotos.xml)
        imagen = itemView.findViewById(R.id.imageViewImagen);
        titulo = itemView.findViewById(R.id.textViewTitulo);
    }

}
