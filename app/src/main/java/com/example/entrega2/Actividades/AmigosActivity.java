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
import com.example.entrega2.R;
import com.example.entrega2.Workers.AmigosWorker;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;

// Actividad que muestra un ListView personalizado con los nombres de los amigos del usuario y da la opción de eliminarlos.
public class AmigosActivity extends AppCompatActivity implements AdaptadorListViewAmigos.ListenerSolicitud {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // ListView personalizado para mostrar los nombres de los amigos del usuario
    private ListView listView;
    private String[] usernames;

    // Se ejecuta al crearse la actividad
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

        // Inicialización del elemento 'listView' del layout 'activity_amigos.xml'
        listView = findViewById(R.id.listViewAmigos);

        mostrarAmigos();
    }

    // Método encargado de obtener los nombres de los amigos del usuario y mostrarlos en el 'listView' personalizado
    private void mostrarAmigos() {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getAmigos")
                .putString("username", usuario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'AmigosWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(AmigosWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se obtienen los nombres de los amigos del usuario y se muestran en el 'listView' personalizado
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

    // Método sobrescrito de la interfaz 'AdaptadorListViewAmigos.ListenerSolicitud' --> Se ejecuta tras eliminar un amigo mediante el botón 'Eliminar' de un item del 'listView' personalizado
    @Override
    public void alEliminar() {
        // Llama al metodo 'mostarAmigos' para actualizar el 'listView' personalizado
        mostrarAmigos();
    }
}