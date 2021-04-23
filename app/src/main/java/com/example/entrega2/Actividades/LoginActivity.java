package com.example.entrega2.Actividades;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.entrega2.Receivers.NotificationAlarmReceiver;
import com.example.entrega2.PasswordAuthentication;
import com.example.entrega2.R;
import com.example.entrega2.Workers.TokensWorker;
import com.example.entrega2.Workers.UsuariosWorker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

// Actividad con la que se inicia la aplicación y representa el login de un usuario ya registrado
public class LoginActivity extends AppCompatActivity {

    // Elementos necesarios del layout 'activity_login.xml'
    private ImageView loginImage;
    private EditText username;
    private EditText password;
    private TextView register;

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

        setContentView(R.layout.activity_login);

        // Inicialización de los elementos del layout 'activity_login.xml'
        loginImage = findViewById(R.id.imageViewLogin);
        loginImage.setImageResource(R.drawable.login);
        username = findViewById(R.id.editTextUsuario);
        password = findViewById(R.id.editTextContraseña);
        register = findViewById(R.id.textViewRegistrarse);
        register.setPaintFlags(register.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);    // El texto del TextView se subraya

    }

    // Listener 'onClick' del botón de login del layout 'acitvity_login.xml'
    public void onClickLogin(View v) {
        String usuario = username.getText().toString();
        String contrasena = password.getText().toString();
        // Se comprueba que todos los EditText (usuario y contraseña) estén rellenos
        if(usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
        }
        // Se comprueba que el nombre del usuario existe en la base de datos
        else {
            // Información a enviar a la tarea
            Data datos = new Data.Builder()
                    .putString("funcion", "validar")
                    .putString("username", username.getText().toString())
                    .build();
            // Restricciones a cumplir: es necesaria la conexión a internet
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            // Se ejecuta el trabajo una única vez: 'UsuariosWorker'
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(UsuariosWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(datos)
                    .build();

            // Recuperación de los resultados de la tarea
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                    .observe(this, status -> {
                        // En caso de éxito 'Result.success()', se comprueba si el hash que se ha devuelto de la base de datos
                        // pertenece a la contraseña que ha introducido el usuario
                        if (status != null && status.getState().isFinished()) {
                            String hash = status.getOutputData().getString("resultado");
                            try {
                                // Si el usuario no existe el hash devuelto está vacío
                                if (hash.isEmpty()){
                                    Toast.makeText(this, getString(R.string.UserPassIncorrectos), Toast.LENGTH_SHORT).show();
                                }
                                // Si el hash pertenece a la contraseña, se crea una nueva actividad, se comprueba si el token Firebase del dispositivo
                                // está registrado para el usuario, y se programa la alarma para lanzar una notificación
                                else if(PasswordAuthentication.validatePassword(password.getText().toString(), hash)) {
                                    Intent intent = new Intent(this, MainActivity.class);
                                    intent.putExtra("usuario", usuario);
                                    startActivity(intent);
                                    finish();
                                    comprobarTokens();
                                    lanzarNotificacion(usuario);
                                }
                                // Si el hash no pertenece a la contraseña, se le informa al usuario
                                else {
                                    Toast.makeText(this, getString(R.string.UserPassIncorrectos), Toast.LENGTH_SHORT).show();
                                }
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            } catch (InvalidKeySpecException e) {
                                e.printStackTrace();
                            }
                        }
                    });

            WorkManager.getInstance(this).enqueue(otwr);
        }
    }

    // Listener 'onClick' del TextView para registrarse del layout 'acitvity_login.xml'
    public void onClickRegistrarse(View v) {
        // Se crea una nueva actividad 'RegisterActivity'
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    // Método encargado de obtener los token guardados para el usuario en la base de datos y comprobar si el token Firebase del dispositivo
    // está registrado para el usuario, y en caso de no estarlo, se añade
    private void comprobarTokens(){
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "getTokens")
                .putString("username", username.getText().toString())
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'TokensWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(TokensWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se almacenan los tokens del usuario y se obtiene el token Firebase del dispositivo,
                    // si el token todavía no estaba almacenado, se añade
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);
                            ArrayList<String> tokens = new ArrayList<>();
                            for(int i = 0; i < jsonArray.length(); i++) {
                                tokens.add(jsonArray.getString(i));
                            }

                            FirebaseInstanceId.getInstance().getInstanceId()
                                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                            if (!task.isSuccessful()) {
                                                Log.d("firebase_error", "Firebase error: " + task.getException().toString());
                                                return;
                                            }
                                            String token = task.getResult().getToken();
                                            Log.d("token_firebase", "Token: " + token);

                                            if(!tokens.contains(token)) {
                                                insertToken(token);
                                            }
                                        }
                                    });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Método encargado de insertar un token de Firebase para un usuario en la base de datos
    private void insertToken(String pToken){
        // Información a enviar a la tarea
        Data datos = new Data.Builder()
                .putString("funcion", "insertarToken")
                .putString("username", username.getText().toString())
                .putString("token", pToken)
                .build();
        // Restricciones a cumplir: es necesaria la conexión a internet
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        // Se ejecuta el trabajo una única vez: 'TokensWorker'
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(TokensWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        // Recuperación de los resultados de la tarea
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, status -> {
                    // En caso de éxito 'Result.success()', se informa al usuario que se ha añadido el token correctamente
                    if (status != null && status.getState().isFinished()) {
                        Toast.makeText(this, getString(R.string.NuevoToken), Toast.LENGTH_SHORT).show();
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }

    // Método encargado de programar una alarma a los 30 minutos de hacer login para lanzar un servicio música + notificación
    private void lanzarNotificacion(String usuario) {
        // Se programa una nueva alarma (AlarmManager) con el nombre del usuario que ha hecho login
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationAlarmReceiver.class);
        intent.setAction("alarma");
        intent.putExtra("usuario", usuario);

        // PendingIntent que lanza un broadcast
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Se le indica al AlarmManager cuándo quiere lanzar el PendingIntent
        //  - RTC_WAKEUP: lanza la alarma a la hora especificada despertando el dispositivo
        //  - setExactAndAllowWhileIdle: la alarma funciona en modo descanso (Doze) y con exactitud
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + 30*60000, pendingIntent);
    }
}