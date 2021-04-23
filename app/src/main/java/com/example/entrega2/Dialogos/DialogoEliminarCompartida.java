package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

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

// Diálogo que se muestra antes de eliminar una foto compartida (tras pulsar prolongadamente en un elemento del RecyclerView de la actividad CompartidasActivity)
// Diálogo tipo alerta
public class DialogoEliminarCompartida extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (CompartidasActivity)
    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void compartidaEliminada();
    }

    private String usuario;
    private String imagen;
    private String amigo;

    public DialogoEliminarCompartida(String pUsuario, String pImagen, String pAmigo){
        this.usuario = pUsuario;
        this.imagen = pImagen;
        this.amigo = pAmigo;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener = (ListenerdelDialogo) getActivity();            // Se referencia a la implementación de la actividad

        // Creación del diálogo tipo alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.EliminarCompartida));
        builder.setMessage(getString(R.string.SeguroEliminarCompartida));

        // Se define el botón 'positivo' --> Eliminará la foto compartida de la base de datos
        builder.setPositiveButton(getString(R.string.Si), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Información a enviar a la tarea
                Data datos = new Data.Builder()
                        .putString("funcion", "eliminar")
                        .putString("usuario", usuario)
                        .putString("imagen", imagen)
                        .putString("amigo", amigo)
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
                            // En caso de éxito 'Result.success()', se llama al método 'compartidaEliminada' del listener para ejecutar la acción en la actividad
                            if (status != null && status.getState().isFinished()) {
                                miListener.compartidaEliminada();
                            }
                        });

                WorkManager.getInstance(getActivity()).enqueue(otwr);
            }
        });

        // Se define el botón 'negativo' --> Cancelará el diálogo actual
        builder.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'negativo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }
}
