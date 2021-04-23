package com.example.entrega2.Actividades;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.entrega2.Dialogos.DialogoPermisosLocalizacion;
import com.example.entrega2.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

// Actividad que muestra un mapa de Google Maps con un marcador en la posición guardada para la foto y permite editar su localización
public class UbicacionActivity extends FragmentActivity implements OnMapReadyCallback {

    // Variables para la gestión del mapa y los marcadores
    private boolean estadoGooglePlay;
    private GoogleMap googleMap;
    private Marker marker;

    // Spinner para la selección del ListView personalizado que se quiere mostrar
    private Spinner spinner;
    private ArrayAdapter<String> adaptadorSpinner;

    // Variables para guardar los datos del marcador
    private String titulo;
    private String latitud;
    private String longitud;

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

        setContentView(R.layout.activity_ubicacion);

        // Inicialización de los datos del marcador obtenidos a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            titulo = extras.getString("titulo");
            latitud = extras.getString("latitud");
            longitud = extras.getString("longitud");
        }

        // Se comprueba el estado de Google Play Services
        estadoGooglePlay = comprobarPlayServices();
        if(estadoGooglePlay) {
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapaUbicacion);
            elfragmento.getMapAsync(this);
        }

        spinner = findViewById(R.id.spinnerVistaUbicacion);

    }

    // Método encargado de comprobar el estado de Google Play Services del dispositivo
    private boolean comprobarPlayServices(){
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            return true;
        }
        else {
            if (api.isUserResolvableError(code)){
                api.getErrorDialog(this, code, 58).show();
            }
            return false;
        }
    }

    // Método que devuelve el objeto 'googleMap' con el que trabajar para gestionar el mapa
    @Override
    public void onMapReady(GoogleMap map) {
        // Inicialización del adaptador del spinner con los 4 tipos de vista del mapa que admite el objeto 'googleMap'
        String[] opciones = {getString(R.string.Mapa), getString(R.string.Satelite), getString(R.string.Hibrido), getString(R.string.Terreno)};
        adaptadorSpinner = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_selected_layout, opciones);
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptadorSpinner);

        // Listener al seleccionar un elemento del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Se ejecuta al seleccionar un elemento del Spinner
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == 0) {         // Configura la vista del mapa 'Normal'
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                else if(position == 1){     // Configura la vista del mapa 'Satétite'
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
                else if(position == 2){     // Configura la vista del mapa 'Híbrido'
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                else if(position == 3){     // Configura la vista del mapa 'Terreno'
                    map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }

                googleMap = map;

                // Si la imagen no tiene una ubicación asignada, se actualiza la cámara del objeto 'googleMap' con la geolocalización actual
                if(latitud.isEmpty() && longitud.isEmpty()){
                    establecerUbicacionActual();
                }

                // Si la imagen tiene una ubicación asignada, la cámara del objeto 'googleMap' se establece en esa posición y se crea un marcador
                else {
                    CameraPosition Poscam = new CameraPosition.Builder()
                            .target(new LatLng(Double.parseDouble(latitud), Double.parseDouble(longitud)))
                            .zoom(13)
                            .build();
                    CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                    googleMap.animateCamera(actualizar);

                    marker = googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(Double.parseDouble(latitud), Double.parseDouble(longitud)))
                            .title(titulo));
                }

                // Listener 'onMapClick' al pulsar en una posición del mapa
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    // Se ejecuta al pulsar en el mapa
                    @Override
                    public void onMapClick(LatLng latLng) {
                        // Se elimina el marcador anterior
                        if(marker != null) {
                            marker.remove();
                        }

                        // Se crea un nuevo marcador en la posición que se ha pulsado
                        latitud = String.valueOf(latLng.latitude);
                        longitud = String.valueOf(latLng.longitude);
                        marker = googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latLng.latitude, latLng.longitude))
                                .title(titulo));

                        // Se actualiza la posición de la cámara del objeto 'googleMop'
                        float zoomActual = googleMap.getCameraPosition().zoom;
                        CameraPosition Poscam = new CameraPosition.Builder()
                                .target(new LatLng(latLng.latitude, latLng.longitude))
                                .zoom(zoomActual)
                                .build();
                        CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                        googleMap.animateCamera(actualizar);
                    }
                });
            }

            // Se ejecuta cuando no hay ningún elemento del Spinner seleccionado --> No se hace nada
            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });

    }

    // Obtener geolocalización actual y actualizar la cámara del objeto 'googleMap'
    private FusedLocationProviderClient proveedordelocalizacion;
    private LocationCallback actualizador;

    private void establecerUbicacionActual() {
        // Se comprueba si hay permisos para obtener la geolocalización precisa (ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si el permiso no se ha concedido, se pide
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Se muestra en un diálogo por qué es necesario el permiso
                DialogFragment dialogoPermisosLocalizacion = new DialogoPermisosLocalizacion();
                dialogoPermisosLocalizacion.show(getSupportFragmentManager(), "permisos_localizacion");
            } else {
                // El permiso no esta concedido o el usuario a indicado que no queire que se le vuelva a preguntar
                Toast.makeText(this, getString(R.string.NoPermisoLocalizacion), Toast.LENGTH_SHORT).show();
            }
            // Se pide el permiso al usuario
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            // El permiso esta concedido
            // Para detectar cambios de geolocalización se crea un objeto LocationRequest
            Toast.makeText(this, getString(R.string.ObteniendoGeolocalizacion), Toast.LENGTH_SHORT).show();
            LocationRequest peticion = LocationRequest.create();
            peticion.setInterval(1000);
            peticion.setFastestInterval(5000);
            peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // En el actualizador se indica qué hacer al actualizarse la geolocalización --> Mover la cámara del objeto 'googleMap' a la posición obtenida
            actualizador = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (locationResult != null) {
                        double latitud = locationResult.getLastLocation().getLatitude();
                        double longitud = locationResult.getLastLocation().getLongitude();
                        CameraPosition Poscam = new CameraPosition.Builder()
                                .target(new LatLng(latitud, longitud))
                                .zoom(13)
                                .build();
                        CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                        googleMap.animateCamera(actualizar);

                        detenerActualizador();          // Cuando se detecta una posición válida se deja de actualizar
                    }
                }
            };

            // *** Se ha utilizado un detector de cambios de posición en lugar de obtener la geolocalización una sola vez,
            //     porque el método getLastLocation() devuelve coordenadas 'null' en muchas de las ocasiones ***
            proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);
        }
    }

    // Método encargado de detener la captura de cambios de posiciones
    private void detenerActualizador() {
        proveedordelocalizacion.removeLocationUpdates(actualizador);
    }

    // Listener 'onClick' del botón 'Guardar' del layout 'activity_ubicacion.xml'
    public void onClickGuardar(View v) {
        // Se destruye la actividad devolviendo la latitud y la longitud nueva especificada por el usuario
        Intent intent = new Intent();
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        setResult(RESULT_OK, intent);
        finish();
    }

    // Listener 'onClick' del botón 'Guardar' del layout 'activity_ubicacion.xml'
    public void onClickCancelar(View v) {
        // Se destruye la actividad sin devolver ningún dato
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    // Método sobrescrito que se ejecuta al permitir o denegar los permisos de geolocalización
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Si el permiso se ha concedido se destruye la actividad y se vuelve a crear
                    Intent intent = new Intent(this, PuntosInteresActivity.class);
                    intent.putExtra("titulo", titulo);
                    intent.putExtra("latitud", latitud);
                    intent.putExtra("longitud", longitud);
                    startActivity(intent);
                    finish();
                } else {
                    // Si el permiso se ha denegado se destruye la actividad
                    finish();
                }
                return;
            }
        }
    }
}