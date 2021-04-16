package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.entrega2.Actividades.MainActivity;
import com.example.entrega2.Actividades.SubirFotoActivity;
import com.example.entrega2.R;
import com.example.entrega2.Workers.CompartirFotoWorker;
import com.example.entrega2.Workers.InsertarFotoWorker;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

// Diálogo que se muestra antes de añadir una película a una lista de favoritos (tras pulsar el botón 'Añadir a favoritos' de la actividad 'PeliculaActivity')
// Diálogo con listado de opciónes para añadir la película a una lista ya creada o a una nueva lista a crear
public class DialogoCompartirFoto extends DialogFragment {

    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void fotoCompartida();
    }

    // Datos de la película y el usuario que se van a utilizar en el diálogo
    private String usuario;
    private String fotoID;
    private ArrayList<String> amigos;
    private String titulo;

    private String[] opciones;                  // Array con las opciones de listas de favoritos
    private ArrayList<String> elegidos;         // Array para almacenar las opciones elegidas

    // Constructor del diálogo
    public DialogoCompartirFoto(String pUsuario, String pFotoId, ArrayList<String> pAmigos, String pTitulo) {
        usuario = pUsuario;
        fotoID = pFotoId;
        amigos = pAmigos;
        titulo = pTitulo;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener = (ListenerdelDialogo) getActivity();

        // Creación del listado de opciones --> Las listas de favoritos disponibles se obtienen de la base de datos local y se guardan el el Array 'opciones'
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.ConQuien));

        elegidos = new ArrayList<>();

        opciones = new String[amigos.size()];
        for(int i=0; i<amigos.size(); i++) {
            opciones[i] = amigos.get(i);
        }

        // Se puede elegir más de una opción del listado (tipo checkbox)
        builder.setMultiChoiceItems(opciones, null, new DialogInterface.OnMultiChoiceClickListener() {
            // Se ejecuta al pulsar una opción del listado
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                // Se añade o elimina la opción pulsada al Array 'elegidos' dependiendo si ya estaba o no
                if (b == true){
                    elegidos.add(opciones[i]);
                }
                else {
                    elegidos.remove(opciones[i]);
                }
            }
        });

        // Se define el botón 'positivo' --> Añadirá la película a las listas seleccionadas entre las opciones
        builder.setPositiveButton(getString(R.string.Aceptar), new DialogInterface.OnClickListener() {
            // Se ejeucta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Se itera por cada opción (lista de favoritos) seleccionada
                String[] elegidosArray = new String[elegidos.size()];
                for(int j=0; j<elegidos.size(); j++) {
                    // Compartir con amigo
                    elegidosArray[j] = elegidos.get(j);
                }

                Data datos = new Data.Builder()
                        .putString("usuario", usuario)
                        .putString("imagen", fotoID)
                        .putString("titulo", titulo)
                        .putStringArray("amigos", elegidosArray)
                        .build();
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(CompartirFotoWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(getActivity(), status -> {
                            if (status != null && status.getState().isFinished()) {
                                miListener.fotoCompartida();
                            }
                        });

                WorkManager.getInstance(getActivity()).enqueue(otwr);

            }
        });

        // Se define el botón 'negativo' --> Cancelará el diálogo actual
        builder.setNegativeButton(getString(R.string.Cancelar), new DialogInterface.OnClickListener() {
            // Se ejeucta al pulsar el botón 'negativo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getActivity(), getString(R.string.FotoNoCompartida), Toast.LENGTH_SHORT).show();
            }
        });

        return builder.create();
    }
}
