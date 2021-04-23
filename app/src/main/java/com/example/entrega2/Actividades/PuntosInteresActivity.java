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
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.entrega2.Dialogos.DialogoCrearMarcador;
import com.example.entrega2.Dialogos.DialogoEliminarMarcador;
import com.example.entrega2.Dialogos.DialogoPermisosLocalizacion;
import com.example.entrega2.R;
import com.example.entrega2.ServicioMusicaNotificacion;
import com.example.entrega2.Workers.MarcadoresWorker;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

// Actividad que muestra un mapa de Google Maps con marcadores en los puntos de interés que el usuario a creado
public class PuntosInteresActivity  extends FragmentActivity implements OnMapReadyCallback, DialogoEliminarMarcador.ListenerdelDialogo, DialogoCrearMarcador.ListenerdelDialogo {

    //private Context contexto;

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Variables para la gestión del mapa y los marcadores
    private boolean estadoGooglePlay;
    private GoogleMap googleMap;            // Mapa Google Maps
    private ArrayList<String> marcadores;
    private HashMap<String, Marker> markers;

    // ListView con la los nombres y las coordenadas de los marcadores del usuario
    private ListView listViewMarcadores;

    // Spinner para la selección del ListView personalizado que se quiere mostrar
    private Spinner spinner;
    private ArrayAdapter<String> adaptadorSpinner;

    // Variables para guardar los datos de los monumetos recibidos del widget
    private String monumento;
    private double latitud;
    private double longitud;

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

        setContentView(R.layout.activity_puntos_interes);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");

            // Si la actividad se ha abierto al pulsar en la acción 'Añade marcadores' de la notificación, se para el servicio asociado y se cancela su notificación
            String desdeServicio = extras.getString("servicio");
            if(desdeServicio != null && desdeServicio.equals("true")) {
                Intent i = new Intent(this, ServicioMusicaNotificacion.class);
                stopService(i);
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(extras.getInt("notification_id"));
            }

