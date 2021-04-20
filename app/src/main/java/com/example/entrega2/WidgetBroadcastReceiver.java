package com.example.entrega2;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.entrega2.Widgets.MonumentosWidget;

public class WidgetBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.monumentos_widget);

        String monumento = MonumentosWidget.getMonumento(context);
        String nombreMonumento = monumento.split(" --> ")[0];
        remoteViews.setTextViewText(R.id.textViewMonumentoW, nombreMonumento);

        ComponentName tipowidget = new ComponentName(context, MonumentosWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(tipowidget, remoteViews);
    }
}