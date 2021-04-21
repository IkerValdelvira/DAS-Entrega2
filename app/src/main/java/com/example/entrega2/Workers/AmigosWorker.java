package com.example.entrega2.Workers;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class AmigosWorker extends Worker {

    public AmigosWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String funcion = getInputData().getString("funcion");

            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/amigos.php";
            HttpURLConnection urlConnection = null;
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // ELIMINAR AMIGO
            if("eliminar".equals(funcion)){
                String user = getInputData().getString("user");
                String friend = getInputData().getString("friend");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("user", user)
                        .appendQueryParameter("friend", friend);
                String parametros = builder.build().getEncodedQuery();

                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    return Result.success();
                }
            }

            // GET AMIGOS COMPARTIR
            else if("getAmigosCompartir".equals(funcion)){
                String usuario = getInputData().getString("usuario");
                String imagen = getInputData().getString("imagen");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("usuario", usuario)
                        .appendQueryParameter("imagen", imagen);
                String parametros = builder.build().getEncodedQuery();

                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line, result = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                    Data resultados = new Data.Builder()
                            .putString("datos",result)
                            .build();

                    return Result.success(resultados);
                }
            }

            // GET AMIGOS
            else if("getAmigos".equals(funcion)){
                String username = getInputData().getString("username");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("username", username);
                String parametros = builder.build().getEncodedQuery();

                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line, result = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                    Data resultados = new Data.Builder()
                            .putString("datos",result)
                            .build();

                    return Result.success(resultados);
                }
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
