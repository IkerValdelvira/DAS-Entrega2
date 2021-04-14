package com.example.entrega2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.entrega2.Adaptadores.AdaptadorListViewAnadir;
import com.example.entrega2.Adaptadores.AdaptadorListViewSolicitud;
import com.example.entrega2.PasswordAuthentication;
import com.example.entrega2.R;
import com.example.entrega2.Workers.BuscarSolicitudesWorker;
import com.example.entrega2.Workers.BuscarUsuariosPorDefectoWorker;
import com.example.entrega2.Workers.BuscarUsuariosWorker;
import com.example.entrega2.Workers.ValidarUsuarioWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Locale;

public class AnadirAmigoActivity extends AppCompatActivity implements AdaptadorListViewSolicitud.ListenerSolicitud{

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Spinner para la selección del ListView personalizado que se quiere mostrar
    private Spinner spinner;
    private ArrayAdapter<String> adaptadorSpinner;

    // ListView personalizado para mostrar las películas favoritas de una lista del usuario
    private ListView listView;
    private String[] usernames;

    private EditText buscador;
    private Button buscar;

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

        setContentView(R.layout.activity_anadir_amigo);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
            if(extras.getString("toUser") != null){
                usuario = extras.getString("toUser");
            }
        }

        // Inicialización de los elementos 'spinner' y 'listView' del layout 'activity_favoritos.xml'
        spinner = findViewById(R.id.spinnerAnadir);
        listView = findViewById(R.id.listViewAnadir);

        buscador = findViewById(R.id.editTextBuscar);
        buscar = findViewById(R.id.buttonBuscar);

        // Inicialización el adaptador del spinner con los nombres de las listas de favoritos recibidos de la base de datos
        String[] opciones = {getString(R.string.BuscarAmigos), getString(R.string.SolicitudesPendientes)};
        adaptadorSpinner = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_selected_layout, opciones);
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptadorSpinner);

        // Listener al seleccionar un elemento del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Se ejecuta al seleccionar un elemento del Spinner
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == 0) {
                    // BUSCAR AMIGOS
                    buscador.setVisibility(View.VISIBLE);
                    buscar.setVisibility(View.VISIBLE);

                    busquedaPorDefecto();
                }
                else {
                    // SOLICITUDES PENDIENTES
                    buscador.setVisibility(View.INVISIBLE);
                    buscar.setVisibility(View.INVISIBLE);

                    mostrarSolicitudes();
                }
            }

            // Se ejecuta cuando no hay ningún elemento del Spinner seleccionado --> No se hace nada
            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });
    }

    public void onClickBuscar(View v) {
        if(buscador.getText().toString().isEmpty()){
            Toast.makeText(this, getString(R.string.EscribeBuscador), Toast.LENGTH_SHORT).show();
        }
        else{
            Data datos = new Data.Builder()
                    .putString("username", usuario)
                    .putString("search", buscador.getText().toString())
                    .build();
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(BuscarUsuariosWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                    .observe(this, status -> {
                        if (status != null && status.getState().isFinished()) {
                            String result = status.getOutputData().getString("datos");
                            try {
                                JSONObject jsonObject = new JSONObject(result);

                                JSONArray buscados = jsonObject.getJSONArray("buscados");
                                ArrayList<String> buscadosList = new ArrayList<String>();
                                for (int i=0; i<buscados.length(); i++) {
                                    buscadosList.add(buscados.getString(i));
                                }

                                JSONArray amigos = jsonObject.getJSONArray("amigos");
                                ArrayList<String> amigosList = new ArrayList<String>();
                                for (int i=0; i<amigos.length(); i++) {
                                    amigosList.add(amigos.getString(i));
                                }

                                ArrayList<String> mostrar = new ArrayList<>();
                                for (int i=0; i<buscadosList.size(); i++) {
                                    if(!amigosList.contains(buscadosList.get(i))) {
                                        mostrar.add(buscadosList.get(i));
                                    }
                                }

                                usernames = new String[mostrar.size()];
                                for(int i = 0; i < mostrar.size(); i++) {
                                    usernames[i] = mostrar.get(i);
                                }
                                AdaptadorListViewAnadir adaptadorListView = new AdaptadorListViewAnadir(usuario, AnadirAmigoActivity.this, usernames);
                                listView.setAdapter(adaptadorListView);

                                if(usernames.length == 0) {
                                    Toast.makeText(this, getString(R.string.NoCoincidencias), Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

            WorkManager.getInstance(this).enqueue(otwr);
        }
    }

    private void mostrarSolicitudes() {
        Data datos = new Data.Builder()
                .putString("toUser", usuario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(BuscarSolicitudesWorker.class)
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
                            AdaptadorListViewSolicitud adaptadorListView = new AdaptadorListViewSolicitud(usuario, AnadirAmigoActivity.this, usernames);
                            listView.setAdapter(adaptadorListView);

                            if(usernames.length == 0) {
                                Toast.makeText(this, getString(R.string.NoSolicitudes), Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }


    @Override
    public void alCambiar() {
        mostrarSolicitudes();
    }

    private void busquedaPorDefecto() {
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(BuscarUsuariosPorDefectoWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONObject jsonObject = new JSONObject(result);

                            JSONArray buscados = jsonObject.getJSONArray("buscados");
                            ArrayList<String> buscadosList = new ArrayList<String>();
                            for (int i=0; i<buscados.length(); i++) {
                                buscadosList.add(buscados.getString(i));
                            }

                            JSONArray amigos = jsonObject.getJSONArray("amigos");
                            ArrayList<String> amigosList = new ArrayList<String>();
                            for (int i=0; i<amigos.length(); i++) {
                                amigosList.add(amigos.getString(i));
                            }

                            ArrayList<String> mostrar = new ArrayList<>();
                            for (int i=0; i<buscadosList.size(); i++) {
                                if(!amigosList.contains(buscadosList.get(i))) {
                                    mostrar.add(buscadosList.get(i));
                                }
                            }

                            usernames = new String[mostrar.size()];
                            for(int i = 0; i < mostrar.size(); i++) {
                                usernames[i] = mostrar.get(i);
                            }
                            AdaptadorListViewAnadir adaptadorListView = new AdaptadorListViewAnadir(usuario, AnadirAmigoActivity.this, usernames);
                            listView.setAdapter(adaptadorListView);

                            if(usernames.length == 0) {
                                Toast.makeText(this, getString(R.string.NoCoincidencias), Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }
}