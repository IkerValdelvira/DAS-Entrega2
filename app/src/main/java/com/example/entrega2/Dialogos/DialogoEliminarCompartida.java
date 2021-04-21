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

public class DialogoEliminarCompartida extends DialogFragment {

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener = (ListenerdelDialogo) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.EliminarCompartida));
        builder.setMessage(getString(R.string.SeguroEliminarCompartida));

        builder.setPositiveButton(getString(R.string.Si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Eliminar de la base de datos
                Data datos = new Data.Builder()
                        .putString("funcion", "eliminar")
                        .putString("usuario", usuario)
                        .putString("imagen", imagen)
                        .putString("amigo", amigo)
                        .build();
                Constraints restricciones = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(CompartidasWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

                WorkManager.getInstance(getActivity()).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(getActivity(), status -> {
                            if (status != null && status.getState().isFinished()) {
                                miListener.compartidaEliminada();
                            }
                        });

                WorkManager.getInstance(getActivity()).enqueue(otwr);
            }
        });

        builder.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }
}
