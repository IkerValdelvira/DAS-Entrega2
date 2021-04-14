package com.example.entrega2.Widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.entrega2.Actividades.MainActivity;
import com.example.entrega2.PasswordAuthentication;
import com.example.entrega2.R;
import com.example.entrega2.Workers.ValidarUsuarioWorker;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

/**
 * The configuration screen for the {@link Widget Widget} AppWidget.
 */
public class WidgetConfigureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "com.example.entrega2.Widgets.Widget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int idWidgetAConfigurar = AppWidgetManager.INVALID_APPWIDGET_ID;
    /*
    EditText mAppWidgetText;
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = WidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            String widgetText = mAppWidgetText.getText().toString();
            saveTitlePref(context, mAppWidgetId, widgetText);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            Widget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    public WidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }
    */

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
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

        setContentView(R.layout.widget_configure);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            idWidgetAConfigurar = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (idWidgetAConfigurar == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        //mAppWidgetText.setText(loadTitlePref(WidgetConfigureActivity.this, mAppWidgetId));

        TextView textView = findViewById(R.id.textViewExplicacion);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);       // Se justifica el texto dentro del TextView 'sinopsis'
        }

    }

    public void onClickAnadir(View v) {
        EditText editTextUsuario = findViewById(R.id.editTextUsuarioW);
        EditText editTextContrasena = findViewById(R.id.editTextContraseñaW);
        String usuario = editTextUsuario.getText().toString();
        String contrasena = editTextContrasena.getText().toString();

        // Se comprueba que todos los EditText (usuario y contraseña) estén rellenos
        if(usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, getString(R.string.RellenarCampos), Toast.LENGTH_SHORT).show();
        }
        // Se comprueba que el usuario existe en la base de datos local
        else {
            Data datos = new Data.Builder()
                    .putString("username", usuario)
                    .build();
            Constraints restricciones = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ValidarUsuarioWorker.class)
                    .setConstraints(restricciones)
                    .setInputData(datos)
                    .build();

                    WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                            .observe(this, status -> {
                                if (status != null && status.getState().isFinished()) {
                                    String hash = status.getOutputData().getString("resultado");
                                    try {
                                        if (hash.isEmpty()){
                                            Toast.makeText(this, getString(R.string.UserPassIncorrectos), Toast.LENGTH_SHORT).show();
                                        }
                                        else if(PasswordAuthentication.validatePassword(contrasena, hash)) {
                                            // When the button is clicked, store the string locally
                                            SharedPreferences.Editor prefs = this.getSharedPreferences(PREFS_NAME, 0).edit();
                                            prefs.putString(PREF_PREFIX_KEY + idWidgetAConfigurar, usuario);
                                            prefs.apply();

                                            // It is the responsibility of the configuration activity to update the app widget
                                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                                            Widget.updateAppWidget(this, appWidgetManager, idWidgetAConfigurar);

                                            // Make sure we pass back the original appWidgetId
                                            Intent resultadoConfig = new Intent();
                                            resultadoConfig.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, idWidgetAConfigurar);
                                            setResult(RESULT_OK, resultadoConfig);
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

    public void onClickCancelar(View v) {

    }

}