package com.example.entrega2.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.entrega2.ServicioMusicaNotificacion;

public class NotificationDismissedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, ServicioMusicaNotificacion.class);
        context.stopService(i);
    }
}