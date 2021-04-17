package com.example.entrega2.Widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.example.entrega2.Actividades.PuntosInteresActivity;
import com.example.entrega2.Adaptadores.AdaptadorRecyclerMisFotos;
import com.example.entrega2.R;
import com.example.entrega2.Workers.GetFotosUsuarioWorker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WidgetConfigureActivity WidgetConfigureActivity}
 */
public class Widget extends AppWidgetProvider {

    private static final String PREFS_NAME = "com.example.entrega2.Widgets.Widget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String usuario = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.textViewUsuarioW, usuario);

        // Poner una foto aleatoria
        fotoAleatoria(context, usuario, views);

        // Boton marcadores
        Intent intent = new Intent(context, PuntosInteresActivity.class);
        intent.putExtra("usuario", usuario);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                1004, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.buttonMarcadores, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
            prefs.remove(PREF_PREFIX_KEY + appWidgetId);
            prefs.apply();
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static void fotoAleatoria(Context context, String usuario, RemoteViews views) {
        // Obtener las fotos
        Data datos = new Data.Builder()
                .putString("username", usuario)
                .build();
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GetFotosUsuarioWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();

        /*
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(otwr.getId())
                .observe(context, status -> {
                    if (status != null && status.getState().isFinished()) {
                        String result = status.getOutputData().getString("datos");
                        try {
                            JSONArray jsonArray = new JSONArray(result);

                            if(jsonArray.length() == 0) {
                                Toast.makeText(context, context.getString(R.string.NoFotosSubidas), Toast.LENGTH_SHORT).show();
                            }
                            else{
                                ArrayList<String> fotos = new ArrayList<>();
                                for(int i=0; i<jsonArray.length(); i++) {
                                    JSONObject foto = jsonArray.getJSONObject(i);
                                    fotos.add(foto.getString("id"));
                                }

                                Random randomGenerator = new Random();
                                int indexFoto = randomGenerator.nextInt(fotos.size());
                                String foto = fotos.get(indexFoto);

                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                StorageReference pathReference = storageRef.child(foto);
                                pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        try {
                                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                                            views.setImageViewBitmap(R.id.imageViewFotoW, bitmap);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        //Glide.with(context).load(uri).into();
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        WorkManager.getInstance(context).enqueue(otwr);
        */

    }
}