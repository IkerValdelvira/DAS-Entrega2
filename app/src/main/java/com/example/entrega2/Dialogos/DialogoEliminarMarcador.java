package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.entrega2.R;

// Diálogo que se muestra antes de eliminar un marcador (tras pulsar prolongadamente en un elemento del ListView de la actividad PuntosInteresActivity)
// Diálogo tipo alerta
public class DialogoEliminarMarcador extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (PuntosInteresActivity)
    ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void borrarMarcador(double latActual, double longActual, String texto);
    }

    private double latActual;
    private double longActual;
    private String texto;

    public DialogoEliminarMarcador(double latActual, double longActual, String texto){
        this.latActual = latActual;
        this.longActual = longActual;
        this.texto = texto;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener =(ListenerdelDialogo) getActivity();             // Se referencia a la implementación de la actividad

        // Creación del diálogo tipo alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.EliminarMarcador));
        builder.setMessage(getString(R.string.SeguroEliminarMarcador));

        // Se define el botón 'positivo' --> Eliminará el marcador del ListView y de la base de datos
        builder.setPositiveButton(getString(R.string.Si), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Se llama al método 'borrarMarcador' del listener en la actividad asociada
                miListener.borrarMarcador(latActual, longActual, texto);
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
