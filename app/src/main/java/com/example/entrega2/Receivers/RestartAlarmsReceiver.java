package com.example.entrega2.Receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.entrega2.R;
import com.example.entrega2.Widgets.MonumentosWidget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

// BroadcastReceiver que se utiliza para poner en marcha las alarmas de actualización del widget después de reiniciar el dispositivo
public class RestartAlarmsReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "com.example.entrega2.Widgets.MonumentosWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    // Se ejecuta al recibir un aviso de mensaje de broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        // Si la acción del aviso broadcast es 'BOOT_COMPLETED' se tratará --> Se ha terminado el arranque del sistema
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            // Se obtienen los IDs de las instancias del widget
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, MonumentosWidget.class);
            int[] appWidgetIds = widgetManager.getAppWidgetIds(cn);

            // Por cada instancia del widget se pone en marcha una alarma para actualizarla cada 30 segundos
            for (int appWidgetId : appWidgetIds) {
                // Se obtiene el nombre de usuario asociado a la instancia del widget
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
                String usuario = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
                if (usuario != null) {
                    // La alarma se ejecutara cada 30 segundos y mandará un aviso broadcast a WidgetReceiver para actualizar la instancia del widget
                    AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                    Intent i = new Intent(context, WidgetReceiver.class);
                    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    i.putExtra("usuario", usuario);
                    Random random = new Random();
                    int num = random.nextInt(1000000 - 0 + 1) + 0;
                    PendingIntent pi = PendingIntent.getBroadcast(context, num, i, PendingIntent.FLAG_UPDATE_CURRENT);
                    am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, 30000 , pi);

                    // Guarda el número del PendingIntent asociado a la alarma en las preferencias
                    SharedPreferences.Editor prefs2 = context.getSharedPreferences(PREFS_NAME, 0).edit();
                    prefs2.putInt(PREF_PREFIX_KEY + appWidgetId + "_alarm", num);
                    prefs2.apply();
                }
            }
        }
    }

}
