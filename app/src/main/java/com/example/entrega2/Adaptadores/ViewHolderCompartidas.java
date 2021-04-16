package com.example.entrega2.Adaptadores;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.entrega2.R;

// ViewHolder para el RecyclerView que muestra el catálogo de películas
public class ViewHolderCompartidas extends RecyclerView.ViewHolder {

    public String usuario;

    // Atributos públicos para acceder a ellos desde el adaptador (AdaptadorRecycler)
    public String id;           // id de la película
    // Elementos del CardView que representa una película en el catálogo
    public ImageView imagen;
    public TextView titulo;
    public TextView usuarioFoto;
    public boolean[] seleccion;     // Array de booleanos para indicar qué elementos se han elegido

    // Contructor del ViewHolder
    public ViewHolderCompartidas(@NonNull View itemView){
        super(itemView);

        // Inicialización de los elementos definidos en el layout CardView (item_layout.xml)
        imagen = itemView.findViewById(R.id.imageViewImagenC);
        titulo = itemView.findViewById(R.id.textViewTituloC);
        usuarioFoto = itemView.findViewById(R.id.textViewCompartidaPor);
    }

}
