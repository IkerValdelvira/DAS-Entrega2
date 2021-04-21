package com.example.entrega2.Widgets;

import android.app.AlarmManager;
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
import com.example.entrega2.Receivers.WidgetReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link MonumentosWidgetConfigureActivity MonumentosWidgetConfigureActivity}
 */
public class MonumentosWidget extends AppWidgetProvider {

    private static String usuario;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        usuario = MonumentosWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.monumentos_widget);
        views.setTextViewText(R.id.textViewUsuarioW, context.getString(R.string.Hola) + " " + usuario + "!!!");

        // Elegir monumento aleatorio
        views.setImageViewResource(R.id.imageViewPinW, R.drawable.location);
        String monumento = getMonumento(context);
        String nombreMonumento = monumento.split(" --> ")[0];
        double latitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[0]);
        double longitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[1]);
        views.setTextViewText(R.id.textViewMonumentoW, nombreMonumento);

        // Boton cambiar monumento
        Intent intentCambiar = new Intent(context, MonumentosWidget.class);
        intentCambiar.setAction("com.example.entrega2.ACTUALIZAR_WIDGET");
        intentCambiar.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        Random random = new Random();
        int num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentCambiar = PendingIntent.getBroadcast(context,
                num, intentCambiar, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonCambiarW, pendingIntentCambiar);

        // Boton marcar monumento
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

        // Boton abrir camara
        Intent intentCamara = new Intent(context, SubirFotoActivity.class);
        intentCamara.putExtra("usuario", usuario);
        intentCamara.putExtra("origen", "camara");
        random = new Random();
        num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentCamara = PendingIntent.getActivity(context,
                num, intentCamara, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonCamaraW, pendingIntentCamara);

        // Boton abrir galeria
        Intent intentGaleria = new Intent(context, SubirFotoActivity.class);
        intentGaleria.putExtra("usuario", usuario);
        intentGaleria.putExtra("origen", "galeria");
        random = new Random();
        num = random.nextInt(100000 - 0 + 1) + 0;
        PendingIntent pendingIntentGaleria = PendingIntent.getActivity(context,
                num, intentGaleria, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonGaleriaW, pendingIntentGaleria);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            MonumentosWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
            MonumentosWidgetConfigureActivity.deleteAlarmPref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("com.example.entrega2.ACTUALIZAR_WIDGET")) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, widgetManager, widgetId);
            }
        }
    }


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