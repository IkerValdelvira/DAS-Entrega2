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

// Diálogo que se muestra antes de crear una nueva lista de favoritos (tras pulsar la opción 'Crear nueva lista' en el diálogo 'DialogoAñadirFavoritos')
// Diálogo con diseño personalizado para introducir el nombre de la nueva lista a crear
public class DialogoCrearEtiqueta extends DialogFragment {

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

        miListener =(ListenerdelDialogo) getActivity();

        // Creación del diálogo con diseño personalizado mediante el layout 'anadir_lista_fav.xml'
        // El usuario introducirá el nombre de la nueva lista de favoritos en un EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.CrearEtiqueta));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.crear_etiqueta,null);
        builder.setView(view);

        EditText editTextEtiqueta = view.findViewById(R.id.ediTextEtiqueta);

        // Se define el botón 'positivo' --> Creará la nueva lista e insertará la película en ella
        builder.setPositiveButton(getString(R.string.Hecho), new DialogInterface.OnClickListener() {
            // Se ejeucta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String etiqueta = editTextEtiqueta.getText().toString();
                if(!etiqueta.isEmpty()){
                    // Si el nombre no está vacío, se crea la lista y se inserta la película en la base de datos local
                    miListener.crearEtiqueta(etiqueta);
                }
                else {
                    // Si el nombre está vacío se vuelve a crear el diálogo
                    Toast.makeText(getActivity(), getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
                    DialogFragment dialogoCrearEtiqueta = new DialogoCrearEtiqueta();
                    dialogoCrearEtiqueta.show(getActivity().getSupportFragmentManager(), "crear_etiqueta");
                }

            }
        });

        // Se define el botón 'negativo' --> Cancelará el diálogo actual
        builder.setNegativeButton(getString(R.string.Cancelar), new DialogInterface.OnClickListener() {
            // Se ejeucta al pulsar el botón 'negativo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }

}
