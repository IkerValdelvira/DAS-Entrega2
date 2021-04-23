package com.example.entrega2.Receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import com.example.entrega2.Actividades.AnadirAmigoActivity;
import com.example.entrega2.Actividades.MainActivity;
import com.example.entrega2.Actividades.PuntosInteresActivity;
import com.example.entrega2.ServicioMusicaNotificacion;

// BroadcastReceiver que se utiliza para recibir la alarma a los 30 minutos de hacer login en la aplicación y que pone en marcha un servicio música + notificación
public class NotificationAlarmReceiver extends BroadcastReceiver {

    // Se ejecuta al recibir un aviso de mensaje de broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        // Si la acción del aviso broadcast es 'alarma' se tratará
        if(intent.getAction().equals("alarma")) {
            // Se recogen los datos del Intent que ha creado el aviso broadcast mediante un Bundle
            Bundle extras = intent.getExtras();
            String usuario = "";
            if (extras != null) {
                usuario = extras.getString("usuario");          // Nombre de usuario
            }

            // Se lanza el servicio ServicioMusicaNotificacion música + notificación
            Intent myService = new Intent(context, ServicioMusicaNotificacion.class);
            myService.putExtra("usuario", usuario);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(myService);
            } else {
                context.startService(myService);
            }
        }

    }
}
