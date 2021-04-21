package com.example.entrega2.Actividades;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.entrega2.PasswordAuthentication;
import com.example.entrega2.R;
import com.example.entrega2.Workers.UsuariosWorker;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

// Actividad que representa el registro de un nuevo usuario en la aplicación
public class RegisterActivity extends AppCompatActivity {

    // Elementos necesarios del layout 'activity_register.xml'
    ImageView registerImage;
    EditText username;
    EditText password;
    EditText paswword2;

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

        setContentView(R.layout.activity_register);

        // Inicialización de los elementos del layout 'activity_register.xml'
        registerImage = findViewById(R.id.imageViewRegister);
        registerImage.setImageResource(R.drawable.registro);
        username = findViewById(R.id.editTextUsuarioR);
        password = findViewById(R.id.editTextContraseñaR);
        paswword2 = findViewById(R.id.editTextContraseñaR2);

    }

    // Listener 'onClick' del botón de registro del layout 'acitvity_register.xml'
    public void onClickRegistro(View v) throws InvalidKeySpecException, NoSuchAlgorithmException {
        String usuario = username.getText().toString();
        String contrasena1 = password.getText().toString();
        String contrasena2 = paswword2.getText().toString();
        // Se comprueba que todos los EditText (usuario, contraseña y repetición de la contraseña) estén rellenos
        if(usuario.isEmpty() || contrasena1.isEmpty() || contrasena2.isEmpty()) {
            Toast.makeText(this, getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
        }
        // Se comprueba que la contraseña y la repetición de la contraseña sean iguales
        else if(!contrasena1.equals(contrasena2)) {
            Toast.makeText(this, getString(R.string.PassDiferentes), Toast.LENGTH_SHORT).show();
        }
        // Se comprueba si existe un usuario con los datos introducidos en la base de datos local
        else{
            String hashPassword = PasswordAuthentication.generateStrongPasswordHash(password.getText().toString());
            Data datos = new Data.Builder()
                    .putString("funcion", "validar")
                    .putString("username", username.getText().toString())
                    .putString("password", hashPassword)
                    .build();
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(UsuariosWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(datos)
                    .build();

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                    .observe(this, status -> {
                        if (status != null && status.getState().isFinished()) {
                            String result = status.getOutputData().getString("resultado");
                            if(!result.isEmpty()) {
                                // Si el usuario existe, se crea un Toast notificándolo
                                Toast.makeText(this, getString(R.string.UsuarioRepetido), Toast.LENGTH_SHORT).show();
                            }
                            else {
                                // Si el usuario no existe, se inserta en la base de datos local y se destruye la actividad actual
                                Data datos2 = new Data.Builder()
                                        .putString("funcion", "insertar")
                                        .putString("username", username.getText().toString())
                                        .putString("password", hashPassword)
                                        .build();

                                OneTimeWorkRequest otwr2 = new OneTimeWorkRequest.Builder(UsuariosWorker.class)
                                        .setConstraints(restricciones)
                                        .setInputData(datos2)
                                        .build();

                                WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr2.getId())
                                        .observe(this, status2 -> {
                                            if (status2 != null && status2.getState().isFinished()) {
                                                Toast.makeText(this, getString(R.string.UsuarioRegistrado), Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        });

                                WorkManager.getInstance(this).enqueue(otwr2);
                            }
                        }
                    });

            WorkManager.getInstance(this).enqueue(otwr);
        }
    }
}