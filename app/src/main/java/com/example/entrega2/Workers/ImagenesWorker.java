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

public class ImagenesWorker extends Worker {

    public ImagenesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String funcion = getInputData().getString("funcion");

            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/imagenes.php";
            HttpURLConnection urlConnection = null;
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // GET FOTOS USUARIO
            if("getFotosUsuario".equals(funcion)){
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

            // GET FOTO
            else if("getFoto".equals(funcion)){
                String username = getInputData().getString("username");
                String imagen = getInputData().getString("imagen");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("username", username)
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

            // INSERTAR FOTO
            else if("insertar".equals(funcion)){
                String usuario = getInputData().getString("usuario");
                String imagen = getInputData().getString("imagen");
                String titulo = getInputData().getString("titulo");
                String descripcion = getInputData().getString("descripcion");
                String fecha = getInputData().getString("fecha");
                String latitud = getInputData().getString("latitud");
                String longitud = getInputData().getString("longitud");
                String etiquetas = getInputData().getString("etiquetas");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("usuario", usuario)
                        .appendQueryParameter("imagen", imagen)
                        .appendQueryParameter("titulo", titulo)
                        .appendQueryParameter("descripcion", descripcion)
                        .appendQueryParameter("fecha", fecha)
                        .appendQueryParameter("latitud", latitud)
                        .appendQueryParameter("longitud", longitud)
                        .appendQueryParameter("etiquetas", etiquetas);
                String parametros = builder.build().getEncodedQuery();

                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    return Result.success();
                }
            }

            // ACTUALIZAR FOTO
            else if("actualizar".equals(funcion)){
                String usuario = getInputData().getString("usuario");
                String imagen = getInputData().getString("imagen");
                String titulo = getInputData().getString("titulo");
                String descripcion = getInputData().getString("descripcion");
                String latitud = getInputData().getString("latitud");
                String longitud = getInputData().getString("longitud");
                String etiquetas = getInputData().getString("etiquetas");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("usuario", usuario)
                        .appendQueryParameter("imagen", imagen)
                        .appendQueryParameter("titulo", titulo)
                        .appendQueryParameter("descripcion", descripcion)
                        .appendQueryParameter("latitud", latitud)
                        .appendQueryParameter("longitud", longitud)
                        .appendQueryParameter("etiquetas", etiquetas);
                String parametros = builder.build().getEncodedQuery();

                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    return Result.success();
                }
            }

            // ELIMINAR FOTO
            else if("eliminar".equals(funcion)){
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
                    return Result.success();
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