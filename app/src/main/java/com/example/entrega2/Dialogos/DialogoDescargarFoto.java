package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.entrega2.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Diálogo que se muestra antes de descargar una foto al almacenamiento local del dispositivo (tras pulsar prolongadamente en un elemento del RecyclerView de la actividad MainActivity)
// Diálogo tipo alerta
public class DialogoDescargarFoto extends DialogFragment {

    // Interfaz del listener para que las acciones del diálogo se ejecuten en la actividad que creó el diálogo (MainActivity)
    private ListenerdelDialogo miListener;
    public interface ListenerdelDialogo {
        void descargarFoto(Uri uri);
    }

    private String imagen;

    public DialogoDescargarFoto(String pImagen){
        this.imagen = pImagen;
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
        builder.setTitle(getString(R.string.DescargarFoto));
        builder.setMessage(getString(R.string.SeguroDescargar));

        // Se define el botón 'positivo' --> Descargará la imagen al almacenamiento local del dispositivo
        builder.setPositiveButton(getString(R.string.Si), new DialogInterface.OnClickListener() {
            // Se ejecuta al pulsar el botón 'positivo'
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Se descarga la imagen del almacenamiento Firebase Cloud Storage y se llama al método 'descargarFoto' del listener en la actividad asociada
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference pathReference = storageRef.child(imagen);
                pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // Se llama al método 'descargarFoto' del listener en la actividad asociada
                        miListener.descargarFoto(uri);
                    }
                });
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
