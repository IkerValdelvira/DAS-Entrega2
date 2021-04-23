package com.example.entrega2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.entrega2.Actividades.AnadirAmigoActivity;
import com.example.entrega2.Actividades.MainActivity;
import com.example.entrega2.Actividades.PuntosInteresActivity;
import com.example.entrega2.Receivers.NotificationDismissedReceiver;

// Servicio encargado de lanzar una notificación con audio recordándole al usuario que entre en la aplicación
public class ServicioMusicaNotificacion extends Service {

    private MediaPlayer mediaPlayer;        // Objeto para reproducir audio

    // Método que se ejecuta cada vez que se hace 'startService' o 'startForegroundService'
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Creación de una notificación: Manager y Builder
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelID");

        // Creación de un canal NorificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel("channelID", "CanalAlarma", NotificationManager.IMPORTANCE_DEFAULT);

            canal.setDescription("Canal para las notificaciones de alarmas.");
            canal.enableLights(true);
            canal.setLightColor(Color.RED);
            canal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            canal.enableVibration(true);

            manager.createNotificationChannel(canal);
        }

        // Se recogen los datos del Intent que ha creado el aviso broadcast mediante un Bundle
        Bundle extras = intent.getExtras();
        String usuario = "";
        if (extras != null) {
            usuario = extras.getString("usuario");          // Nombre de usuario
        }

        // PendingIntent para abrir MainActivity
        Intent i1 = new Intent(this, MainActivity.class);        // Crea una nueva actividad MainActivity
        i1.putExtra("usuario", usuario);
        i1.putExtra("servicio", "true");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i1, PendingIntent.FLAG_UPDATE_CURRENT);

        // PendingIntent "Añade marcadores" --> Acción de la notificación
        Intent i2 = new Intent(this, PuntosInteresActivity.class);     // Crea una nueva actividad 'PuntosInteresActivity'
        i2.putExtra("usuario", usuario);
        i2.putExtra("servicio", "true");
        i2.putExtra("notification_id", 2);
        PendingIntent pendingIntentMarcadores = PendingIntent.getActivity(this, 0, i2, PendingIntent.FLAG_UPDATE_CURRENT);

        // PendingIntent "Buscar amigos" --> Acción de la notificación
        Intent i3 = new Intent(this, AnadirAmigoActivity.class);     // Crea una nueva actividad 'AnadirAmigoActivity'
        i3.putExtra("usuario", usuario);
        i3.putExtra("servicio", "true");
        i3.putExtra("notification_id", 2);
        PendingIntent pendingIntentAmigos = PendingIntent.getActivity(this, 0, i3, PendingIntent.FLAG_UPDATE_CURRENT);

        // PendingIntent para enviar un aviso broadcast a NotificationDismissedReceiver cuando el usuario elimine la notificación (arrastrar y eliminar)
        Intent i4 = new Intent(this, NotificationDismissedReceiver.class);
        PendingIntent pendingIntentDismiss = PendingIntent.getBroadcast(this.getApplicationContext(), 0, i4, 0);

        // Se definen las características de la notificación
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.Hola) + " " + usuario + "!!! " + getString(R.string.MantenteActivo))
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setAutoCancel(true)        // La notificación desaparece al pulsar en ella
                .addAction(android.R.drawable.ic_search_category_default,getString(R.string.BuscarAmigosNot), pendingIntentAmigos)      // Añade la acción 'Buscar amigos'
                .addAction(android.R.drawable.ic_dialog_map,getString(R.string.MarcadoresNot), pendingIntentMarcadores)            // Añade la acción 'Añade marcadores'
                .setContentIntent(pendingIntent)            // Añade la acción que abre la actividad MainActivity (al pulsar en el cuerpo de la notificación)
                .setDeleteIntent(pendingIntentDismiss);     // Añade el aviso broadcast cuando el usuario elimine la notificación


        // Lanzar la notificación
        Notification notification = builder.build();
        startForeground(1, notification);
        stopForeground(true);
        manager.notify(2, notification);


        // Reproducir fichero de audio
        mediaPlayer = MediaPlayer.create(this, R.raw.notificacion);
        mediaPlayer.start();

        return START_NOT_STICKY;        // Si el servicio se detiene, no se reinicia
    }

    // Método que se ejecuta cada vez que se hace 'stopService'
    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();     // Se para el reproductor de audio
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
