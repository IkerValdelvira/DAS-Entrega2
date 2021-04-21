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

public class UbicacionActivity extends FragmentActivity implements OnMapReadyCallback {

    private Context contexto;

    private boolean estadoGooglePlay;
    private GoogleMap googleMap;
    private Marker marker;
    private ArrayList<String> marcadores;
    private HashMap<String, Marker> markers;

    // Spinner para la selección del ListView personalizado que se quiere mostrar
    private Spinner spinner;
    private ArrayAdapter<String> adaptadorSpinner;

    private boolean primeraVez;

    private String titulo;
    private String latitud;
    private String longitud;

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

        contexto = this;

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            titulo = extras.getString("titulo");
            latitud = extras.getString("latitud");
            longitud = extras.getString("longitud");
        }

        // COMPROBAR ESTADO DE GOOGLE PLAY
        estadoGooglePlay = comprobarPlayServices();

        if(estadoGooglePlay) {
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapaUbicacion);
            elfragmento.getMapAsync(this);
        }

        spinner = findViewById(R.id.spinnerVistaUbicacion);

        marcadores = new ArrayList<>();
        markers = new HashMap<>();

        primeraVez = true;
    }

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

    @Override
    public void onMapReady(GoogleMap map) {

        // Inicialización el adaptador del spinner con los nombres de las listas de favoritos recibidos de la base de datos
        String[] opciones = {getString(R.string.Mapa), getString(R.string.Satelite), getString(R.string.Hibrido), getString(R.string.Terreno)};
        adaptadorSpinner = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_selected_layout, opciones);
        adaptadorSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptadorSpinner);

        // Listener al seleccionar un elemento del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Se ejecuta al seleccionar un elemento del Spinner
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == 0) {     // Normal
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                else if(position == 1){
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
                else if(position == 2){
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                else if(position == 3){
                    map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }

                googleMap = map;

                if(latitud.isEmpty() && longitud.isEmpty()){
                    establecerUbicacionActual();
                }
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

                // Listener cuando se pulsa --> Crea un nuevo marcador
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        // Elimina el marcador anterior
                        if(marker != null) {
                            marker.remove();
                        }

                        // Crea un nuevo marcador
                        latitud = String.valueOf(latLng.latitude);
                        longitud = String.valueOf(latLng.longitude);
                        marker = googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latLng.latitude, latLng.longitude))
                                .title(titulo));

                        // Actualizar camara
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

    // Obtener ubicacion actual y actualizar camara google maps
    private FusedLocationProviderClient proveedordelocalizacion;
    private LocationCallback actualizador;

    private void establecerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO
                DialogFragment dialogoPermisosLocalizacion = new DialogoPermisosLocalizacion();
                dialogoPermisosLocalizacion.show(getSupportFragmentManager(), "permisos_localizacion");
            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
                Toast.makeText(this, getString(R.string.NoPermisoLocalizacion), Toast.LENGTH_SHORT).show();
            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
            Toast.makeText(this, getString(R.string.ObteniendoGeolocalizacion), Toast.LENGTH_SHORT).show();
            LocationRequest peticion = LocationRequest.create();
            peticion.setInterval(1000);
            peticion.setFastestInterval(5000);
            peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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

                        detenerActualizador();
                    }
                }
            };

            proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);
        }
    }

    private void detenerActualizador() {
        proveedordelocalizacion.removeLocationUpdates(actualizador);
    }


    public void onClickGuardar(View v) {
        Intent intent = new Intent();
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onClickCancelar(View v) {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
                    Intent intent = new Intent(this, PuntosInteresActivity.class);
                    intent.putExtra("titulo", titulo);
                    intent.putExtra("latitud", latitud);
                    intent.putExtra("longitud", longitud);
                    startActivity(intent);
                    finish();
                } else {
                    // PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                    finish();
                }
                return;
            }
        }
    }
}