package com.example.entrega2.Workers;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class EliminarMarcadorWorker extends Worker {

    public EliminarMarcadorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String username = getInputData().getString("username");
        Double latActual = getInputData().getDouble("lat", 0);
        Double longActual = getInputData().getDouble("long", 0);

        String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/eliminarMarcador.php";
        HttpURLConnection urlConnection = null;
        try {
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("username", username)
                    .appendQueryParameter("lat", latActual.toString())
                    .appendQueryParameter("long", longActual.toString());
            String parametros = builder.build().getEncodedQuery();

            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametros);
            out.close();

            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200) {
                return Result.success();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Result.failure();
    }
}