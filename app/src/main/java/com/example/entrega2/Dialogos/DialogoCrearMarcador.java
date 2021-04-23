package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.entrega2.R;
import com.google.android.gms.maps.model.LatLng;

// Diálogo que se muestra antes de crear un nuevo marcador (tras pulsar en el mapa de Google Maps de la actividad PuntosInteresActivity)
// Diálogo con diseño personalizado para introducir el nombre de la nueva lista a crear
public class DialogoCrearMarcador extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (PuntosInteresActivity)
    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void crearMarcador(LatLng latLng, String texto);
    }

    private LatLng latLng;

    public DialogoCrearMarcador(LatLng latLng){
        this.latLng = latLng;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener =(ListenerdelDialogo) getActivity();         // Se referencia a la implementación de la actividad

        // Creación del diálogo con diseño personalizado mediante el layout 'crear_marcador.xml'
        // El usuario introducirá el nombre del marcador en un EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.CrearNuevoMarcador));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.crear_marcador,null);
        builder.setView(view);

        EditText editTextNombre = view.findViewById(R.id.ediTextNombre);

        // Se define el botón 'positivo' --> Creará el nuevo marcador en el mapa de Google Maps
        builder.setPositiveButton(getString(R.string.Hecho), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String titulo = editTextNombre.getText().toString();
                if(!titulo.isEmpty()){
                    // Se llama al método 'crearMarcador' del listener en la actividad asociada
                    miListener.crearMarcador(latLng, titulo);
                }
                else {
                    // Si el nombre del marcador está vacío se vuelve a crear el diálogo
                    Toast.makeText(getActivity(), getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
                    DialogFragment dialogoCrearMarcador = new DialogoCrearMarcador(latLng);
                    dialogoCrearMarcador.show(getActivity().getSupportFragmentManager(), "crear_marcador");
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
