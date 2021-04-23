package com.example.entrega2.Widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
import com.example.entrega2.Receivers.WidgetReceiver;
import com.example.entrega2.Workers.UsuariosWorker;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;
import java.util.Random;

// Actividad encargada de pedir login al usuario para crear una instancia del widget
public class MonumentosWidgetConfigureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "com.example.entrega2.Widgets.MonumentosWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public MonumentosWidgetConfigureActivity() {
        super();
    }

    // Guarda el nombre de usuario en las preferencias para las instancia del widget
    static void saveUserPref(Context context, int appWidgetId, String usuario) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, usuario);
        prefs.apply();
    }

    // Crea la alarma asociada al widget y escribe en las preferencias el número del PendingIntent asociado a la alarma
    static void createAlarmPref(Context context, int appWidgetId, String usuario) {
        // La alarma se ejecutara cada 30 segundos y mandará un aviso broadcast a WidgetReceiver para actualizar la instancia del widget
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetReceiver.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("usuario", usuario);
        Random random = new Random();
        int num = random.nextInt(1000000 - 0 + 1) + 0;
        PendingIntent pi = PendingIntent.getBroadcast(context, num, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, 30000 , pi);

        // Guarda el número del PendingIntent asociado a la alarma en las preferencias
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + "_alarm", num);
        prefs.apply();
    }

    // Lee el nombre de usuario asociado a la instancia del widget de las preferencias
    static String loadUserPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String user = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (user != null) {
            return user;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    // Elimina el nombre de usuario asociado a la instancia del widget de las preferencias
    static void deleteUserPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    // Elimina de la alarma asociada al widget y elimina de las preferencias el número del PendingIntent asociado a la alarma
    static void deleteAlarmPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int num = prefs.getInt(PREF_PREFIX_KEY + appWidgetId + "_alarm", 0);
        Intent intent = new Intent(context, WidgetReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, num, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        SharedPreferences.Editor prefs2 = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs2.remove(PREF_PREFIX_KEY + appWidgetId + "_alarm");
        prefs2.apply();
    }

    // Se ejecuta al crearse la actividad
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // No añade el widget al escritorio en un principio, para evitar que se añada el widget si el usuario cancela/pulsa Back
        setResult(RESULT_CANCELED);

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

        setContentView(R.layout.monumentos_widget_configure);

        // Se obtiene el ID del widget del Intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Si la actividad se ha iniciado con un Intent sin el ID del widget, se destruye
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
    }

    // Listener 'onClick' del botón 'Añadir' del layout 'monumentos_widget_configure.xml'
    public void onClickAnadir(View v) {
        EditText editTextUsuario = findViewById(R.id.editTextUsuarioW);
        EditText editTextContrasena = findViewById(R.id.editTextContraseñaW);

        String usuario = editTextUsuario.getText().toString();
        String contrasena = editTextContrasena.getText().toString();
        // Se comprueba que todos los EditText (usuario y contraseña) estén rellenos
        if(usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
        }
        // Se comprueba que el nombre del usuario existe en la base de datos
        else {
            // Información a enviar a la tarea
            Data datos = new Data.Builder()
                    .putString("funcion", "validar")
                    .putString("username", usuario)
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
                        // pertenece a la contraseña que ha introducido el usuari
                        if (status != null && status.getState().isFinished()) {
                            String hash = status.getOutputData().getString("resultado");
                            try {
                                // Si el usuario no existe el hash devuelto está vacío
                                if (hash.isEmpty()){
                                    Toast.makeText(this, getString(R.string.UserPassIncorrectos), Toast.LENGTH_SHORT).show();
                                }
                                // Si el hash pertenece a la contraseña
                                else if(PasswordAuthentication.validatePassword(contrasena, hash)) {
                                    // Se guarda el usuario asociado al widget en las preferencias
                                    saveUserPref(this, mAppWidgetId, usuario);

                                    // Se crea la alarma asociada al widget para actualizarlo cada 30 segundos
                                    createAlarmPref(this, mAppWidgetId, usuario);

                                    // Se actualiza la instancia del widget
                                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                                    MonumentosWidget.updateAppWidget(this, appWidgetManager, mAppWidgetId);

                                    // Se crear un Intent para devolver el resultado con la instancia del widget y se destruye la actividad
                                    Intent resultValue = new Intent();
                                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                                    setResult(RESULT_OK, resultValue);
                                    finish();
                                }
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

    // Listener 'onClick' del botón 'Cancelar' del layout 'monumentos_widget_configure.xml'
    public void onClickCancelar(View v) {
        // Se destruye la actividad sin crear la instancia del widget
        finish();
    }
}