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
import com.example.entrega2.Workers.ImagenesWorker;
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

// Actividad que abre la c??mara o la galer??a del dispositivo y, tras elegir una foto, la muestra y da la opci??n de guardarla junto con datos que se pueden insertar
public class SubirFotoActivity extends AppCompatActivity implements DialogoCrearEtiqueta.ListenerdelDialogo{

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    private String origenFoto;                      // Guarda 'camara' o 'galeria' dependiendo desde d??nde se quiere cargar la imagen desde el dispositivo
    private static int CODIGO_GALERIA = 1;
    private static int CODIGO_FOTO_ARCHIVO = 2;

    // Datos necesarios para cargar y guardar la imagen
    private Uri uriimagen;
    private String imageName;
    private File fichImg;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String latitud;
    private String longitud;
    private String etiquetasString;
    private ArrayList<String> etiquetasMostrar;
    private ArrayList<String> etiquetasGuardar;

    // Elementos necesarios del layout 'activity_subir_foto.xml'
    private ImageView imageViewFoto;
    private EditText editTextTituloSubir;
    private EditText editTextDescripcionSubir;
    private RadioButton radioButtonGeolocalizacion;
    private ListView listViewEtiquetas;

    private static boolean primeraVezGaleria = true;

    // Se ejecuta al crearse la actividad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Acceso al las preferencias para obtener el valor de 'idioma'
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma", "es");

        // Crear nueva localizaci??n con el idioma recogido de las preferencias (necesario para mantener el idioma tras cambio de orientacion del dispositivo)
        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        setContentView(R.layout.activity_subir_foto);

        // Inicializaci??n del nombre de usuario y del origen desde d??nde cargar la foto obtenidos a trav??s del Bundle asociado al Intent que ha creado la actividad
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

