package com.example.entrega2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.entrega2.Preferencias;
import com.example.entrega2.R;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Preferencias.ListenerPreferencias {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // View para la gestión de las preferencias
    private View preferencias;
    private int prefsVisibles;

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
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_dialog_info);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final DrawerLayout elmenudesplegable = findViewById(R.id.drawer_layout);
        NavigationView elnavigation = findViewById(R.id.elnavigationview);
        elnavigation.setNavigationItemSelectedListener(new
           NavigationView.OnNavigationItemSelectedListener() {
               @Override
               public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                   if(item.getItemId() == R.id.galeria){
                       Intent intent = new Intent(MainActivity.this, SubirFotoActivity.class);
                       intent.putExtra("usuario", usuario);
                       intent.putExtra("origen", "galeria");
                       startActivity(intent);
                   }
                   else if(item.getItemId() == R.id.camara){
                       Intent intent = new Intent(MainActivity.this, SubirFotoActivity.class);
                       intent.putExtra("usuario", usuario);
                       intent.putExtra("origen", "camara");
                       startActivity(intent);
                   }
                   else if(item.getItemId() == R.id.albumes) {
                       System.out.println("Opción navigation drawer ALBUMES");
                   }
                   else if(item.getItemId() == R.id.puntos) {
                       Intent intent = new Intent(MainActivity.this, PuntosInteresActivity.class);
                       intent.putExtra("usuario", usuario);
                       startActivity(intent);
                   }
                   elmenudesplegable.closeDrawers();
                   return false;
               }
           });

        // Inicializacíon del fragment que contiene la selección de las preferencias
        preferencias = findViewById(R.id.fragmentPreferencias);
        prefsVisibles = 0;
        if (savedInstanceState != null) {
            // Se recupera el estado del fragment de las preferencias (visible o invisible) tras la destrucción de la actividad
            prefsVisibles = savedInstanceState.getInt("prefsVisibles");
        }

        if(prefsVisibles == 0 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            // Si la orientación del dispositivo es de retrato (vertical) y el fragment de las preferencias era invisible, se mantiene invisible
            preferencias.setVisibility(View.INVISIBLE);
        }
        else if(prefsVisibles == 1 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            // Si la orientación del dispositivo es de retrato (vertical) y el fragment de las preferencias era visible, se mantiene visible
            preferencias.setVisibility(View.VISIBLE);
        }
        else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            // Si la orientación del dispositivo es apaisada (horizonta), el fragment de las preferencias va a estar siempre visible al crear la actividad
            preferencias.setVisibility(View.VISIBLE);
            prefsVisibles = 1;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id == R.id.anadir) {
            Intent intent = new Intent(this, AnadirAmigoActivity.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        }
        else if(id == R.id.amigos) {
            Intent intent = new Intent(this, AmigosActivity.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        }
        else if(id == R.id.preferencias) {
            // Si se ha pulsado en la 'opcionPreferencias', se muestra/oculta el fragment con las preferencias
            if(prefsVisibles == 0 ){
                preferencias.setVisibility(View.VISIBLE);
                prefsVisibles = 1;
            }
            else {
                preferencias.setVisibility(View.INVISIBLE);
                prefsVisibles = 0;
            }
        }
        else if(id == R.id.cerrarSesion) {
            // Si se ha pulsado en la 'opcionCerrarSesion', se crea una nueva actividad 'LoginActivity' y se destruye la actividad actual
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        else if(id == android.R.id.home) {
            final DrawerLayout elmenudesplegable = findViewById(R.id.drawer_layout);
            elmenudesplegable.openDrawer(GravityCompat.START);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout elmenudesplegable = findViewById(R.id.drawer_layout);
        if (elmenudesplegable.isDrawerOpen(GravityCompat.START)) {
            elmenudesplegable.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Método sobrescrito de la interfaz 'Preferencias.ListenerPreferencias' --> Se ejecuta al cambiar la preferencia 'idioma'
    @Override
    public void alCambiarIdioma() {
        // Se destruye la actividad y se vuelve a crear --> Al crearse de nuevo se establecerá la nueva localización en el método 'onCreate'
        finish();
        startActivity(getIntent());
    }

    // Se ejecuta siempre antes de destruirse la actividad
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Se guarda en un Bundle la variable que indica si el fragment de las preferencias estaba visible o invisible
        outState.putInt("prefsVisibles", prefsVisibles);
    }
}