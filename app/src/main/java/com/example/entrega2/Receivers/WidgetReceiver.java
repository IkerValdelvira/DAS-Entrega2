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

// BroadcastReceiver que se utiliza para recibir la alarma cada 30 segundos al crear un widget y que se encarga de actualizarlo
public class WidgetReceiver extends BroadcastReceiver {

    // Se ejecuta al recibir un aviso de mensaje de broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        // Se obtienen los elementos del widget
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.monumentos_widget);

        // Se elige un monumento de forma aleatoria y se escribe en el TextView del widget
        String monumento = MonumentosWidget.getMonumento(context);
        String nombreMonumento = monumento.split(" --> ")[0];
        double latitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[0]);
        double longitud = Double.parseDouble(monumento.split(" --> ")[1].split(";")[1]);
        remoteViews.setTextViewText(R.id.textViewMonumentoW, nombreMonumento);

        // Se actualiza el PendingIntent del botón 'Marcar' del widget para que abra una actividad PuntosInteresActivity con la localización nuevo monumento
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

        // Se obtiene el ID la instancia del widget que ha mandado el aviso broadcast y se actualizan sus elementos
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}