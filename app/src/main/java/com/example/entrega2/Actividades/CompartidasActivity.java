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
import com.example.entrega2.Adaptadores.AdaptadorRecyclerMisFotos;
import com.example.entrega2.Dialogos.DialogoEliminarCompartida;
import com.example.entrega2.Dialogos.DialogoEnviarComentario;
import com.example.entrega2.R;
import com.example.entrega2.Workers.EnviarComentarioWorker;
import com.example.entrega2.Workers.EnviarSolicitudWorker;
import com.example.entrega2.Workers.GetFotosCompartidasUsuarioWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class CompartidasActivity extends AppCompatActivity implements DialogoEliminarCompartida.ListenerdelDialogo, DialogoEnviarComentario.ListenerdelDialogo {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Para el RecyclerView
    private RecyclerView recyclerView;
    private AdaptadorRecyclerCompartidas adaptador;            // Adaptador del RecyclerView
    private GridLayoutManager gridLayout;           // Layout para el RecyclerView

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
        gridLayout = new GridLayoutManager(this,2,GridLayoutManager.VERTICAL,false); // Los elementos se muestran de forma de tabla de 2 columnas
        recyclerView.setLayoutManager(gridLayout);

        cargarFotosCompartidas();
    }

    private void cargarFotosCompartidas(){
        // Obtener las fotos
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GetFotosCompartidasUsuarioWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);

                            if(jsonArray.length() == 0) {
                                Toast.makeText(this, getString(R.string.NoFotosCompartidas), Toast.LENGTH_SHORT).show();
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
                                adaptador = new AdaptadorRecyclerCompartidas(this,usuario,ids,usuarios,titulos);
                                recyclerView.setAdapter(adaptador);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    @Override
    public void compartidaEliminada() {
        Toast.makeText(this, getString(R.string.CompartidaEliminada), Toast.LENGTH_SHORT).show();
        cargarFotosCompartidas();
    }

    @Override
    public void enviarComentario(String amigo, String titulo, String comentario) {
        // ENVIAR SOLICITUD
        Data datos = new Data.Builder()
                .putString("from", usuario)
                .putString("to", amigo)
                .putString("titulo", titulo)
                .putString("comentario", comentario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(EnviarComentarioWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.ComentarioEnviado), Toast.LENGTH_SHORT).show();
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }

}