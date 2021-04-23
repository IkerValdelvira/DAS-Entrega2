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

// Tarea encargada de conectarse con un servicio web (PHP) para realizar operaciones relacionadas con la tabla 'Solicitudes' de la base de datos remota de la aplicación (y funciones FCM)
public class SolicitudesWorker extends Worker {

    public SolicitudesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    // Método donde se realiza la tarea
    @NonNull
    @Override
    public Result doWork() {
        try {
            String funcion = getInputData().getString("funcion");       // Se recupera de la información enviada a la tarea la operación que debe realizar en la base de datos (o función FCM)

            // Se genera un objeto HttpURLConnection para conectarse con el servicio PHP 'compartidas.php' en el servidor
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/solicitudes.php";
            HttpURLConnection urlConnection = null;
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            urlConnection.setRequestMethod("POST");             // Se usa el método de petición POST
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");      // Los parametros se codifican en pares claves-valor

            // Si la operación es OBTENER LAS SOLICITUDES ENVIADAS A UN USUARIO
            if("buscar".equals(funcion)){
                // Se recupera la información enviada a la tarea
                String toUser = getInputData().getString("toUser");

                // Parametros a enviar al fichero PHP en formato pares clave-valor (en la URI)
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("toUser", toUser);
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
                            .putString("datos", result)
                            .build();

                    // Se devuelve 'Result.success' con los datos
                    return Result.success(resultados);
                }
            }

            // Si la operación es ENVIAR UN SOLICITUD A UN USUARIO (añadir solicitud a la tabla 'Solicitudes' y mandar datos+notificación con FCM)
            else if("enviar".equals(funcion)){
                // Se recupera la información enviada a la tarea
                String from = getInputData().getString("from");
                String to = getInputData().getString("to");

                // Parametros a enviar al fichero PHP en formato pares clave-valor (en la URI)
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("from", from)
                        .appendQueryParameter("to", to);
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

            // Si la operación es ACEPTAR (eliminar solicitud y añadir amigo) O RECHAZAR (eliminar solicitud) UNA SOLICITUD ENVIADA A UN USUARIO
            else if("gestionar".equals(funcion)){
                // Se recupera la información enviada a la tarea
                String user = getInputData().getString("user");
                String friend = getInputData().getString("friend");
                String status = getInputData().getString("status");

                // Parametros a enviar al fichero PHP en formato pares clave-valor (en la URI)
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("funcion", funcion)
                        .appendQueryParameter("user", user)
                        .appendQueryParameter("friend", friend)
                        .appendQueryParameter("status", status);
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
