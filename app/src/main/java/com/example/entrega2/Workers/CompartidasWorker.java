package com.example.entrega2.Workers;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class CompartidasWorker extends Worker {

    public CompartidasWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String funcion = getInputData().getString("funcion");

            // COMPARTIR FOTO
            if("compartir".equals(funcion)){
                String usuario = getInputData().getString("usuario");
                String imagen = getInputData().getString("imagen");
                String[] amigos = getInputData().getStringArray("amigos");
                String titulo = getInputData().getString("titulo");

                String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/compartirFoto.php";
                HttpURLConnection urlConnection = null;
                URL destino = new URL(direccion);
                urlConnection = (HttpURLConnection) destino.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");

                JSONArray amigosArray = new JSONArray();
                for(int i=0; i<amigos.length; i++) {
                    amigosArray.put(amigos[i]);
                }

                JSONObject parametrosJSON = new JSONObject();
                parametrosJSON.put("usuario", usuario);
                parametrosJSON.put("imagen", imagen);
                parametrosJSON.put("amigos", amigosArray);
                parametrosJSON.put("titulo", titulo);

                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametrosJSON.toString());
                out.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    return Result.success();
                }
            }

            // ELIMINAR COMPARTIDA
            else {
                String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/ivaldelvira001/WEB/entrega2/compartidas.php";
                HttpURLConnection urlConnection = null;
                URL destino = new URL(direccion);
                urlConnection = (HttpURLConnection) destino.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                if("eliminar".equals(funcion)){
                    String usuario = getInputData().getString("usuario");
                    String imagen = getInputData().getString("imagen");
                    String amigo = getInputData().getString("amigo");

                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("funcion", funcion)
                            .appendQueryParameter("usuario", usuario)
                            .appendQueryParameter("imagen", imagen)
                            .appendQueryParameter("amigo", amigo);
                    String parametros = builder.build().getEncodedQuery();

                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                    out.print(parametros);
                    out.close();

                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == 200) {
                        return Result.success();
                    }
                }

                // GET FOTOS COMPARTIDAS
                else if("getCompartidas".equals(funcion)){
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

                // ENVIAR COMENTARIO
                else if("enviarComentario".equals(funcion)){
                    String from = getInputData().getString("from");
                    String to = getInputData().getString("to");
                    String titulo = getInputData().getString("titulo");
                    String comentario = getInputData().getString("comentario");

                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("funcion", funcion)
                            .appendQueryParameter("from", from)
                            .appendQueryParameter("to", to)
                            .appendQueryParameter("titulo", titulo)
                            .appendQueryParameter("comentario", comentario);
                    String parametros = builder.build().getEncodedQuery();

                    PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                    out.print(parametros);
                    out.close();

                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == 200) {
                        return Result.success();
                    }
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
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return Result.failure();
    }
}