        // Listener 'onLongClick' de los elementos del ListView de etiquetas
        listViewEtiquetas.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            // Se ejecuta al pulsar prolongadamente en un elemento del ListView de etiquetas
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                // Se borra la etiqueta seleccionada y se actualiza el ListView
                etiquetasMostrar.remove(position);
                etiquetasGuardar.remove(position);
                ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(SubirFotoActivity.this, android.R.layout.simple_list_item_1, etiquetasMostrar);
                listViewEtiquetas.setAdapter(adaptadorEtiquetas);
                return false;
            }
        });

        // Si cambia la orientaci??n del dispositivo, la actividad se destruye y se vuelve a crear
        // Hay que mantener la imagen cargada en el ImageView (dependiendo del origen: c??mara o galer??a) y los elementos del ListView de etiquetas
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

        // Si el origen es 'galeria', se crea un intent para elegir una imagen de la galer??a
        else if(origenFoto.equals("galeria")) {
            Intent elIntentGal = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(elIntentGal, CODIGO_GALERIA);
        }

        // Si el origen es 'camara', se crea un intent para sacar una fotograf??a usando una aplicaci??n de fotograf??a del dispositivo
        else if(origenFoto.equals("camara")) {
            // Se comprueba si hay permisos de c??mara
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Si el permiso no se ha concedido, se pide
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // Se muestra en un di??logo por qu?? es necesario el permiso
                    DialogFragment dialogoPermisosCamara = new DialogoPermisosCamara();
                    dialogoPermisosCamara.show(getSupportFragmentManager(), "permisos_camara");
                } else {
                    // El permiso no esta concedido o el usuario a indicado que no queire que se le vuelva a preguntar
                    Toast.makeText(this, getString(R.string.NoPermisoCamara), Toast.LENGTH_SHORT).show();
                }
                // Se pide el permiso al usuario
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
            }
            else{
                // El permiso esta concedido
                // Se define el directorio donde almacenar la imagen, compatible con lo definido en el FileProvider
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String nombrefich = "IMG_" + timeStamp + "_";
                File directorio = this.getFilesDir();
                try {
                    fichImg = File.createTempFile(nombrefich, ".jpg",directorio);
                    uriimagen = FileProvider.getUriForFile(this, "com.example.entrega2.provider", fichImg);
                } catch (IOException e) {

                }
                // Intent para abrir una aplicaci??n de fotograf??a y sacar foto
                Intent elIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                elIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriimagen);
                startActivityForResult(elIntent, CODIGO_FOTO_ARCHIVO);
            }
        }

        latitud = "";
        longitud = "";
    }

    // Metodo encargado de recoger el resultado obtenido de los intents para abrir la galer??a o la c??mara
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Se recoge la imagen de la galer??a y se pone en el ImageView
        if (requestCode == CODIGO_GALERIA && resultCode == RESULT_OK) {
            uriimagen = data.getData();
            imageName = new File(uriimagen.getPath()).getName();
            imageViewFoto.setImageURI(uriimagen);
            etiquetadoAutomatico();     // M??todo que realiza el etiquetado autom??tico de la imagen
        }

        // Se recoge la imagen sacada con la c??mara y se pone en el ImageView
        else if (requestCode == CODIGO_FOTO_ARCHIVO && resultCode == RESULT_OK) {
            imageName = new File(uriimagen.getPath()).getName();
            imageViewFoto.setImageURI(uriimagen);
            etiquetadoAutomatico();     // M??todo que realiza el etiquetado autom??tico de la imagen
        }

        // En el caso de pulsar el bot??n Back mientras el usuario est?? en la galer??a o la c??mara
        // Se destruye la actividad
        else {
            finish();
            primeraVezGaleria = true;
        }

    }

    // M??todo encargado de realizar el etiquetado autom??tico de la imagen mediante el kit de Machine Learning que ofrece Firebase
    // https://developers.google.com/ml-kit/vision/image-labeling
    private void etiquetadoAutomatico() {
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
                            // Obetener etiquetas y mostarlas en el ListView de etiquetas de la actividad
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
                            etiquetasMostrar = new ArrayList<>();
                            etiquetasGuardar = new ArrayList<>();
                            Toast.makeText(SubirFotoActivity.this, getString(R.string.EtiquetadoFallido), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // M??todo 'onClick' del bot??n 'A??adir' del layout 'activity_subir_foto.xml'
    public void onClickA??adirEtiqueta(View v) {
        // Crea un di??logo para que el usuario introduzca la nueva etiqueta
        DialogFragment dialogoCrearEtiqueta = new DialogoCrearEtiqueta();
        dialogoCrearEtiqueta.show(getSupportFragmentManager(), "crear_etiqueta");
    }

    // M??todo sobrescrito de la interfaz 'DialogoCrearEtiqueta.ListenerdelDialogo' --> Se ejecuta tras introducir una nueva etiqueta y aceptar el di??logo
    @Override
    public void crearEtiqueta(String etiqueta) {
        // A??ade la nueva etiqueta y actualiza el ListView de etiquetas
        etiquetasMostrar.add(etiqueta);
        etiquetasGuardar.add(etiqueta);
        ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(SubirFotoActivity.this, android.R.layout.simple_list_item_1, etiquetasMostrar);
        listViewEtiquetas.setAdapter(adaptadorEtiquetas);
    }

    // M??todo 'onClick' del bot??n 'Subir' del layout 'activity_subir_foto.xml'
    public void onClickSubir(View v) {
        // Se obtienen los valores introducidos en los EditText t??tulo y descripci??n
        // La foto solo se podr?? subir si el t??tulo no est?? vaci?? y si la descripci??n tiene menos de 255 caracteres
        titulo = editTextTituloSubir.getText().toString();
        descripcion = editTextDescripcionSubir.getText().toString();
        if(titulo.isEmpty()) {
            Toast.makeText(this, getString(R.string.EscribeTitulo), Toast.LENGTH_SHORT).show();
        }
        else if(descripcion.length() > 255) {
            Toast.makeText(this, getString(R.string.DescripcionLarga), Toast.LENGTH_SHORT).show();
        }
        else {
            // Se obtiene la fecha de ese momento (fecha de subida de la foto)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                fecha = dtf.format(now);
            }

            // Se obtienen las etiquetas
            for(int i=0; i<etiquetasGuardar.size(); i++) {
                if (etiquetasString == null) {
                    etiquetasString = etiquetasGuardar.get(i) + ";";
                }
                else {
                    etiquetasString += etiquetasGuardar.get(i) + ";";
                }
            }

            // Si el RadioButton de la geolocalizaci??n est?? activado, se obtiene la ubicaci??n actual y se sube la foto
            if(radioButtonGeolocalizacion.isChecked()) {
                establecerUbicacionActual();
            }
            //Si el RadioButton de la geolocalizaci??n est?? desactivado, se sube la foto sin ubicaci??n
            else {
                subirFoto();
            }
        }
    }

    // Obtener geolocalizaci??n actual
    private FusedLocationProviderClient proveedordelocalizacion;
    private LocationCallback actualizador;

    private void establecerUbicacionActual() {
        // Se comprueba si hay permisos para obtener la geolocalizaci??n precisa (ACCESS_FINE_LOCATION)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si el permiso no se ha concedido, se pide
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Se muestra en un di??logo por qu?? es necesario el permiso
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
            // Para detectar cambios de geolocalizaci??n se crea un objeto LocationRequest
            Toast.makeText(this, getString(R.string.ObteniendoGeolocalizacion), Toast.LENGTH_SHORT).show();
            LocationRequest peticion = LocationRequest.create();
            peticion.setInterval(1000);
            peticion.setFastestInterval(5000);
            peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // En el actualizador se indica qu?? hacer al actualizarse la geolocalizaci??n
            actualizador = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (locationResult != null) {
                        latitud = String.valueOf(locationResult.getLastLocation().getLatitude());
                        longitud = String.valueOf(locationResult.getLastLocation().getLongitude());

                        detenerActualizador();      // Cuando se detecta una posici??n v??lida se deja de actualizar
                        subirFoto();                // Una vez obtenida la geolocalizaci??n se sube la foto
                    }
                }
            };

            // *** Se ha utilizado un detector de cambios de posici??n en lugar de obtener la geolocalizaci??n una sola vez,
            //     porque el m??todo getLastLocation() devuelve coordenadas 'null' en muchas de las ocasiones ***
            proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);
        }
    }

    // M??todo encargado de detener la captura de cambios de posiciones
    private void detenerActualizador() {
        proveedordelocalizacion.removeLocationUpdates(actualizador);
    }

    // M??todo encargado de subir la imagen al almacenamiento Firebase Cloud Storage
    private void subirFoto() {
        // Se sube al almacenamiento Firebase Cloud Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference spaceRef = storageRef.child(imageName);
        spaceRef.putFile(uriimagen);

        // Se guardan los datos de la imagen en la base de datos
        // Informaci??n a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "insertar")
                .putString("usuario", usuario)
                .putString("imagen", imageName)
                .putString("titulo", titulo)
                .putString("descripcion", descripcion)
                .putString("fecha", fecha)
                .putString("latitud", latitud)
                .putString("longitud", longitud)
                .putString("etiquetas", etiquetasString)
                .build();
        // Restricciones a cumplir: es necesaria la conexi??n a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una ??nica vez: 'ImagenesWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ImagenesWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperaci??n de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de ??xito 'Result.success()', se destruye la actividad actual y se crea una nueva actividad MainActivity, destruyendo la anterior
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.SubiendoFoto), Toast.LENGTH_LONG).show();

                        if (origenFoto.equals("camara")) {
                            // Elimina la imagen del directorio files de la aplicaci??n
                            boolean deleted = false;
                            try {
                                deleted = fichImg.delete();
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                            if (!deleted) {
                                fichImg.deleteOnExit();
                            }
                        }

                        // Es necesario un delay de unos segundos para que la foto se pueda guardar en el almacenamiento Firebase Cloud Storage
                        // y se pueda descargar al abrir la actividad MainActivity
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SubirFotoActivity.this, getString(R.string.FotoSubida), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SubirFotoActivity.this, MainActivity.class);
                                intent.putExtra("usuario", usuario);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                primeraVezGaleria = true;
                            }
                        }, 4000);
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // M??todo 'onClick' del bot??n 'Elegir otra' del layout 'activity_subir_foto.xml'
    public void onClickOtraGaleria(View v) {
        // Se destruye la actividad y se vuelve a crear otra con origen = 'galeria'
        Intent intent = new Intent(this, SubirFotoActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("origen", "galeria");
        startActivity(intent);
        finish();
        primeraVezGaleria = true;
    }

    // M??todo 'onClick' del bot??n 'Sacar otra' del layout 'activity_subir_foto.xml'
    public void onClickOtraCamara(View v) {
        // Se destruye la actividad y se vuelve a crear otra con origen = 'camara'
        Intent intent = new Intent(this, SubirFotoActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("origen", "camara");
        startActivity(intent);
        finish();
    }

    // M??todo sobrescrito que se ejecuta al permitir o denegar los permisos de c??mara o geolocalizaci??n
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.CAMERA)) {
                    // Si el permiso de c??mara se ha concedido, se destruye la actividad y se vuelve a crear
                    Intent intent = new Intent(this, SubirFotoActivity.class);
                    intent.putExtra("usuario", usuario);
                    intent.putExtra("origen", origenFoto);
                    startActivity(intent);
                    finish();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Si el permiso de geolocalizaci??n se ha concedido, se sube la foto
                    subirFoto();
                } else if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Si el permiso de geolocalizaci??n se ha denegado, se desactiva el RadioButton de la geolocalizaci??n
                    radioButtonGeolocalizacion.setChecked(false);
                } else {
                    // Si el permiso de c??mara se ha denegado, se destruye la actividad
                    finish();
                    primeraVezGaleria = true;
                }
                return;
            }
        }
    }

    // M??todo sobrescrito que se llama antes de destruir la actividad
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Se guarda la informaci??n de la imagen (dependiendo de si se ha cargado desde c??mara o galer??a) y las etiquetas
        outState.putString("rotacion", "true");
        outState.putString("origen", origenFoto);
        if(origenFoto.equals("camara")) {
            outState.putString("fichImg", fichImg.toString());
        }
        else if(origenFoto.equals("galeria")) {
            // El booleano 'primeraVezGaleria' sirve para gestionar los cambios de orientaci??n si todavia no se ha elegido una imagen de la galer??a
            if(!primeraVezGaleria && uriimagen != null) {
                outState.putString("uriimagen", uriimagen.toString());
            }
            else {
                primeraVezGaleria = false;
            }
        }
        outState.putStringArrayList("etiquetasMostrar", etiquetasMostrar);
        outState.putStringArrayList("etiquetasGuardar", etiquetasGuardar);
    }

    @Override
    public void onBackPressed() {
        primeraVezGaleria = true;
        finish();
    }
}