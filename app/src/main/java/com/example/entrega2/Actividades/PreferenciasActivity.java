package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import com.example.entrega2.Preferencias;
import com.example.entrega2.R;
import com.example.entrega2.ServicioMusicaNotificacion;

import java.util.Locale;

public class PreferenciasActivity extends AppCompatActivity implements Preferencias.ListenerPreferencias {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Acceso al las preferencias para obtener el valor de 'idioma'
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma", "es");

        // Crear nueva localización con el idioma recogido de las preferencias (necesario para mantener el idioma tras cambio de orientacion del dispositivo)
        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        setContentView(R.layout.activity_preferencias);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
        }
    }

    // Método sobrescrito de la interfaz 'Preferencias.ListenerPreferencias' --> Se ejecuta al cambiar la preferencia 'idioma'
    @Override
    public void alCambiarIdioma() {
        // Se destruye la actividad y se vuelve a crear --> Al crearse de nuevo se establecerá la nueva localización en el método 'onCreate'
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("usuario", usuario);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}