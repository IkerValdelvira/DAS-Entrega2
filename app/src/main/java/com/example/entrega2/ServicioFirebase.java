package com.example.entrega2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.entrega2.Actividades.AnadirAmigoActivity;
import com.example.entrega2.Actividades.CompartidasActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

// Servicio encargado de recibir los mensajes FCM (Firebase Cloud Messaging)
public class ServicioFirebase extends FirebaseMessagingService {

    private String from;
    private String to;
    private String titulo;
    private String comentario;

    public ServicioFirebase(){}

    // Método que se ejecuta al recibir un mensaje FCM cuando la aplicación está en ejecución (no se ejecuta si está en background)
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Si el mensaje FCM viene con datos, se recogen y se guardan en las variables
        if (remoteMessage.getData().size() > 0) {
            from = remoteMessage.getData().get("fromUser");
            to = remoteMessage.getData().get("toUser");
            titulo = remoteMessage.getData().get("titulo");
            comentario = remoteMessage.getData().get("comentario");
        }

        // Si el mensaje FCM es una notificación
        if (remoteMessage.getNotification() != null) {
            // Creación del canal de notificaciones
            NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "Notificaciones");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel elCanal = new NotificationChannel("Notificaciones", "CanalNotificaciones",
                        NotificationManager.IMPORTANCE_DEFAULT);
                elCanal.setDescription("Canal de notificaciones");
                elCanal.enableLights(true);
                elCanal.setLightColor(Color.RED);
                elCanal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                elCanal.enableVibration(true);
                elManager.createNotificationChannel(elCanal);
            }

            // Si la acción de la notificación FCM es 'SOLICITUD', se crea una notificación que abrirá la actividad 'AnadirAmigoActivity' con los datos recibidos
            if(remoteMessage.getNotification().getClickAction().equals("SOLICITUD")) {
                Intent i = new Intent(this, AnadirAmigoActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.putExtra("usuario", to);
                i.putExtra("solicitudes", true);
                PendingIntent intentEnNot = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

                elBuilder.setSmallIcon(android.R.drawable.ic_input_add)
                        .setContentTitle(getString(R.string.SolicitudRecibida))
                        .setContentText(getString(R.string.Hola) + " " + to + "!!! " + from + " " + getString(R.string.QuiereAmigo))
                        .setSubText(getString(R.string.Solicitud))
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true)
                        .setContentIntent(intentEnNot);
            }

            // Si la acción de la notificación FCM es 'COMENTARIO', se crea una notificación mostrando los datos recibidos
            else if(remoteMessage.getNotification().getClickAction().equals("COMENTARIO")) {
                elBuilder.setSmallIcon(android.R.drawable.ic_menu_send)
                        .setContentTitle(to + ", " + from + " " + getString(R.string.HaComentado) + " " + titulo)
                        .setContentText(comentario)
                        .setSubText(getString(R.string.Comentario))
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true);
            }

            // Si la acción de la notificación FCM es 'COMPARTIDA', se crea una notificación que abrirá la actividad 'CompartidasActivity' con los datos recibidos
            else if(remoteMessage.getNotification().getClickAction().equals("COMPARTIDA")) {
                Intent i = new Intent(this, CompartidasActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.putExtra("usuario", to);
                PendingIntent intentEnNot = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

                elBuilder.setSmallIcon(android.R.drawable.ic_menu_share)
                        .setContentTitle(getString(R.string.FotoCompartidaNot))
                        .setContentText(getString(R.string.Hola) + " " + to + "!!! " + from + " " + getString(R.string.HaCompartido) + " '" + titulo + "'" + getString(R.string.TocaRevisarCompartidas))
                        .setSubText(getString(R.string.Solicitud))
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true)
                        .setContentIntent(intentEnNot);
            }

            elManager.notify(1, elBuilder.build());

        }

    }

}
