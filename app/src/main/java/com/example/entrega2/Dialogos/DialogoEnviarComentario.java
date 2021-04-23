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

// Diálogo que se muestra antes de enviar un comentario a un usuario en una foto (tras pulsar en un elemento del RecyclerView de la actividad CompartidasActivity)
// Diálogo con diseño personalizado para introducir el comentario
public class DialogoEnviarComentario extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (CompartidasActivity)
    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void enviarComentario(String amigo, String titulo, String comentario);
    }

    private String amigo;
    private String titulo;

    public DialogoEnviarComentario(String pAmigo, String pTitulo){
        amigo = pAmigo;
        titulo = pTitulo;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener =(ListenerdelDialogo) getActivity();             // Se referencia a la implementación de la actividad

        // Creación del diálogo con diseño personalizado mediante el layout 'comentar_foto.xml'
        // El usuario introducirá el comentario en un EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.ComentarFoto));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.comentar_foto,null);
        builder.setView(view);

        EditText editTextComentario = view.findViewById(R.id.editTextComentario);

        // Se define el botón 'positivo' --> Enviará el comentario (notificación) al usuario propietario de la foto compartida
        builder.setPositiveButton(getString(R.string.Hecho), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String comentario = editTextComentario.getText().toString();
                if(!comentario.isEmpty()){
                    // Se llama al método 'enviarComentario' del listener en la actividad asociada
                    miListener.enviarComentario(amigo, titulo, comentario);
                }
                else {
                    // Si el comentario está vacío se vuelve a crear el diálogo
                    Toast.makeText(getActivity(), getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
                    DialogFragment dialogoEnviarComentario = new DialogoEnviarComentario(amigo, titulo);
                    dialogoEnviarComentario.show(getActivity().getSupportFragmentManager(), "enviar_comentario");
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
