package com.example.entrega2.Adaptadores;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.entrega2.R;

// ViewHolder para el RecyclerView que muestra las fotos compartidas con el usuario en la actividad CompartidasActivity
public class ViewHolderCompartidas extends RecyclerView.ViewHolder {

    public String usuario;              // Nombre de usuario actual

    // Atributos públicos para acceder a ellos desde el adaptador (AdaptadorRecyclerCompartidas)
    public String id;                   // id de la foto

    // Elementos del CardView que representa una foto compartida
    public ImageView imagen;
    public TextView titulo;
    public TextView usuarioFoto;
    public boolean[] seleccion;         // Array de booleanos para indicar qué elementos se han elegido

    // Contructor del ViewHolder
    public ViewHolderCompartidas(@NonNull View itemView){
        super(itemView);

        // Inicialización de los elementos definidos en el layout CardView (item_layout_compartidas.xml)
        imagen = itemView.findViewById(R.id.imageViewImagenC);
        titulo = itemView.findViewById(R.id.textViewTituloC);
        usuarioFoto = itemView.findViewById(R.id.textViewCompartidaPor);
    }

}
