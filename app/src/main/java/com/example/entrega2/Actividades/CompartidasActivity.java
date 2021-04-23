package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import android.widget.Toast;

import com.example.entrega2.Adaptadores.AdaptadorRecyclerCompartidas;
import com.example.entrega2.Dialogos.DialogoEliminarCompartida;
import com.example.entrega2.Dialogos.DialogoEnviarComentario;
import com.example.entrega2.R;
import com.example.entrega2.Workers.CompartidasWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

// Actividad que muestra las fotos que se han compartido con el usuario mediante un RecyclerView
public class CompartidasActivity extends AppCompatActivity implements DialogoEliminarCompartida.ListenerdelDialogo, DialogoEnviarComentario.ListenerdelDialogo {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Para el RecyclerView
    private RecyclerView recyclerView;
    private AdaptadorRecyclerCompartidas adaptador;         // Adaptador del RecyclerView
    private GridLayoutManager gridLayout;                   // Layout para el RecyclerView

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

        setContentView(R.layout.activity_compartidas);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
        }

        // Inicializacion RecyclerView
        recyclerView = findViewById(R.id.recyclerViewCompartidas);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT){
            // Si la orientación del dispositivo es vertical (retrato), los elementos se muestran en forma de tabla de 2 columnas
            gridLayout = new GridLayoutManager(this,2,GridLayoutManager.VERTICAL,false);
        }
        else{
            // Si la orientación del dispositivo es horizontal (apaisado), los elementos se muestran en forma de tabla de 4 columnas
            gridLayout = new GridLayoutManager(this,4,GridLayoutManager.VERTICAL,false);
        }
        recyclerView.setLayoutManager(gridLayout);

        cargarFotosCompartidas();
    }

    // Metodo encargado de obtener las fotos que se han compartido con el usuario
    private void cargarFotosCompartidas(){
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getCompartidas")
                .putString("username", usuario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'CompartidasWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(CompartidasWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se obtienen los identificadores de las fotos compartidas con los usuarios, sus títulos
                    // y los nombres de los usuarios propietarios de las fotos, y se muestran en el 'recyclerView'
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);

                            if(jsonArray.length() == 0) {
                                Toast.makeText(this, getString(R.string.NoFotosCompartidas), Toast.LENGTH_SHORT).show();
                                recyclerView.setAdapter(null);
                            }
                            else{
                                String[] usuarios = new String[jsonArray.length()];
                                String[] ids = new String[jsonArray.length()];
                                String[] titulos = new String[jsonArray.length()];

                                for(int i=0; i<jsonArray.length(); i++) {
                                    JSONObject foto = jsonArray.getJSONObject(i);
                                    usuarios[i] = foto.getString("usuario");
                                    ids[i] = foto.getString("imagen");
                                    titulos[i] = foto.getString("titulo");
                                }
                                adaptador = new AdaptadorRecyclerCompartidas(this, usuario, ids, usuarios, titulos);
                                recyclerView.setAdapter(adaptador);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Método sobrescrito de la interfaz 'DialogoEliminarCompartida.ListenerdelDialogo' --> Se ejecuta tras aceptar el diálogo de borrar una foto compartida
    @Override
    public void compartidaEliminada() {
        // Llama al metodo 'cargarFotosCompartidas' para actualizar el 'recyclerView'
        Toast.makeText(this, getString(R.string.CompartidaEliminada), Toast.LENGTH_SHORT).show();
        cargarFotosCompartidas();
    }

    // Método sobrescrito de la interfaz 'DialogoEnviarComentario.ListenerdelDialogo' --> Se ejecuta escribir un mensaje y aceptar el diálogo para enviar un comentario al propietario de una de las fotos compartidas
    @Override
    public void enviarComentario(String amigo, String titulo, String comentario) {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "enviarComentario")
                .putString("from", usuario)
                .putString("to", amigo)
                .putString("titulo", titulo)
                .putString("comentario", comentario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'CompartidasWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(CompartidasWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se le informa al usuario de que se ha enviado el comentario correctamente
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.ComentarioEnviado), Toast.LENGTH_SHORT).show();
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }

}