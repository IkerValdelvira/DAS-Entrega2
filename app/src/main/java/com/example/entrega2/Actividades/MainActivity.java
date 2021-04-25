package com.example.entrega2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.entrega2.Adaptadores.AdaptadorRecyclerMisFotos;
import com.example.entrega2.Dialogos.DialogoDescargarFoto;
import com.example.entrega2.Preferencias;
import com.example.entrega2.R;
import com.example.entrega2.ServicioMusicaNotificacion;
import com.example.entrega2.Workers.ImagenesWorker;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

// Actividad principal de la aplicación que muestra las fotos almacenadas del usuario en un RecyclerView.
// También contiene un barra de acción Toolbar y un menú lateral desplegable NavigationDrawer
public class MainActivity extends AppCompatActivity implements DialogoDescargarFoto.ListenerdelDialogo {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Para el RecyclerView
    private RecyclerView recyclerView;
    private AdaptadorRecyclerMisFotos adaptador;            // Adaptador del RecyclerView
    private GridLayoutManager gridLayout;           // Layout para el RecyclerView

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

        setContentView(R.layout.activity_main);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");

            // Si la actividad se ha abierto al pulsar en la notificación, se para el servicio asociado
            String desdeServicio = extras.getString("servicio");
            if(desdeServicio != null && desdeServicio.equals("true")) {
                Intent i = new Intent(this, ServicioMusicaNotificacion.class);
                stopService(i);
            }
        }

        // Se incluye el icono en la Toolbar que indica la existencia de un NavigationDrawer
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_dialog_info);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configuración del NavigationDrawer
        final DrawerLayout elmenudesplegable = findViewById(R.id.drawer_layout);
        NavigationView elnavigation = findViewById(R.id.elnavigationview);
        View headerView = elnavigation.getHeaderView(0);
        TextView textViewBienvenido = headerView.findViewById(R.id.textViewBienvenido);
        textViewBienvenido.setText(getString(R.string.Bienvenido) + " " + usuario + "!!!");
        // Listener para poder reaccionar a la interacción del usuario sobre las opciones del menú
        elnavigation.setNavigationItemSelectedListener(new
           NavigationView.OnNavigationItemSelectedListener() {
               @Override
               public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                   if(item.getItemId() == R.id.galeria){
                       // Si se pulsa la opción 'Desde la galería', se abre una actividad 'SubirFotoActivity' con origen = galeria
                       Intent intent = new Intent(MainActivity.this, SubirFotoActivity.class);
                       intent.putExtra("usuario", usuario);
                       intent.putExtra("origen", "galeria");
                       startActivity(intent);
                   }
                   else if(item.getItemId() == R.id.camara){
                       // Si se pulsa la opción 'Desde la galería', se abre una actividad 'SubirFotoActivity' con origen = camara
                       Intent intent = new Intent(MainActivity.this, SubirFotoActivity.class);
                       intent.putExtra("usuario", usuario);
                       intent.putExtra("origen", "camara");
                       startActivity(intent);
                   }
                   else if(item.getItemId() == R.id.compartidas) {
                       // Si se pulsa la opción 'Ver compartidas conmigo', se abre una actividad 'CompartidasActivity'
                       Intent intent = new Intent(MainActivity.this, CompartidasActivity.class);
                       intent.putExtra("usuario", usuario);
                       startActivity(intent);
                   }
                   else if(item.getItemId() == R.id.puntos) {
                       // Si se pulsa la opción 'Puntos de interes', se abre una actividad 'PuntosInteresActivity'
                       Intent intent = new Intent(MainActivity.this, PuntosInteresActivity.class);
                       intent.putExtra("usuario", usuario);
                       startActivity(intent);
                   }
                   elmenudesplegable.closeDrawers();
                   return false;
               }
           });

        // Inicializacion RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
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

        // Obtener las fotos del usuario de la base de datos
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getFotosUsuario")
                .putString("username", usuario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'ImagenesWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ImagenesWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se obtienen los identificadores de las fotos del usuario y sus títulos, y se muestran en el 'recyclerView'
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);

                            if(jsonArray.length() == 0) {
                                Toast.makeText(this, getString(R.string.NoFotosSubidas), Toast.LENGTH_SHORT).show();
                            }
                            else{
                                String[] ids = new String[jsonArray.length()];
                                String[] titulos = new String[jsonArray.length()];

                                for(int i=0; i<jsonArray.length(); i++) {
                                    JSONObject foto = jsonArray.getJSONObject(i);
                                    ids[i] = foto.getString("id");
                                    titulos[i] = foto.getString("titulo");
                                }
                                adaptador = new AdaptadorRecyclerMisFotos(this,usuario,ids,titulos);
                                recyclerView.setAdapter(adaptador);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);

    }

    // Método sobrescrito para asignar el fichero 'menu.xml' con la definición del menú a la Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    // Método sobrescrito para reaccionar ante la interacción del usuario con cualquiera de las opciones del menú Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id == R.id.anadir) {
            // Si se pulsa la opción 'Buscar gente', se abre una actividad 'AnadirAmigoActivity'
            Intent intent = new Intent(this, AnadirAmigoActivity.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        }
        else if(id == R.id.amigos) {
            // Si se pulsa la opción 'Amigos', se abre una actividad 'AmigosActivity'
            Intent intent = new Intent(this, AmigosActivity.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        }
        else if(id == R.id.preferencias) {
            // Si se pulsa la opción 'Amigos', se abre una actividad 'PreferenciasActivity'
            Intent intent = new Intent(this, PreferenciasActivity.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        }
        else if(id == R.id.cerrarSesion) {
            // Si se pulsa la opción 'Amigos', se abre una nueva actividad 'LoginActivity' y se destruye la actividad actual
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        else if(id == android.R.id.home) {
            // Si se pulsa la opción (icono) 'home', se abre el NavigationDrawer
            final DrawerLayout elmenudesplegable = findViewById(R.id.drawer_layout);
            elmenudesplegable.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Método sobrescrito para reaccionar al pulsar el boton Back, si el NavigationDrawer está abierto, se cierra
    @Override
    public void onBackPressed() {
        final DrawerLayout elmenudesplegable = findViewById(R.id.drawer_layout);
        if (elmenudesplegable.isDrawerOpen(GravityCompat.START)) {
            elmenudesplegable.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Método sobrescrito de la interfaz 'DialogoDescargarFoto.ListenerdelDialogo' --> Se ejecuta al aceptar el diálogo para eliminar una foto
    @Override
    public void descargarFoto(Uri uri) {
        // Se descargar una imagen del almacenamiento Firebase Cloud Storage y se guarda en el directorio Pictures del dispositivo (DownloadManager)
        File path = new File(uri.toString());
        String fileName = path.getName();
        final DownloadManager downloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(fileName);
        request.setDescription(fileName);
        request.setVisibleInDownloadsUi(true);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);
        long ref = downloadManager.enqueue(request);
    }
}