            // Si se ha abierto la actividad al pulsar en el botón 'Marcar' del widget, se recogen los datos del monumento
            monumento = extras.getString("monumento");
            latitud = extras.getDouble("latitud");
            longitud = extras.getDouble("longitud");
        }

        // Se comprueba el estado de Google Play Services
        estadoGooglePlay = comprobarPlayServices();
        if(estadoGooglePlay) {
            // Se carga el Fragment que contiene el mapa de Google Maps
            SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
            elfragmento.getMapAsync(this);
        }

        listViewMarcadores = findViewById(R.id.listViewMarcadores);
        spinner = findViewById(R.id.spinnerVista);

        marcadores = new ArrayList<>();
        markers = new HashMap<>();

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

                // Si se ha abierto la actividad al pulsar en el botón 'Marcar' del widget, se han tenido que recibir los datos 'latitud' y 'longitud'
                // Se establece la cámara del objeto 'googleMap' en la posición especificada y se añade un marcador (Marker)
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

                    // Se añade el marcador a la base de datos
                    añadirMarcadorBD(latActual, longActual, monumento);

                    // Se añade el marcador a la ListView
                    añadirMarcadoresLV();
                }

                // Si la actividad se ha abierto sin usar el widget, se establece la cámara del objeto 'googleMap' en la ubicación actual
                else {
                    establecerUbicacionActual();
                }

                // Se cargan los marcadores previamente guardados por el usuario en el objeto 'googleMap'
                getMarcadoresGuardados();

                // Listener 'onMapClick' al pulsar en un punto de la vista del objeto 'googleMap'
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    // Se ejecuta al pulsar en unas coordenadas latitud, longitud
                    @Override
                    public void onMapClick(LatLng latLng) {
                        // Crea un diálogo que pedirá un nombre para el marcador a crear en esa posición
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

    // Método sobrescrito de la interfaz 'DialogoCrearMarcador.ListenerdelDialogo' --> Se ejecuta tras escribir un nombre para el marcador y aceptar el diálogo
    @Override
    public void crearMarcador(LatLng latLng, String texto) {
        // Se crea un nuevo marcador en el objeto 'googleMap' en la posición especificada
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .title(texto));

        double latActual = latLng.latitude;
        double longActual = latLng.longitude;
        marcadores.add(texto + ": " + latActual + ", " + longActual);
        markers.put(texto + ": " + latActual + ", " + longActual, marker);

        // Se añade el marcador a la base de datos
        añadirMarcadorBD(latActual, longActual, texto);

        // Se añade el marcador a la ListView
        añadirMarcadoresLV();

        // Se actualiza la cámara del objeto 'googleMap' a la posición del marcador creado
        float zoomActual = googleMap.getCameraPosition().zoom;
        CameraPosition Poscam = new CameraPosition.Builder()
                .target(new LatLng(latActual, longActual))
                .zoom(zoomActual)
                .build();
        CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
        googleMap.animateCamera(actualizar);
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

                        detenerActualizador();      // Cuando se detecta una posición válida se deja de actualizar
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

    // Método encargado de obtener los marcadores guardados por el usuarios y añadirlos al objeto 'googleMap'
    private void getMarcadoresGuardados() {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getMarcadores")
                .putString("username", usuario)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'MarcadoresWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(MarcadoresWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se crea un nuevo marcador Marker en la posición obtenida y se añade al ListView
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

                                if(!marcadores.contains(texto + ": " + latitud + ", " + longitud)){
                                    marcadores.add(texto + ": " + latitud + ", " + longitud);
                                }
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

    // Método encargado de añadir la informacion de un marcador a la base de datos
    private void añadirMarcadorBD(double latActual, double longActual, String texto) {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "insertar")
                .putString("username", usuario)
                .putDouble("lat", latActual)
                .putDouble("long", longActual)
                .putString("text", texto)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'MarcadoresWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(MarcadoresWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se informa al usuario que el marcador se ha guardado correctamente en la base de datos
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.MarcadorGuardado), Toast.LENGTH_SHORT).show();
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Método encargado de añadir un nuevo marcador al ListView
    private void añadirMarcadoresLV() {
        // Se actualiza el adaptador del ListView con la lista de marcadores actualizada
        ArrayAdapter adaptador =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, marcadores){
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

        // Listener 'onClick' para cada elemento del ListView
        listViewMarcadores.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            // Se ejecuta al pulsar en un elemento del ListView
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                // Se actualiza la cámara del objeto 'googleMap' a la posición del marcador especificado en el elemento del ListView seleccionado
                String marcador = marcadores.get(position);
                double latActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[0]);
                double longActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[1]);
                CameraPosition Poscam = new CameraPosition.Builder()
                        .target(new LatLng(latActual, longActual))
                        .zoom(13)
                        .build();
                CameraUpdate actualizar = CameraUpdateFactory.newCameraPosition(Poscam);
                googleMap.animateCamera(actualizar);
            }
        });

        // Listener 'onLongClick' para cada elemento del ListView
        listViewMarcadores.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            // Se ejecuta al pulsar prolongadamente en un elemento del ListView
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                String marcador = marcadores.get(position);
                String texto = marcador.split(": ")[0];
                double latActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[0]);
                double longActual = Double.parseDouble(marcador.split(": ")[1].split(", ")[1]);

                // Se crea un diálogo que preguntará al usuario si quiere eliminar el marcador del elemento del ListView seleccionado
                DialogFragment dialogoEliminarMarcador = new DialogoEliminarMarcador(latActual, longActual, texto);
                dialogoEliminarMarcador.show(getSupportFragmentManager(), "eliminar_marcador");

                return true;
            }
        });
    }

    // Método sobrescrito de la interfaz 'DialogoEliminarMarcador.ListenerdelDialogo' --> Se ejecuta tras aceptar el díalogo para eliminar el marcador seleccionado
    @Override
    public void borrarMarcador(double latActual, double longActual, String texto) {
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "eliminar")
                .putString("username", usuario)
                .putDouble("lat", latActual)
                .putDouble("long", longActual)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'MarcadoresWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(MarcadoresWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', el marcador se ha borrado de la base de datos y se borra del objeto 'googleMap' y del ListView
                    if (status != null && status.getState().isFinished()) {
                        // Se elimina el marcador del ListView y se actualiza
                        marcadores.remove(texto + ": " + latActual + ", " + longActual);
                        ArrayAdapter adaptador =
                                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, marcadores){
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

                        // Se elimina el marcador del objeto 'googleMap'
                        markers.get(texto + ": " + latActual + ", " + longActual).remove();

                        Toast.makeText(this, getString(R.string.MarcadorEliminado), Toast.LENGTH_SHORT).show();
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
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
                    intent.putExtra("usuario", usuario);
                    intent.putExtra("monumento", monumento);
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