package com.example.entrega2.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.entrega2.ServicioMusicaNotificacion;

// BroadcastReceiver que se utiliza para recibir el mensaje broadcast lanzado al eliminar la noticificación (cuando el usuario arrastra la notificación y la elimina) creada en el servico ServicioMusicaNotificacion
public class NotificationDismissedReceiver extends BroadcastReceiver {

    // Se ejecuta al recibir un aviso de mensaje de broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        // Se para el servicio ServicioMusicaNotificacion
        Intent i = new Intent(context, ServicioMusicaNotificacion.class);
        context.stopService(i);
    }
}