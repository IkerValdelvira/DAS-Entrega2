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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.entrega2.Dialogos.DialogoCrearEtiqueta;
import com.example.entrega2.Dialogos.DialogoPermisosCamara;
import com.example.entrega2.Dialogos.DialogoPermisosLocalizacion;
import com.example.entrega2.R;
import com.example.entrega2.Workers.InsertarFotoWorker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubirFotoActivity extends AppCompatActivity implements DialogoCrearEtiqueta.ListenerdelDialogo{

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    private String origenFoto;
    private ImageView imageViewFoto;

    private static int CODIGO_GALERIA = 1;
    private static int CODIGO_FOTO_ARCHIVO = 2;

    private Uri uriimagen;
    private String imageName;
    private File fichImg;
    private ArrayList<String> etiquetasMostrar;
    private ArrayList<String> etiquetasGuardar;

    private EditText editTextTituloSubir;
    private EditText editTextDescripcionSubir;
    private RadioButton radioButtonGeolocalizacion;
    private ListView listViewEtiquetas;

    private String titulo;
    private String descripcion;
    private String fecha;
    private String latitud;
    private String longitud;
    private String etiquetasString;

    private static boolean primeraVez = true;

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
        editTextTituloSubir = findViewById(R.id.editTextTituloSubir);
        editTextDescripcionSubir = findViewById(R.id.editTextDescripcionSubir);
        radioButtonGeolocalizacion = findViewById(R.id.radioButtonGeolocalizacion);
        listViewEtiquetas = findViewById(R.id.listViewEtiquetas);

        listViewEtiquetas.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                etiquetasMostrar.remove(position);
                etiquetasGuardar.remove(position);
                ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(SubirFotoActivity.this, android.R.layout.simple_list_item_1, etiquetasMostrar);
                listViewEtiquetas.setAdapter(adaptadorEtiquetas);
                return false;
            }
        });

        if (savedInstanceState != null && savedInstanceState.getString("rotacion").equals("true")) {
            if(savedInstanceState.getString("origen").equals("camara")){
                fichImg = new File(savedInstanceState.getString("fichImg"));
                uriimagen = FileProvider.getUriForFile(this, "com.example.entrega2.provider", fichImg);
                imageName = new File(uriimagen.getPath()).getName();
                imageViewFoto.setImageURI(uriimagen);
            }
            else if(savedInstanceState.getString("origen").equals("galeria") && savedInstanceState.getString("uriimagen") != null){
                uriimagen = Uri.parse(savedInstanceState.getString("uriimagen"));
                imageName = new File(uriimagen.getPath()).getName();
                imageViewFoto.setImageURI(uriimagen);
            }
            etiquetasMostrar = savedInstanceState.getStringArrayList("etiquetasMostrar");
            etiquetasGuardar = savedInstanceState.getStringArrayList("etiquetasGuardar");
            if(etiquetasMostrar != null) {
            ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(SubirFotoActivity.this, android.R.layout.simple_list_item_1, etiquetasMostrar);
            listViewEtiquetas.setAdapter(adaptadorEtiquetas);
            }
        }
        else if(origenFoto.equals("galeria")) {
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
            imageName = new File(uriimagen.getPath()).getName();
            imageViewFoto.setImageURI(uriimagen);
            etiquetadoAutomatico();
        }

        else if (requestCode == CODIGO_FOTO_ARCHIVO && resultCode == RESULT_OK) {
            imageName = new File(uriimagen.getPath()).getName();
            imageViewFoto.setImageURI(uriimagen);
            etiquetadoAutomatico();
        }

        else {
            finish();
            primeraVez = true;
        }

    }

    private void setImage() {
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

    private void etiquetadoAutomatico() {
        // Etiquetado ML
        try {
            // Crear InputImage desde la URI de la imagen
            InputImage image = InputImage.fromFilePath(this, uriimagen);

            // Obtener instancia de ImageLabeler (con opciones por defecto)
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            // Procesar etiquetado de la imagen
            labeler.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                        @Override
                        public void onSuccess(List<ImageLabel> labels) {
                            // Obetener etiquetas
                            etiquetasMostrar = new ArrayList<>();
                            etiquetasGuardar = new ArrayList<>();
                            for(int i=0; i<labels.size(); i++) {
                                String tag = labels.get(i).getText();
                                float confidence = labels.get(i).getConfidence();
                                String confidenceAux = String.format("%.3f", confidence);
                                etiquetasMostrar.add(tag + " (" + confidenceAux + ")");
                                etiquetasGuardar.add(tag);
                            }

                            ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(SubirFotoActivity.this, android.R.layout.simple_list_item_1, etiquetasMostrar);
                            listViewEtiquetas.setAdapter(adaptadorEtiquetas);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            // ...
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickAñadirEtiqueta(View v) {
        DialogFragment dialogoCrearEtiqueta = new DialogoCrearEtiqueta();
        dialogoCrearEtiqueta.show(getSupportFragmentManager(), "crear_etiqueta");
    }

    @Override
    public void crearEtiqueta(String etiqueta) {
        etiquetasMostrar.add(etiqueta);
        etiquetasGuardar.add(etiqueta);
        ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(SubirFotoActivity.this, android.R.layout.simple_list_item_1, etiquetasMostrar);
        listViewEtiquetas.setAdapter(adaptadorEtiquetas);
    }

    public void onClickSubir(View v) {
        titulo = editTextTituloSubir.getText().toString();
        descripcion = editTextDescripcionSubir.getText().toString();
        if(titulo.isEmpty()) {
            Toast.makeText(this, getString(R.string.EscribeTitulo), Toast.LENGTH_SHORT).show();
        }
        else if(descripcion.length() > 255) {
            Toast.makeText(this, getString(R.string.DescripcionLarga), Toast.LENGTH_SHORT).show();
        }
        else {
            // Fecha
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                fecha = dtf.format(now);
            }

            // Etiquetas
            for(int i=0; i<etiquetasGuardar.size(); i++) {
                if (etiquetasString == null) {
                    etiquetasString = etiquetasGuardar.get(i) + ";";
                }
                else {
                    etiquetasString += etiquetasGuardar.get(i) + ";";
                }
            }

            // Opcion geolocalizacion
            if(radioButtonGeolocalizacion.isChecked()) {
                establecerUbicacionActual();
            }
            else {
                subirFoto();
            }
        }
    }

    // Obtener ubicacion actual
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
                        latitud = String.valueOf(locationResult.getLastLocation().getLatitude());
                        longitud = String.valueOf(locationResult.getLastLocation().getLongitude());

                        detenerActualizador();
                        subirFoto();
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

    private void subirFoto() {
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
                .putString("etiquetas", etiquetasString)
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
                        Toast.makeText(this, getString(R.string.SubiendoFoto), Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SubirFotoActivity.this, getString(R.string.FotoSubida), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SubirFotoActivity.this, MainActivity.class);
                                intent.putExtra("usuario", usuario);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                primeraVez = true;
                            }
                        }, 4000);
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    public void onClickOtraGaleria(View v) {
        primeraVez = true;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.CAMERA)) {
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
                    Intent intent = new Intent(this, SubirFotoActivity.class);
                    intent.putExtra("usuario", usuario);
                    intent.putExtra("origen", origenFoto);
                    startActivity(intent);
                    finish();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
                    subirFoto();
                } else if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    radioButtonGeolocalizacion.setChecked(false);
                } else {
                    // PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                    finish();
                    primeraVez = true;
                }
                return;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("rotacion", "true");
        outState.putString("origen", origenFoto);
        if(origenFoto.equals("camara")) {
            outState.putString("fichImg", fichImg.toString());
        }
        else if(origenFoto.equals("galeria")) {
            if(!primeraVez && uriimagen != null) {
                outState.putString("uriimagen", uriimagen.toString());
            }
            else {
                primeraVez = false;
            }
        }
        outState.putStringArrayList("etiquetasMostrar", etiquetasMostrar);
        outState.putStringArrayList("etiquetasGuardar", etiquetasGuardar);
    }

    @Override
    public void onBackPressed() {
        primeraVez = true;
        finish();
    }
}