package com.example.entrega2.Widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.RemoteViews;

import com.example.entrega2.Actividades.PuntosInteresActivity;
import com.example.entrega2.Actividades.SubirFotoActivity;
import com.example.entrega2.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

// Widget encargado de mostrar nombres de monumentos alrededor del mundo de forma aleatoria con opción de marcar en los puntos de interés (PuntosInteresActivity) de un usuario
public class MonumentosWidget extends AppWidgetProvider {

    private static String usuario;      // Nombre del usuario que ha creado el widget

    // Método encargado de actualizar los elementos del widget con ID 'appWidgetId'
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Se obtiene el nombre de usuario del widget con ID 'appWidgetId'
        usuario = MonumentosWidgetConfigureActivity.loadUserPref(context, appWidgetId);

        // Se recogen los elementos del widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.monumentos_widget);
        views.setTextViewText(R.id.textViewUsuarioW, context.getString(R.string.Hola) + " " + usuario + "!!!");

        // Se elige un monumento de forma aleatoria y se escribe en el TextView del widget
        views.setImageViewResource(R.id.imageViewPinW, R.drawable.location);
        String monumento = getMonumento(context);
        String nombreMonumento = monumento.split(" --> ")[0];
        double latitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[0]);
        double longitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[1]);
        views.setTextViewText(R.id.textViewMonumentoW, nombreMonumento);

        // Se actualiza el PendingIntent del botón 'Cambiar' del widget encargado de mandar un aviso broadcast para actualizar el widget
        Intent intentCambiar = new Intent(context, MonumentosWidget.class);
        intentCambiar.setAction("com.example.entrega2.ACTUALIZAR_WIDGET");
        intentCambiar.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        Random random = new Random();
        int num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentCambiar = PendingIntent.getBroadcast(context,
                num, intentCambiar, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonCambiarW, pendingIntentCambiar);

        // Se actualiza el PendingIntent del botón 'Marcar' del widget para que abra una actividad PuntosInteresActivity con la localización nuevo monumento
        Intent intentMarcar = new Intent(context, PuntosInteresActivity.class);
        intentMarcar.putExtra("usuario", usuario);
        intentMarcar.putExtra("monumento", nombreMonumento);
        intentMarcar.putExtra("latitud", latitud);
        intentMarcar.putExtra("longitud", longitud);
        random = new Random();
        num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentMarcar = PendingIntent.getActivity(context,
                num, intentMarcar, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonMarcarW, pendingIntentMarcar);

        // Se actualiza el PendingIntent del botón 'Abrir Cámara' del widget encargado abrir la actividad SubirFotoActivity con origen = 'camara'
        Intent intentCamara = new Intent(context, SubirFotoActivity.class);
        intentCamara.putExtra("usuario", usuario);
        intentCamara.putExtra("origen", "camara");
        random = new Random();
        num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentCamara = PendingIntent.getActivity(context,
                num, intentCamara, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonCamaraW, pendingIntentCamara);

        // Se actualiza el PendingIntent del botón 'Abrir Galería' del widget encargado abrir la actividad SubirFotoActivity con origen = 'galeria'
        Intent intentGaleria = new Intent(context, SubirFotoActivity.class);
        intentGaleria.putExtra("usuario", usuario);
        intentGaleria.putExtra("origen", "galeria");
        random = new Random();
        num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentGaleria = PendingIntent.getActivity(context,
                num, intentGaleria, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonGaleriaW, pendingIntentGaleria);

        // Se actualizan los elementos del widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    // Método que se ejecuta cada vez que se crea una nueva instancia del widget y cada vez que pasan los milisegundo definidos en el fichero de configuración del widget 'monumentos_widget_info.xml'
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // LLama al método 'updateAppWidget' para todas las instancias del widget
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // Método que se ejecuta al borrar una instancia del widget
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // Para cada instancia del widget borra las preferencias que guardan el usuario asociado y la alarma para actualizar el widget cada 30 segundos
        for (int appWidgetId : appWidgetIds) {
            MonumentosWidgetConfigureActivity.deleteUserPref(context, appWidgetId);
            MonumentosWidgetConfigureActivity.deleteAlarmPref(context, appWidgetId);
        }
    }

    // Método que se ejecuta al crear la primera instancia del widget
    @Override
    public void onEnabled(Context context) { }

    // Método que se ejecuta al borrar la última instancia del widget
    @Override
    public void onDisabled(Context context) { }


    // Método que recibe el aviso broadcast con acción 'ACTUALIZAR_WIDGET' al pulsar el botón 'Cambiar'
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Actualiza la instancia del widget en el que se ha pulsado el botón 'Cambiar', llamando a su método 'updateAppWidget'
        if (intent.getAction().equals("com.example.entrega2.ACTUALIZAR_WIDGET")) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, widgetManager, widgetId);
            }
        }
    }


    // Método encargado de devolver un monumento aleatorio (y sus coordenadas) del fichero monumentos.txt
    public static String getMonumento(Context contexto) {
        ArrayList<String> monumentos = new ArrayList<>();
        String monumento = "";

        try{
            Resources res = contexto.getResources();
            BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.monumentos)));
            String line = reader.readLine();
            while(line != null){
                monumentos.add(line);
                line = reader.readLine();
            }
            reader.close();

            Random randomGenerator = new Random();
            int index = randomGenerator.nextInt(monumentos.size());
            monumento = monumentos.get(index);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return monumento;
    }

}