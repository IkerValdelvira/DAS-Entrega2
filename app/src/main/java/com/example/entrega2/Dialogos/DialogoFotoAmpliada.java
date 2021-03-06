package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.example.entrega2.Actividades.InfoFotoActivity;
import com.example.entrega2.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

// Diálogo que se muestra para visualizar una foto en tamaño pantalla completa (tras pulsar en el ImageView con la foto en la activiad 'InfoFotoActivity')
// Diálogo con diseño personalizado para mostar la imagen en un ImageView en tamaño pantalla completa
public class DialogoFotoAmpliada extends DialogFragment {

    private String imagen;

    public DialogoFotoAmpliada(String pImagen) {
        imagen = pImagen;
    }

    // Se ejecuta al crearse el diálogo
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        // Creación del diálogo con diseño personalizado mediante el layout 'foto_ampliada.xml'
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.foto_ampliada,null);
        builder.setView(view);

        ImageView imageViewFotoAmpliada = view.findViewById(R.id.imageViewFotoAmpliada);

        // Se descarga la imagen del almacenamiento Firebase Cloud Storage y se carga en el ImageView
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference pathReference = storageRef.child(imagen);
        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getActivity()).load(uri).into(imageViewFotoAmpliada);
            }
        });

        return builder.create();
    }

}
