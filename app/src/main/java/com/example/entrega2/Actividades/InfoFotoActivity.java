package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.entrega2.Dialogos.DialogoCompartirFoto;
import com.example.entrega2.Dialogos.DialogoCrearEtiqueta;
import com.example.entrega2.Dialogos.DialogoFotoAmpliada;
import com.example.entrega2.R;
import com.example.entrega2.Workers.AmigosWorker;
import com.example.entrega2.Workers.ImagenesWorker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

// Actividad que muestra y permite editar la información de la foto seleccionada
public class InfoFotoActivity extends AppCompatActivity implements DialogoCompartirFoto.ListenerdelDialogo, DialogoCrearEtiqueta.ListenerdelDialogo {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    // Información necesaria para mostrar en la actividad
    private String fotoID;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String latitud;
    private String longitud;
    private ArrayList<String> etiquetas;

    // Elementos necesarios del layout 'activity_info_foto.xml'
    private ImageView imageViewFoto;
    private EditText editTextTitulo;
    private EditText editTextDescripcion;
    private TextView textViewFecha;
    private ListView listViewEtiquetas;

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

        setContentView(R.layout.activity_info_foto);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
            fotoID = extras.getString("foto");
        }

        // Inicialización de los elementos del layout 'activity_info_foto.xml'
        imageViewFoto = findViewById(R.id.imageViewFotoI);
        editTextTitulo = findViewById(R.id.editTextTituloI);
        textViewFecha = findViewById(R.id.textViewFechaI);
        editTextDescripcion = findViewById(R.id.editTextDescripcionI);
        listViewEtiquetas = findViewById(R.id.listViewEtiquetasI);

        // Listener 'onLongClick' al seleccionar un elemento del ListView con las etiquetas relacionadas con la foto
        listViewEtiquetas.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            // Se ejecuta al pulsar de manera prolongada sobre un elemento de 'listViewEtiquetas'
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id){
                // Borra la etiqueta seleccionada y actualiza el 'listViewEtiquetas'
                etiquetas.remove(position);
                ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(InfoFotoActivity.this, android.R.layout.simple_list_item_1, etiquetas);
                listViewEtiquetas.setAdapter(adaptadorEtiquetas);
                return false;
            }
        });

        // Se obtiene la información de la foto seleccionada
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getFoto")
                .putString("username", usuario)
                .putString("imagen", fotoID)
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
                    // En caso de éxito 'Result.success()', se obtienen la imagen, el título, la descripcion, la fecha, la latitud, la longitud y las etiquetas
                    // de la foto seleccionada, y se muestran el los elementos View de la actividad
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONObject foto = new JSONObject(result);

                            titulo = foto.getString("titulo");
                            descripcion = foto.getString("descripcion");
                            fecha = foto.getString("fecha");
                            latitud = foto.getString("latitud");
                            longitud = foto.getString("longitud");
                            String etiquetasString = foto.getString("etiquetas");


                            editTextTitulo.setText(titulo);
                            editTextDescripcion.setText(descripcion);
                            textViewFecha.setText(fecha);
                            etiquetas = new ArrayList<>();
                            if(etiquetasString.length()>0) {
                                String[] etiquetasAux = etiquetasString.split(";");
                                for(int i=0; i<etiquetasAux.length; i++) {
                                    etiquetas.add(etiquetasAux[i]);
                                }
                                ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(InfoFotoActivity.this, android.R.layout.simple_list_item_1, etiquetas);
                                listViewEtiquetas.setAdapter(adaptadorEtiquetas);
                            }

                            // Se descarga la imagen del almacenamiento Firebase Cloud Storage y se muestra en un ImageView (mediante la librería Glide)
                            FirebaseStorage storage = FirebaseStorage.getInstance();
                            StorageReference storageRef = storage.getReference();
                            StorageReference pathReference = storageRef.child(fotoID);
                            pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Glide.with(InfoFotoActivity.this).load(uri).into(imageViewFoto);
                                }
                            });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Listener 'onClick' del ImageView 'imageViewFoto' del layout 'activity_info_foto.xml'
    public void onClickAmpliar(View v) {
        // Crea un diálogo encargado de mostrar la imagen en tamaño pantalla completa
        DialogFragment dialogoFotoAmpliada = new DialogoFotoAmpliada(fotoID);
        dialogoFotoAmpliada.show(getSupportFragmentManager(), "ampliar_foto");
    }

    // Listener 'onClick' del botón 'Añadir' del layout 'activity_info_foto.xml'
    public void onClickAñadirEtiqueta(View v) {
        // Crea un diálogo que recoge la nueva etiqueta escrita por el usuario para la foto
        DialogFragment dialogoCrearEtiqueta = new DialogoCrearEtiqueta();
        dialogoCrearEtiqueta.show(getSupportFragmentManager(), "crear_etiqueta");
    }

    // Método sobrescrito de la interfaz 'DialogoCrearEtiqueta.ListenerdelDialogo' --> Se ejecuta al escribir una etiqueta y aceptar el diálogo para crear una nueva etiqueta en la foto
    @Override
    public void crearEtiqueta(String etiqueta) {
        // Añade la etiqueta y actualiza el 'listViewEtiquetas'
        etiquetas.add(etiqueta);
        ArrayAdapter adaptadorEtiquetas = new ArrayAdapter<String>(InfoFotoActivity.this, android.R.layout.simple_list_item_1, etiquetas);
        listViewEtiquetas.setAdapter(adaptadorEtiquetas);
    }

    // Listener 'onClick' del botón 'Mostrar/Editar Ubicación' del layout 'activity_info_foto.xml'
    public void onClickUbicacion(View v) {
        // Abre una actividad UbicacionActivity que muestra una vista GoogleMaps y permite editar la ubicación establecida en la foto
        Intent intent= new Intent(this, UbicacionActivity.class);
        intent.putExtra("titulo", titulo);
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        startActivityForResult(intent, 666);
    }

    // Metodo encargado de recoger el resultado obtenido de la actividad UbicacionActivity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Si el resultado es 'RESULT_OK' se establecen la latitud y longitud de la nueva ubicación
        if (requestCode == 666 && resultCode == RESULT_OK) {
            latitud = data.getStringExtra("latitud");
            longitud = data.getStringExtra("longitud");
        }
    }

    // Listener 'onClick' del botón 'Editar' del layout 'activity_info_foto.xml'
    public void onClickEditar(View v) {
        // Se obtienen los valores de los datos introducidos por el usuario en la actividad
        titulo = editTextTitulo.getText().toString();
        descripcion = editTextDescripcion.getText().toString();
        String etiquetasString = "";
        for(int i=0; i<etiquetas.size(); i++) {
            etiquetasString += etiquetas.get(i) + ";";
        }

        if(titulo.isEmpty()) {
            // Si el título esta vacío se le informa al usuario
            Toast.makeText(this, getString(R.string.EscribeTitulo), Toast.LENGTH_SHORT).show();
        }
        else {
            // Se actualizan la información de la foto seleccionada en la base de datos
            // Información a enviar a la tarea
            Data datos = new Data.Builder()
                    .putString("funcion", "actualizar")
                    .putString("usuario", usuario)
                    .putString("imagen", fotoID)
                    .putString("titulo", titulo)
                    .putString("descripcion", descripcion)
                    .putString("latitud", latitud)
                    .putString("longitud", longitud)
                    .putString("etiquetas", etiquetasString)
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
                        // En caso de éxito 'Result.success()', se abre una nueva actividad MainActivity, eliminando la actual 'InfoFotoActivity'
                        // y la anterior 'MainActivity'
                        if (status != null && status.getState().isFinished()) {
                            Toast.makeText(InfoFotoActivity.this, getString(R.string.FotoEditada), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(InfoFotoActivity.this, MainActivity.class);
                            intent.putExtra("usuario", usuario);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    });

            WorkManager.getInstance(this).enqueue(otwr);
        }
    }

    // Listener 'onClick' del botón 'Eliminar' del layout 'activity_info_foto.xml'
    public void onClickEliminar(View v) {
        // Se elimina la foto seleccionada de la base de datos
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "eliminar")
                .putString("usuario", usuario)
                .putString("imagen", fotoID)
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
                    // En caso de éxito 'Result.success()', se elimina la foto del almacenamiento Firebase Cloud Storage y se abre una nueva
                    // actividad MainActivity, eliminando la actual 'InfoFotoActivity' y la anterior 'MainActivity'
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(InfoFotoActivity.this, getString(R.string.FotoEliminada), Toast.LENGTH_SHORT).show();

                        // Eliminar de Firebase
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReference();
                        StorageReference desertRef = storageRef.child(fotoID);
                        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Intent intent = new Intent(InfoFotoActivity.this, MainActivity.class);
                                intent.putExtra("usuario", usuario);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Listener 'onClick' del botón con icono de compartir del layout 'activity_info_foto.xml'
    public void onClickCompartir(View v) {
        // Da la opción de compartir la foto con los amigos agregados por el usuario

        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getAmigosCompartir")
                .putString("usuario", usuario)
                .putString("imagen", fotoID)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'AmigosWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(AmigosWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se obtienen los nombres de los amigos del usuario, solo los de los amigos a los que todavía
                    // no se les ha compartido la foto, y se crea un díalogo a través del que se podrá elegir con qué amigo se quiere compartir la foto
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONObject jsonObject = new JSONObject(result);

                            JSONArray amigos = jsonObject.getJSONArray("amigos");
                            ArrayList<String> amigosList = new ArrayList<String>();
                            for (int i=0; i<amigos.length(); i++) {
                                amigosList.add(amigos.getString(i));
                            }

                            JSONArray compartidos = jsonObject.getJSONArray("compartidos");
                            ArrayList<String> compartidosList = new ArrayList<String>();
                            for (int i=0; i<compartidos.length(); i++) {
                                compartidosList.add(compartidos.getString(i));
                            }

                            ArrayList<String> mostrar = new ArrayList<>();
                            for (int i=0; i<amigosList.size(); i++) {
                                if(!compartidosList.contains(amigosList.get(i))) {
                                    mostrar.add(amigosList.get(i));
                                }
                            }

                            if(mostrar.isEmpty()) {
                                Toast.makeText(this, getString(R.string.NoAmigosCompartir), Toast.LENGTH_SHORT).show();
                            }
                            else{
                                DialogFragment dialogoCompartirFoto = new DialogoCompartirFoto(usuario, fotoID, mostrar, titulo);
                                dialogoCompartirFoto.show(getSupportFragmentManager(), "compartir_foto");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Método sobrescrito de la interfaz 'DialogoCompartirFoto.ListenerdelDialogo' --> Se ejecuta al elegir unos amigos y aceptar el diálogo para compartir la foto
    @Override
    public void fotoCompartida() {
        // Se le informa al usuario de que la foto se ha compartido correctamente
        Toast.makeText(this, getString(R.string.FotoCompartida), Toast.LENGTH_SHORT).show();
    }
}