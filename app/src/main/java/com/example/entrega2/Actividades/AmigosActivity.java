package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.example.entrega2.Adaptadores.AdaptadorListViewAmigos;
import com.example.entrega2.Adaptadores.AdaptadorListViewAnadir;
import com.example.entrega2.Adaptadores.AdaptadorListViewSolicitud;
import com.example.entrega2.R;
import com.example.entrega2.Workers.BuscarUsuariosWorker;
import com.example.entrega2.Workers.GetAmigosUsuarioWorker;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;

public class AmigosActivity extends AppCompatActivity implements AdaptadorListViewAmigos.ListenerSolicitud{

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // ListView personalizado para mostrar las películas favoritas de una lista del usuario
    private ListView listView;
    private String[] usernames;

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

        setContentView(R.layout.activity_amigos);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
        }

        listView = findViewById(R.id.listViewAmigos);

        mostrarAmigos();

    }

    private void mostrarAmigos() {
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GetAmigosUsuarioWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);
                            usernames = new String[jsonArray.length()];
                            for(int i = 0; i < jsonArray.length(); i++) {
                                usernames[i] = jsonArray.getString(i);
                            }
                            AdaptadorListViewAmigos adaptadorListView = new AdaptadorListViewAmigos(usuario, AmigosActivity.this, usernames);
                            listView.setAdapter(adaptadorListView);

                            if(usernames.length == 0) {
                                Toast.makeText(this, getString(R.string.NoAmigosAgregados), Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    @Override
    public void alEliminar() {
        mostrarAmigos();
    }
}