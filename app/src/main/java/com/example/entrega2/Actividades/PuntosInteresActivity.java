package com.example.entrega2.Actividades;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.entrega2.Adaptadores.AdaptadorListViewAnadir;
import com.example.entrega2.Dialogos.DialogoCrearMarcador;
import com.example.entrega2.Dialogos.DialogoEliminarMarcador;
import com.example.entrega2.Dialogos.DialogoPermisosLocalizacion;
import com.example.entrega2.R;
import com.example.entrega2.ServicioMusicaNotificacion;
import com.example.entrega2.Workers.BuscarUsuariosWorker;
import com.example.entrega2.Workers.EliminarMarcadorWorker;
import com.example.entrega2.Workers.GetAmigosUsuarioWorker;
import com.example.entrega2.Workers.GetMarcadoresUsuarioWorker;
import com.example.entrega2.Workers.InsertarMarcadorWorker;
import com.example.entrega2.Workers.InsertarUsuarioWorker;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class PuntosInteresActivity  extends FragmentActivity implements OnMapReadyCallback, DialogoEliminarMarcador.ListenerdelDialogo, DialogoCrearMarcador.ListenerdelDialogo {

    private Context contexto;

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    private boolean estadoGooglePlay;
    private GoogleMap googleMap;
    private ArrayList<String> marcadores;
    private HashMap<String, Marker> markers;

    private ListView listViewMarcadores;

    // Spinner para la selección del ListView personalizado que se quiere mostrar
    private Spinner spinner;
    private ArrayAdapter<String> adaptadorSpinner;

    private boolean primeraVez;

    private String monumento;
    private double latitud;
    private double longitud;

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

        setContentView(R.layout.activity_puntos_interes);

        contexto = this;

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
            String desdeServicio = extras.getString("servicio");
            if(desdeServicio != null && desdeServicio.equals("true")) {
                Intent i = new Intent(this, ServicioMusicaNotificacion.class);
                stopService(i);
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(extras.getInt("notification_id"));
            }
            monumento = extras.getString("monumento");
            latitud = extras.getDouble("latitud");
            longitud = extras.getDouble("longitud");
        }

        // COMPROBAR ESTADO DE GOOGLE PLAY
        estadoGooglePlay = comprobarPlayServices();

        if(estadoGooglePlay) {
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
            elfragmento.getMapAsync(this);
        }

        listViewMarcadores = findViewById(R.id.listViewMarcadores);
        spinner = findViewById(R.id.spinnerVista);

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

                if(latitud != 0 && longitud != 0) {
                    CameraPosition Poscam = new CameraPosition.Builder()
                            .target(new LatLng(latitud, longitud))
                            .zoom(13)
                            .build();
                    CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                    googleMap.animateCamera(actualizar);
                    LatLng latLng = new LatLng(latitud, longitud);
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latLng.latitude, latLng.longitude))
                            .title(monumento));

                    double latActual = latLng.latitude;
                    double longActual = latLng.longitude;
                    marcadores.add(monumento + ": " + latActual + ", " + longActual);
                    markers.put(monumento + ": " + latActual + ", " + longActual, marker);

                    // Añadir marcador a la base de datos
                    añadirMarcadorBD(latActual, longActual, monumento);

                    // Añadir marcador a la listView
                    añadirMarcadoresLV();
                }
                else if(primeraVez){
                    establecerUbicacionActual();
                    primeraVez = false;
                }
                getMarcadoresGuardados();

                // Listener cuando se pulsa --> Crea un nuevo marcador
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        // Pedir título del marcador al usuario
                        DialogFragment dialogoCrearMarcador = new DialogoCrearMarcador(latLng);
                        dialogoCrearMarcador.show(getSupportFragmentManager(), "crear_marcador");
                    }
                });
            }

            // Se ejecuta cuando no hay ningún elemento del Spinner seleccionado --> No se hace nada
            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });

    }

    @Override
    public void crearMarcador(LatLng latLng, String texto) {
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .title(texto));

        double latActual = latLng.latitude;
        double longActual = latLng.longitude;
        marcadores.add(texto + ": " + latActual + ", " + longActual);
        markers.put(texto + ": " + latActual + ", " + longActual, marker);

        // Añadir marcador a la base de datos
        añadirMarcadorBD(latActual, longActual, texto);

        // Añadir marcador a la listView
        añadirMarcadoresLV();

        // Actualizar camara
        float zoomActual = googleMap.getCameraPosition().zoom;
        CameraPosition Poscam = new CameraPosition.Builder()
                .target(new LatLng(latActual, longActual))
                .zoom(zoomActual)
                .build();
        CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
        googleMap.animateCamera(actualizar);
    }

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
            FusedLocationProviderClient proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitud = location.getLatitude();
                                double longitud = location.getLongitude();
                                CameraPosition Poscam = new CameraPosition.Builder()
                                        .target(new LatLng(latitud, longitud))
                                        .zoom(13)
                                        .build();
                                CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                                googleMap.animateCamera(actualizar);
                            } else {
                                Toast.makeText(contexto, getString(R.string.GeolocalizacionDesconocida), Toast.LENGTH_SHORT).show();
                                volverAConectar(location);
                            }
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(contexto, getString(R.string.NoUbicacion), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void getMarcadoresGuardados() {
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GetMarcadoresUsuarioWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);

                            for(int i=0; i<jsonArray.length(); i++) {
                                JSONObject marcador = jsonArray.getJSONObject(i);
                                String texto = marcador.getString("texto");
                                double latitud = Double.parseDouble(marcador.getString("latitud"));
                                double longitud = Double.parseDouble(marcador.getString("longitud"));

                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(latitud, longitud))
                                        .title(texto));
                                markers.put(texto + ": " + latitud + ", " + longitud, marker);

                                marcadores.add(texto + ": " + latitud + ", " + longitud);
                                añadirMarcadoresLV();
                            }

                            if (marcadores.size() == 0) {
                                Toast.makeText(this, getString(R.string.NoMarcador), Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    private void añadirMarcadorBD(double latActual, double longActual, String texto) {
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .putDouble("lat", latActual)
                .putDouble("long", longActual)
                .putString("text", texto)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(InsertarMarcadorWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.MarcadorGuardado), Toast.LENGTH_SHORT).show();
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    private void añadirMarcadoresLV() {
        ArrayAdapter adaptador =
                new ArrayAdapter<String>(contexto, android.R.layout.simple_list_item_2, android.R.id.text1, marcadores){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View vista= super.getView(position, convertView, parent);
                        TextView lineaprincipal=(TextView) vista.findViewById(android.R.id.text1);
                        TextView lineasecundaria=(TextView) vista.findViewById(android.R.id.text2);
                        lineaprincipal.setText(marcadores.get(position).split(": ")[0]);
                        lineasecundaria.setText(marcadores.get(position).split(": ")[1]);
                        return vista;
                    }
                };

        listViewMarcadores.setAdapter(adaptador);

        listViewMarcadores.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                String marcador = marcadores.get(position);
                double latActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[0]);
                double longActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[1]);

                // Actualizar camara
                CameraPosition Poscam = new CameraPosition.Builder()
                        .target(new LatLng(latActual, longActual))
                        .zoom(13)
                        .build();
                CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                googleMap.animateCamera(actualizar);
            }
        });

        listViewMarcadores.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                String marcador = marcadores.get(position);
                String texto = marcador.split(": ")[0];
                double latActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[0]);
                double longActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[1]);

                DialogFragment dialogoEliminarMarcador = new DialogoEliminarMarcador(latActual, longActual, texto);
                dialogoEliminarMarcador.show(getSupportFragmentManager(), "eliminar_marcador");

                return true;
            }
        });
    }

    @Override
    public void borrarMarcador(double latActual, double longActual, String texto) {
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .putDouble("lat", latActual)
                .putDouble("long", longActual)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(EliminarMarcadorWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        // Eliminar marcador a la listView
                        marcadores.remove(texto + ": " + latActual + ", " + longActual);
                        ArrayAdapter adaptador =
                                new ArrayAdapter<String>(contexto, android.R.layout.simple_list_item_2, android.R.id.text1, marcadores){
                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {
                                        View vista= super.getView(position, convertView, parent);
                                        TextView lineaprincipal=(TextView) vista.findViewById(android.R.id.text1);
                                        TextView lineasecundaria=(TextView) vista.findViewById(android.R.id.text2);
                                        lineaprincipal.setText(marcadores.get(position).split(": ")[0]);
                                        lineasecundaria.setText(marcadores.get(position).split(": ")[1]);
                                        return vista;
                                    }
                                };

                        listViewMarcadores.setAdapter(adaptador);

                        // Eliminar marcador del mapa
                        markers.get(texto + ": " + latActual + ", " + longActual).remove();

                        Toast.makeText(this, getString(R.string.MarcadorEliminado), Toast.LENGTH_SHORT).show();
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }


    // Si no se consigue la ubicacion, se busca cada segundo hasta encontrarla
    private FusedLocationProviderClient proveedordelocalizacion;
    private LocationCallback actualizador;

    @SuppressLint("MissingPermission")
    private void volverAConectar(Location location) {
        LocationRequest peticion = LocationRequest.create();
        peticion.setInterval(1000);
        peticion.setFastestInterval(5000);
        peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        actualizador = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (location != null) {
                    double latitud = location.getLatitude();
                    double longitud = location.getLongitude();
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

    private void detenerActualizador() {
        proveedordelocalizacion.removeLocationUpdates(actualizador);
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
                    intent.putExtra("usuario", usuario);
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