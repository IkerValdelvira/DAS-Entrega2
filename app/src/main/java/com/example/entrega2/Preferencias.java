package com.example.entrega2;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

// Fragment encargado de gestionar las preferencias del usuario de la aplicacion a través de la clase SharedPreferences
public class Preferencias extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Interfaz del listener para que los cambios de las preferencias tengan efecto en la actividad desde donde se han cambiado (MainActivity)
    ListenerPreferencias miListener;
    public interface ListenerPreferencias {
        void alCambiarIdioma();
    }

    // Se ejecuta al crear las preferencias
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.conf_preferencias);            // Se indica cuál es el fichero XML donde están definidas las preferencias: conf_preferencias.xml
        miListener = (ListenerPreferencias) getActivity();              // Se referencia a la implementación de la actividad
    }

    // Se ejecuta al detectar cambios en el fichero de preferencias
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Si se ha cambiado la preferencia 'idioma', llama al método 'alCambiarIdioma' del listener que se ejecutará en la actividad
        if("idioma".equals(s)){
            miListener.alCambiarIdioma();
        }
    }

    // Se registra el listener 'OnSharedPreferencesChangeListener'
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    // Se desregistra el listener 'OnSharedPreferencesChangeListener'
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
