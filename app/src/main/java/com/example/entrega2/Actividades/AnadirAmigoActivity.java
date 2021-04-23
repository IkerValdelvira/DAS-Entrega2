package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
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
import com.example.entrega2.R;
import com.example.entrega2.ServicioMusicaNotificacion;
import com.example.entrega2.Workers.SolicitudesWorker;
import com.example.entrega2.Workers.UsuariosWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

// Actividad que muestra un ListView personalizado con los usuarios no añadidos como amigos o las solicitudes de amistad pendientes (se elige mediante un Spinner)
public class AnadirAmigoActivity extends AppCompatActivity implements AdaptadorListViewSolicitud.ListenerSolicitud {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Spinner para la selección del ListView personalizado que se quiere mostrar
    private Spinner spinner;
    private ArrayAdapter<String> adaptadorSpinner;

    // ListView personalizado para mostrar los nombres de los usuarios no añadidos como amigos o las solicitudes de amistad pendientes
    private ListView listView;
    private String[] usernames;

    // Elementos necesarios del layout 'activity_anadir_amigo.xml'
    private EditText buscador;
    private Button buscar;

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

        setContentView(R.layout.activity_anadir_amigo);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        Boolean solicitud = false;
        if (extras != null) {
            usuario = extras.getString("usuario");
            if(extras.getString("toUser") != null){
                usuario = extras.getString("toUser");
            }

            // Si la actividad se ha abierto al pulsar en la acción 'Buscar amigos' de la notificación, se para el servicio asociado y se cancela su notificación
            String desdeServicio = extras.getString("servicio");
            if(desdeServicio != null && desdeServicio.equals("true")) {
                Intent i = new Intent(this, ServicioMusicaNotificacion.class);
                stopService(i);
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(extras.getInt("notification_id"));
            }

            // Si la actividad se ha abierto al pulsar en una notificación informando que se ha recibido una solicitud de amistad
            solicitud = extras.getBoolean("solicitudes");
        }

        // Inicialización de los elementos 'spinner' y 'listView' del layout 'activity_anadir_amigo.xml'
        spinner = findViewById(R.id.spinnerAnadir);
        listView = findViewById(R.id.listViewAnadir);

        buscador = findViewById(R.id.editTextBuscar);
        buscar = findViewById(R.id.buttonBuscar);

        // Inicialización el adaptador del spinner con las opciones para mostrar los usuarios no añadidos como amigos o las solicitudes de amistad pendientes
        String[] opciones = {getString(R.string.BuscarAmigos), getString(R.string.SolicitudesPendientes)};
        adaptadorSpinner = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_selected_layout, opciones);
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptadorSpinner);
        // Si se ha recibido una solicitud de amistad, se muestran por defecto las solicitudes al abrir la actividad
        if(solicitud){
            spinner.setSelection(1);
        }

        // Listener al seleccionar un elemento del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Se ejecuta al seleccionar un elemento del Spinner
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == 0) {
                    // Se habilita el buscador de usuarios por nombre
                    buscador.setVisibility(View.VISIBLE);
                    buscar.setVisibility(View.VISIBLE);

                    busquedaPorDefecto();   // Al crear la actividad se muestran todos los usuario no añadidos como amigos
                }
                else {
                    // Se deshabilita el buscador de usuarios por nombre
                    buscador.setVisibility(View.INVISIBLE);
                    buscar.setVisibility(View.INVISIBLE);

                    mostrarSolicitudes();   // Se muestran las solicitudes de amistad pendientes
                }
            }

            // Se ejecuta cuando no hay ningún elemento del Spinner seleccionado --> No se hace nada
            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });
    }

    // Listener 'onClick' del botón 'Buscar' del layout 'activity_anadir_amigo.xml'
    public void onClickBuscar(View v) {
        if(buscador.getText().toString().isEmpty()){
            // Si el EditText del buscador está vacío, se le informa al usuario mediante un Toast
            Toast.makeText(this, getString(R.string.EscribeBuscador), Toast.LENGTH_SHORT).show();
        }
        else{
            // Información a enviar a la tarea
            Data datos = new Data.Builder()
                    .putString("funcion", "buscar")
                    .putString("username", usuario)
                    .putString("search", buscador.getText().toString())
                    .build();
            // Restricciones a cumplir: es necesaria la conexión a internet
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            // Se ejecuta el trabajo una única vez: 'UsuariosWorker'
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(UsuariosWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(datos)
                    .build();

            // Recuperación de los resultados de la tarea
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                    .observe(this, status -> {
                        // En caso de éxito 'Result.success()', se obtienen los nombres de los usuarios que contienen los caracteres especificados en el EditText del buscador,
                        // solo si estos usuarios no han sido añadidos todavía como amigos, y se muestran en el 'listView' personalizado
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

                                JSONArray solicitados = jsonObject.getJSONArray("solicitados");
                                ArrayList<String> solicitadosList = new ArrayList<String>();
                                for (int i=0; i<solicitados.length(); i++) {
                                    solicitadosList.add(solicitados.getString(i));
                                }

                                usernames = new String[mostrar.size()];
                                for(int i = 0; i < mostrar.size(); i++) {
                                    usernames[i] = mostrar.get(i);
                                }
                                AdaptadorListViewAnadir adaptadorListView = new AdaptadorListViewAnadir(usuario, AnadirAmigoActivity.this, usernames, solicitadosList);
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

    // Método encargado de obtener y mostrar todos los usuarios no añadidos como amigos
    private void busquedaPorDefecto() {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "buscarPorDefecto")
                .putString("username", usuario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'UsuariosWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(UsuariosWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se obtienen los nombres de todos los usuarios que no han sido añadidos
                    // todavía como amigos y se muestran en el 'listView' personalizado
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

                            JSONArray solicitados = jsonObject.getJSONArray("solicitados");
                            ArrayList<String> solicitadosList = new ArrayList<String>();
                            for (int i=0; i<solicitados.length(); i++) {
                                solicitadosList.add(solicitados.getString(i));
                            }

                            usernames = new String[mostrar.size()];
                            for(int i = 0; i < mostrar.size(); i++) {
                                usernames[i] = mostrar.get(i);
                            }
                            AdaptadorListViewAnadir adaptadorListView = new AdaptadorListViewAnadir(usuario, AnadirAmigoActivity.this, usernames, solicitadosList);
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

    // Método encargado de obtener y mostrar las solicitudes de amistad pendientes en el 'listView'
    private void mostrarSolicitudes() {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "buscar")
                .putString("toUser", usuario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'SolicitudesWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(SolicitudesWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se obtienen los nombres de los usuarios que han enviado una solicitud de amistad
                    // y se muestran en el 'listView' personalizado
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

    // Método sobrescrito de la interfaz 'AdaptadorListViewSolicitud.ListenerSolicitud' --> Se ejecuta tras pulsar 'Aceptar' o 'Rechazar' ee un item del 'listView' personalizado con las solicitudes de amistad pendientes
    @Override
    public void alCambiar() {
        // Llama al metodo 'mostrarSolicitudes' para actualizar el 'listView' personalizado
        mostrarSolicitudes();
    }
}