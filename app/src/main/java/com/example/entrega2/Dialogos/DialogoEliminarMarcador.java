package com.example.entrega2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.entrega2.R;

public class DialogoEliminarMarcador extends DialogFragment {

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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        setRetainInstance(true);        // Mantiene la información del dialogo tras rotación del dispositivo

        miListener =(ListenerdelDialogo) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.EliminarMarcador));
        builder.setMessage(getString(R.string.SeguroEliminarMarcador));

        builder.setPositiveButton(getString(R.string.Si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                miListener.borrarMarcador(latActual, longActual, texto);
            }
        });

        builder.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {}
        });

        return builder.create();
    }
}
