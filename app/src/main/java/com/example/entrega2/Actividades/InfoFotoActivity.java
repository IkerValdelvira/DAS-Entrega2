package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.entrega2.R;
import com.example.entrega2.Workers.ActualizarFotoWorker;
import com.example.entrega2.Workers.EliminarFotoWorker;
import com.example.entrega2.Workers.GetFotoWorker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class InfoFotoActivity extends AppCompatActivity {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    private String fotoID;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String latitud;
    private String longitud;

    private ImageView imageViewFoto;
    private EditText editTextTitulo;
    private EditText editTextDescripcion;
    private TextView textViewFecha;

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

        imageViewFoto = findViewById(R.id.imageViewFotoI);
        editTextTitulo = findViewById(R.id.editTextTituloI);
        textViewFecha = findViewById(R.id.textViewFechaI);
        editTextDescripcion = findViewById(R.id.editTextDescripcionI);

        // Extraer información de la foto de la base de datos
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .putString("imagen", fotoID)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GetFotoWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONObject foto = new JSONObject(result);

                            titulo = foto.getString("titulo");
                            descripcion = foto.getString("descripcion");
                            fecha = foto.getString("fecha");
                            latitud = foto.getString("latitud");
                            longitud = foto.getString("longitud");

                            editTextTitulo.setText(titulo);
                            editTextDescripcion.setText(descripcion);
                            textViewFecha.setText(fecha);

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

    public void onClickUbicacion(View v) {
        Intent intent= new Intent(this, UbicacionActivity.class);
        intent.putExtra("titulo", titulo);
        intent.putExtra("latitud", latitud);
        intent.putExtra("longitud", longitud);
        startActivityForResult(intent, 666);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TRATAR EL RESULTADO:
        if (requestCode == 666 && resultCode == RESULT_OK) {
            latitud = data.getStringExtra("latitud");
            longitud = data.getStringExtra("longitud");
        }
        else if (requestCode == 666 && resultCode == RESULT_CANCELED) {}
    }

    public void onClickEditar(View v) {
        titulo = editTextTitulo.getText().toString();
        descripcion = editTextDescripcion.getText().toString();

        if(titulo.isEmpty()) {
            Toast.makeText(this, getString(R.string.EscribeTitulo), Toast.LENGTH_SHORT).show();
        }
        else {
            Data datos = new Data.Builder()
                    .putString("usuario", usuario)
                    .putString("imagen", fotoID)
                    .putString("titulo", titulo)
                    .putString("descripcion", descripcion)
                    .putString("latitud", latitud)
                    .putString("longitud", longitud)
                    .build();
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ActualizarFotoWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                    .observe(this, status -> {
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

    public void onClickEliminar(View v) {
        // Eliminar de la base de datos
        Data datos = new Data.Builder()
                .putString("usuario", usuario)
                .putString("imagen", fotoID)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(EliminarFotoWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
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
}