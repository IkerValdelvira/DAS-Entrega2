package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

import com.example.entrega2.R;
import com.example.entrega2.Workers.CompartidasWorker;

import java.util.ArrayList;

// Diálogo que se muestra antes de compartir una foto (tras pulsar el botón de compartir de la actividad InfoFotoActivity)
// Diálogo con listado de opciónes para añadir compartir la foto con uno o más amigos
public class DialogoCompartirFoto extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (InfoFotoActivity)
    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void fotoCompartida();
    }

    // Datos de la usuario y la imagen que se van a utilizar en el diálogo
    private String usuario;
    private String fotoID;
    private ArrayList<String> amigos;
    private String titulo;

    private String[] opciones;                  // Array con los amigos a los que compartir la foto
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

        miListener = (ListenerdelDialogo) getActivity();        // Se referencia a la implementación de la actividad

        // Creación del diálogo --> Se establecen los amigos a los que compartir la foto en el listado
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

        // Se define el botón 'positivo' --> Enviará un mensaje (notificación) a los amigos elegidos de la lista
        builder.setPositiveButton(getString(R.string.Aceptar), new DialogInterface.OnClickListener() {
            // Se ejeucta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Se itera por cada opción seleccionada y se añade el usuario al array de elegidos
                String[] elegidosArray = new String[elegidos.size()];
                for(int j=0; j<elegidos.size(); j++) {
                    elegidosArray[j] = elegidos.get(j);
                }

                // Información a enviar a la tarea
                Data datos = new Data.Builder()
                        .putString("funcion", "compartir")
                        .putString("usuario", usuario)
                        .putString("imagen", fotoID)
                        .putString("titulo", titulo)
                        .putStringArray("amigos", elegidosArray)
                        .build();
                // Restricciones a cumplir: es necesaria la conexión a internet
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                // Se ejecuta el trabajo una única vez: 'CompartidasWorker'
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(CompartidasWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                // Recuperación de los resultados de la tarea
                WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(getActivity(), status -> {
                            // En caso de éxito 'Result.success()', se llama al método 'fotoCompartida' del listener para ejecutar la acción en la actividad
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
