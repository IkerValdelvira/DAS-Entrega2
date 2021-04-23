package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.entrega2.R;

// Diálogo que muestra la explicación de por qué es necesario el permiso de geolocalización (en las actividades SubirFotoActivity, UbicacionActivity y PuntosInteresActivity)
// Diálogo tipo alerta
public class DialogoPermisosLocalizacion extends DialogFragment {

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        // Creación del diálogo tipo alerta
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.PermisosLocalizacion));
        builder.setMessage(getString(R.string.RazonPermisosLocalizacion));

        // Se define el botón 'positivo' --> Cerrará el diálogo
        builder.setPositiveButton(getString(R.string.DeAcuerdo), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }
}
