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
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ServicioFirebase extends FirebaseMessagingService {

    private String from;
    private String to;

    public ServicioFirebase(){}

    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            from = remoteMessage.getData().get("fromUser");
            to = remoteMessage.getData().get("toUser");
        }

        if (remoteMessage.getNotification() != null) {
            NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "Solicitudes");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel elCanal = new NotificationChannel("Solicitudes", "CanalSolicitudes",
                        NotificationManager.IMPORTANCE_DEFAULT);
                elCanal.setDescription("Canal solicitudes de amistad");
                elCanal.enableLights(true);
                elCanal.setLightColor(Color.RED);
                elCanal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                elCanal.enableVibration(true);
                elManager.createNotificationChannel(elCanal);
            }

            if(remoteMessage.getNotification().getClickAction().equals("SOLICITUD")) {
                Intent i = new Intent(this, AnadirAmigoActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.putExtra("usuario", to);
                PendingIntent intentEnNot = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

                elBuilder.setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle(getString(R.string.SolicitudRecibida))
                        .setContentText(getString(R.string.Hola) + " " + to + "!!! " + from + " " + getString(R.string.QuiereAmigo))
                        .setSubText(getString(R.string.app_name))
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true)
                        .setContentIntent(intentEnNot);
            }

            elManager.notify(1, elBuilder.build());

        }

    }

}
