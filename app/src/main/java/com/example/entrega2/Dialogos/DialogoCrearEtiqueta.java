package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.entrega2.R;
import com.google.android.gms.maps.model.LatLng;

// Diálogo que se muestra antes de crear una nueva etiqueta para la foto (tras pulsar el botón 'Añadir' en la actividad InfoFotoActivity o SubirFotoActivity)
// Diálogo con diseño personalizado para introducir la etiqueta a añadir
public class DialogoCrearEtiqueta extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (InfoFotoActivity o SubirFotoActivity)
    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void crearEtiqueta(String etiqueta);
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener =(ListenerdelDialogo) getActivity();         // Se referencia a la implementación de la actividad

        // Creación del diálogo con diseño personalizado mediante el layout 'crear_etiqueta.xml'
        // El usuario introducirá la etiqueta en un EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.CrearEtiqueta));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.crear_etiqueta,null);
        builder.setView(view);

        EditText editTextEtiqueta = view.findViewById(R.id.ediTextEtiqueta);

        // Se define el botón 'positivo' --> Añadirá la nueva etiqueta a la lista de etiquetas de la foto
        builder.setPositiveButton(getString(R.string.Hecho), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String etiqueta = editTextEtiqueta.getText().toString();
                if(!etiqueta.isEmpty()){
                    // Se llama al método 'crearEtiqueta' del listener en la actividad asociada
                    miListener.crearEtiqueta(etiqueta);
                }
                else {
                    // Si la etiqueta está vacía se vuelve a crear el diálogo
                    Toast.makeText(getActivity(), getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
                    DialogFragment dialogoCrearEtiqueta = new DialogoCrearEtiqueta();
                    dialogoCrearEtiqueta.show(getActivity().getSupportFragmentManager(), "crear_etiqueta");
                }

            }
        });

        // Se define el botón 'negativo' --> Cancelará el diálogo actual
        builder.setNegativeButton(getString(R.string.Cancelar), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'negativo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }

}
