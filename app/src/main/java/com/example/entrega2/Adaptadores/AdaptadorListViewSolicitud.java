package com.example.entrega2.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.entrega2.Actividades.AnadirAmigoActivity;
import com.example.entrega2.R;
import com.example.entrega2.Workers.SolicitudesWorker;

// Adaptador para la ListView personalizada de las peliculas favoritas
public class AdaptadorListViewSolicitud extends BaseAdapter{

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que llamó al diálogo (PeliculaActivity)
    ListenerSolicitud miListener;
    public interface ListenerSolicitud {
        void alCambiar();
    }

    private AnadirAmigoActivity contexto;         // Contexto de la actividad que va a mostrar el ListView personalizado: FavoritosActivity
    private LayoutInflater inflater;            // Inflater para el layout que represente una fila de la lista

    // Datos que se quieren mostrar
    private String[] usernames;

    private String usuario;                     // Nombre de usuario actual

    // Constructor del adaptador
    public AdaptadorListViewSolicitud(String pUsuario, AnadirAmigoActivity pContext, String[] pUsernames)  {
        usuario = pUsuario;
        contexto = pContext;
        usernames = pUsernames;
        inflater = (LayoutInflater) contexto.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        miListener = (ListenerSolicitud) contexto;        // Se referencia a la implementación de la actividad
    }

    // Método sobrescrito de la clase BaseAdapter --> Devuelve el número de elementos
    @Override
    public int getCount() {
        return usernames.length;
    }

    // Método sobrescrito de la clase BaseAdapter --> Devuelve el elemento i
    @Override
    public Object getItem(int i) {
        return usernames[i];
    }

    // Método sobrescrito de la clase BaseAdapter --> Devuelve el identificador del elemento i
    @Override
    public long getItemId(int i) {
        return i;
    }

    // Método sobrescrito de la clase BaseAdapter --> Devuelve cómo se visualiza un elemento
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.fila_solicitud,null);      // Se indica el layout para cada elemento: 'fila_favoritos.xml'
        // Se obtienen los elementos del layout
        TextView textViewUsername = (TextView) view.findViewById(R.id.textViewUsername);

        Button botonAceptar = (Button) view.findViewById(R.id.buttonAceptar);
        // Listener 'onClick' del botón del layout para quitar la película de la lista de favoritos
        botonAceptar.setOnClickListener(new View.OnClickListener() {
            // Se ejecuta al pulsar el botón del layout para quitar la película de la lista de favoritos
            @Override
            public void onClick(View v) {
                // Se crea un diálogo preguntando si se quiere quitar realmente la película de la lista de favoritos seleccionada: DialogoQuitarFavoritos
                String amigo = usernames[i];
                // AÑADIR AMIGOS Y BORRAR SOLICITUD
                Data datos = new Data.Builder()
                        .putString("funcion", "gestionar")
                        .putString("user", usuario)
                        .putString("friend", amigo)
                        .putString("status", "accepted")
                        .build();
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(SolicitudesWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                WorkManager.getInstance(contexto).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(contexto, status -> {
                            if (status != null && status.getState().isFinished()) {
                                Toast.makeText(contexto, contexto.getString(R.string.SolicitudAceptada), Toast.LENGTH_SHORT).show();
                                miListener.alCambiar();
                            }
                        });
                WorkManager.getInstance(contexto).enqueue(otwr);
            }
        });

        Button botonRechazar = (Button) view.findViewById(R.id.buttonRechazar);
        // Listener 'onClick' del botón del layout para quitar la película de la lista de favoritos
        botonRechazar.setOnClickListener(new View.OnClickListener() {
            // Se ejecuta al pulsar el botón del layout para quitar la película de la lista de favoritos
            @Override
            public void onClick(View v) {
                // Se crea un diálogo preguntando si se quiere quitar realmente la película de la lista de favoritos seleccionada: DialogoQuitarFavoritos
                String amigo = usernames[i];
                // AÑADIR AMIGOS Y BORRAR SOLICITUD
                Data datos = new Data.Builder()
                        .putString("funcion", "gestionar")
                        .putString("user", usuario)
                        .putString("friend", amigo)
                        .putString("status", "refused")
                        .build();
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(SolicitudesWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                WorkManager.getInstance(contexto).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(contexto, status -> {
                            if (status != null && status.getState().isFinished()) {
                                Toast.makeText(contexto, contexto.getString(R.string.SolicitudRechazada), Toast.LENGTH_SHORT).show();
                                miListener.alCambiar();
                            }
                        });
                WorkManager.getInstance(contexto).enqueue(otwr);
            }
        });

        // Se asigna a cada variable el contenido que se quiere mostrar en ese elemento: título y portada de la película
        textViewUsername.setText(usernames[i]);
        return view;
    }

}
