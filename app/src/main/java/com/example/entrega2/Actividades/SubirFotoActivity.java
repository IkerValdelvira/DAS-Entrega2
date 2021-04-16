package com.example.entrega2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.entrega2.Dialogos.DialogoPermisosCamara;
import com.example.entrega2.Dialogos.DialogoPermisosLocalizacion;
import com.example.entrega2.R;
import com.example.entrega2.Workers.InsertarFotoWorker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class SubirFotoActivity extends AppCompatActivity {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    private String origenFoto;
    private ImageView imageViewFoto;

    private static int CODIGO_GALERIA = 1;
    private static int CODIGO_FOTO_ARCHIVO = 2;

    private Uri uriimagen;
    private String imageName;
    private File fichImg;

    private EditText editTextTituloSubir;
    private EditText editTextDescripcionSubir;
    private RadioButton radioButtonGeolocalizacion;

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

        setContentView(R.layout.activity_subir_foto);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
            origenFoto = extras.getString("origen");
        }

        imageViewFoto = findViewById(R.id.imageViewFoto);
        imageViewFoto.setRotation(90);

        editTextTituloSubir = findViewById(R.id.editTextTituloSubir);
        editTextDescripcionSubir = findViewById(R.id.editTextDescripcionSubir);
        radioButtonGeolocalizacion = findViewById(R.id.radioButtonGeolocalizacion);

        if(origenFoto.equals("galeria")) {
            Intent elIntentGal = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(elIntentGal, CODIGO_GALERIA);
        }
        else if(origenFoto.equals("camara")) {
            // Es necesario dar permisos CAMERA
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO
                    DialogFragment dialogoPermisosCamara = new DialogoPermisosCamara();
                    dialogoPermisosCamara.show(getSupportFragmentManager(), "permisos_camara");
                } else {
                    //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                    //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
                    Toast.makeText(this, getString(R.string.NoPermisoCamara), Toast.LENGTH_SHORT).show();
                }
                //PEDIR EL PERMISO
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                /*
                Intent intent = new Intent(this, SubirFotoActivity.class);
                intent.putExtra("usuario", usuario);
                intent.putExtra("origen", "camara");
                startActivity(intent);
                finish();
                */
            }
            else{
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String nombrefich = "IMG_" + timeStamp + "_";
                File directorio = this.getFilesDir();
                try {
                    fichImg = File.createTempFile(nombrefich, ".jpg",directorio);
                    uriimagen = FileProvider.getUriForFile(this, "com.example.entrega2.provider", fichImg);
                } catch (IOException e) {

                }
                Intent elIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                elIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriimagen);
                startActivityForResult(elIntent, CODIGO_FOTO_ARCHIVO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODIGO_GALERIA && resultCode == RESULT_OK) {
            uriimagen = data.getData();
            imageName = uriimagen.toString().split("%2F")[uriimagen.toString().split("%2F").length-1];
            setImage(uriimagen);
        }

        else if (requestCode == CODIGO_FOTO_ARCHIVO && resultCode == RESULT_OK) {
            imageName = uriimagen.toString().split("/")[uriimagen.toString().split("/").length-1];
            setImage(uriimagen);
        }

    }

    private void setImage(Uri uriimagen) {
        try{
            Bitmap bitmapFoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriimagen);
            int anchoDestino = imageViewFoto.getWidth();
            int altoDestino = imageViewFoto.getHeight();
            int anchoImagen = bitmapFoto.getWidth();
            int altoImagen = bitmapFoto.getHeight();
            float ratioImagen = (float) anchoImagen / (float) altoImagen;
            float ratioDestino = (float) anchoDestino / (float) altoDestino;
            int anchoFinal = anchoDestino;
            int altoFinal = altoDestino;
            if (ratioDestino > ratioImagen) {
                anchoFinal = (int) ((float)altoDestino * ratioImagen);
            } else {
                altoFinal = (int) ((float)anchoDestino / ratioImagen);
            }
            Bitmap bitmapredimensionado = Bitmap.createScaledBitmap(bitmapFoto,anchoFinal,altoFinal,true);
            imageViewFoto.setImageBitmap(bitmapredimensionado);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickSubir(View v) {
        String titulo = editTextTituloSubir.getText().toString();
        String descripcion = editTextDescripcionSubir.getText().toString();
        if(titulo.isEmpty()) {
            Toast.makeText(this, getString(R.string.EscribeTitulo), Toast.LENGTH_SHORT).show();
        }
        else if(descripcion.length() > 255) {
            Toast.makeText(this, getString(R.string.DescripcionLarga), Toast.LENGTH_SHORT).show();
        }
        else {
            // Fecha
            String fecha = "";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                fecha = dtf.format(now);
            }

            // Opcion geolocalizacion
            String latitud = "";
            String longitud = "";
            if(radioButtonGeolocalizacion.isChecked()) {
                getUbicacionActual(titulo, descripcion, fecha);
            }
            else {
                subirFoto(titulo, descripcion, fecha, latitud, longitud);
            }
        }
    }

    private void getUbicacionActual(String titulo, String descripcion, String fecha) {
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
            getUbicacionActual(titulo, descripcion, fecha);
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

                                subirFoto(titulo, descripcion, fecha, String.valueOf(latitud), String.valueOf(longitud));

                            } else {
                                Toast.makeText(SubirFotoActivity.this, getString(R.string.GeolocalizacionDesconocida), Toast.LENGTH_SHORT).show();
                                volverAConectar(titulo, descripcion, fecha, location);
                            }
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SubirFotoActivity.this, getString(R.string.NoUbicacion), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Si no se consigue la ubicacion, se busca cada segundo hasta encontrarla
    private FusedLocationProviderClient proveedordelocalizacion;
    private LocationCallback actualizador;

    @SuppressLint("MissingPermission")
    private void volverAConectar(String titulo, String descripcion, String fecha, Location location) {
        System.out.println("VOLVER A CONECTAR");
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

                    subirFoto(titulo, descripcion, fecha, String.valueOf(latitud), String.valueOf(longitud));

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

    private void subirFoto(String titulo, String descripcion, String fecha, String latitud, String longitud) {
        // Subir a Firebase
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference spaceRef = storageRef.child(imageName);
        spaceRef.putFile(uriimagen);

        // Subir a la BD
        Data datos = new Data.Builder()
                .putString("usuario", usuario)
                .putString("imagen", imageName)
                .putString("titulo", titulo)
                .putString("descripcion", descripcion)
                .putString("fecha", fecha)
                .putString("latitud", latitud)
                .putString("longitud", longitud)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(InsertarFotoWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        try {
                            Thread.sleep(3000); // Delay para que se suba la foto a Firebase y se pueda cargar correctamente
                            Toast.makeText(this, getString(R.string.FotoSubida), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(SubirFotoActivity.this, MainActivity.class);
                            intent.putExtra("usuario", usuario);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    public void onClickOtraGaleria(View v) {
        Intent intent = new Intent(this, SubirFotoActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("origen", "galeria");
        startActivity(intent);
        finish();
    }

    public void onClickOtraCamara(View v) {
        Intent intent = new Intent(this, SubirFotoActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("origen", "camara");
        startActivity(intent);
        finish();
    }
}