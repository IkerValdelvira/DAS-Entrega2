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

import com.example.entrega2.Actividades.AmigosActivity;
import com.example.entrega2.R;
import com.example.entrega2.Workers.AmigosWorker;

// Adaptador para el ListView personalizado de los amigos de un usuario en la actividad AmigosActivity
public class AdaptadorListViewAmigos extends BaseAdapter{

    // Interfaz del listener para que las acciones de los elementos de la lista se ejecuten en la actividad que creó el adaptador (AmigosActivity)
    ListenerSolicitud miListener;
    public interface ListenerSolicitud {
        void alEliminar();
    }

    private AmigosActivity contexto;         // Contexto de la actividad que va a mostrar el ListView personalizado: AmigosActivity
    private LayoutInflater inflater;         // Inflater para el layout que represente una fila de la lista

    // Datos que se quieren mostrar
    private String[] usernames;

    private String usuario;                     // Nombre de usuario actual

    // Constructor del adaptador
    public AdaptadorListViewAmigos(String pUsuario, AmigosActivity pContext, String[] pUsernames)  {
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
        view = inflater.inflate(R.layout.fila_amigo,null);      // Se indica el layout para cada elemento: 'fila_amigo.xml'
        // Se obtienen los elementos del layout
        TextView textViewUsername = (TextView) view.findViewById(R.id.textViewUsername);
        Button botonEliminar = (Button) view.findViewById(R.id.buttonEliminar);
        // Listener 'onClick' del botón del layout para eliminar un amigo
        botonEliminar.setOnClickListener(new View.OnClickListener() {
            // Se ejecuta al pulsar el botón 'Eliminar' del layout 'fila_amigo.xml'
            @Override
            public void onClick(View v) {
                // Se elimina el amigo del usuario de la base de datos
                String amigo = usernames[i];
                // Información a enviar a la tarea
                Data datos = new Data.Builder()
                        .putString("funcion", "eliminar")
                        .putString("user", usuario)
                        .putString("friend", amigo)
                        .build();
                // Restricciones a cumplir: es necesaria la conexión a internet
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                // Se ejecuta el trabajo una única vez: 'AmigosWorker'
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(AmigosWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                // Recuperación de los resultados de la tarea
                WorkManager.getInstance(contexto).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(contexto, status -> {
                            // En caso de éxito 'Result.success()', se llama al método 'alEliminar' del listener para ejecutar la acción en la actividad
                            if (status != null && status.getState().isFinished()) {
                                Toast.makeText(contexto, contexto.getString(R.string.AmigoEliminado), Toast.LENGTH_SHORT).show();
                                miListener.alEliminar();
                            }
                        });
                WorkManager.getInstance(contexto).enqueue(otwr);
            }
        });

        // Se asigna a cada variable el contenido que se quiere mostrar en ese elemento: nombre de usuario
        textViewUsername.setText(usernames[i]);
        return view;
    }

}
