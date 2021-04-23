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

// Tarea encargada de conectarse con un servicio web (PHP) para realizar operaciones relacionadas con la tabla 'Amigos' de la base de datos remota de la aplicación
public class AmigosWorker extends Worker {

    public AmigosWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    // Método donde se realiza la tarea
    @NonNull
    @Override
    public Result doWork() {
        try {
            String funcion = getInputData().getString("funcion");   // Se recupera de la información enviada a la tarea la operación que debe realizar en la base de datos

            // Se genera un objeto HttpURLConnection para conectarse con el servicio PHP 'amigos.php' en el servidor
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/amigos.php";
            HttpURLConnection urlConnection = null;
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            urlConnection.setRequestMethod("POST");     // Se usa el método de petición POST
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");      // Los parametros se codifican en pares claves-valor

            // Si la operación es ELIMINAR UN AMIGO
            if("eliminar".equals(funcion)){
                // Se recupera la información enviada a la tarea
                String user = getInputData().getString("user");
                String friend = getInputData().getString("friend");

                // Parametros a enviar al fichero PHP en formato pares clave-valor (en la URI)
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("user", user)
                        .appendQueryParameter("friend", friend);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se ejecuta la llamada al servicio web
                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    // Si la respuesta del servidor es 200 OK, se devuelve 'Result.success'
                    return Result.success();
                }
            }

            // Si la operación es OBTENER LOS AMIGOS A LOS QUE COMPARTIR UNA FOTO
            else if("getAmigosCompartir".equals(funcion)){
                // Se recupera la información enviada a la tarea
                String usuario = getInputData().getString("usuario");
                String imagen = getInputData().getString("imagen");

                // Parametros a enviar al fichero PHP en formato pares clave-valor (en la URI)
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("usuario", usuario)
                        .appendQueryParameter("imagen", imagen);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se ejecuta la llamada al servicio web
                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    // Si la respuesta del servidor es 200 OK, se leen los datos de la respuesta HTTP
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line, result = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                    // Se añaden los datos de la respuesta HTTP al resultado de la tarea
                    Data resultados = new Data.Builder()
                            .putString("datos",result)
                            .build();

                    // Se devuelve 'Result.success' con los datos
                    return Result.success(resultados);
                }
            }

            // Si la operación es OBTENER LOS AMIGOS DE UN USUARIO
            else if("getAmigos".equals(funcion)){
                // Se recupera la información enviada a la tarea
                String username = getInputData().getString("username");

                // Parametros a enviar al fichero PHP en formato pares clave-valor (en la URI)
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("username", username);
                String parametros = builder.build().getEncodedQuery();

                // Se incluyen los parámetros en la petición HTTP
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                // Se ejecuta la llamada al servicio web
                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    // Si la respuesta del servidor es 200 OK, se leen los datos de la respuesta HTTP
                    BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    String line, result = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        result += line;
                    }
                    inputStream.close();

                    // Se añaden los datos de la respuesta HTTP al resultado de la tarea
                    Data resultados = new Data.Builder()
                            .putString("datos",result)
                            .build();

                    // Se devuelve 'Result.success' con los datos
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

        // Si la respuesta del servidor no ha sido 200 OK, se envía un resultado indicando que algo ha fallado en la ejecución
        return Result.failure();
    }
}
