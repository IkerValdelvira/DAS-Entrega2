package com.example.entrega2.Receivers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.entrega2.Actividades.PuntosInteresActivity;
import com.example.entrega2.Actividades.SubirFotoActivity;
import com.example.entrega2.R;
import com.example.entrega2.Widgets.MonumentosWidget;
import com.example.entrega2.Widgets.MonumentosWidgetConfigureActivity;

import java.util.Random;

public class WidgetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("RECEIVER 30 SEGUNDOS");
        /*
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.monumentos_widget);

        // Elegir monumento aleatorio
        String monumento = MonumentosWidget.getMonumento(context);
        String nombreMonumento = monumento.split(" --> ")[0];
        double latitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[0]);
        double longitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[1]);
        remoteViews.setTextViewText(R.id.textViewMonumentoW, nombreMonumento);

        // Boton marcar monumento
        Intent intentMarcar = new Intent(context, PuntosInteresActivity.class);
        intentMarcar.putExtra("usuario", intent.getStringExtra("usuario"));
        intentMarcar.putExtra("monumento", nombreMonumento);
        intentMarcar.putExtra("latitud", latitud);
        intentMarcar.putExtra("longitud", longitud);
        PendingIntent pendingIntentMarcar = PendingIntent.getActivity(context,
                1000, intentMarcar, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.buttonMarcarW, pendingIntentMarcar);

        ComponentName tipowidget = new ComponentName(context, MonumentosWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(tipowidget, remoteViews);
        */

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.monumentos_widget);

        // Elegir monumento aleatorio
        String monumento = MonumentosWidget.getMonumento(context);
        String nombreMonumento = monumento.split(" --> ")[0];
        double latitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[0]);
        double longitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[1]);
        remoteViews.setTextViewText(R.id.textViewMonumentoW, nombreMonumento);

        // Boton marcar monumento
        Intent intentMarcar = new Intent(context, PuntosInteresActivity.class);
        intentMarcar.putExtra("usuario", intent.getStringExtra("usuario"));
        intentMarcar.putExtra("monumento", nombreMonumento);
        intentMarcar.putExtra("latitud", latitud);
        intentMarcar.putExtra("longitud", longitud);
        Random random = new Random();
        int num = random.nextInt(1000000 - 0 + 1) + 0;
        PendingIntent pendingIntentMarcar = PendingIntent.getActivity(context,
                num, intentMarcar, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.buttonMarcarW, pendingIntentMarcar);

        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}