package com.example.entrega2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.entrega2.Dialogos.DialogoPermisosCamara;
import com.example.entrega2.Dialogos.DialogoPermisosLocalizacion;
import com.example.entrega2.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SubirFotoActivity extends AppCompatActivity {

    private String usuario;                         // Nombre del usuario que ha creado la actividad

    private String origenFoto;
    private ImageView imageViewFoto;

    private static int CODIGO_GALERIA = 1;
    private static int CODIGO_FOTO_ARCHIVO = 2;

    private Uri uriimagen;
    private String imageName;
    private File fichImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Acceso al las preferencias para obtener el valor de 'idioma'
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma", "es");

        // Crear nueva localización con el idioma recogido de las preferencias (necesario para mantener el idioma tras cambio de orientacion del dispositivo)
        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        setContentView(R.layout.activity_subir_foto);

        // Inicialización del nombre de usuario obtenido a través del Bundle asociado al Intent que ha creado la actividad
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            usuario = extras.getString("usuario");
            origenFoto = extras.getString("origen");
        }

        imageViewFoto = findViewById(R.id.imageViewFoto);
        imageViewFoto.setRotation(90);

        if(origenFoto.equals("galeria")) {
            Intent elIntentGal = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(elIntentGal, CODIGO_GALERIA);
        }
        else if(origenFoto.equals("camara")) {
            // Es necesario dar permisos CAMERA
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO
                    DialogFragment dialogoPermisosCamara = new DialogoPermisosCamara();
                    dialogoPermisosCamara.show(getSupportFragmentManager(), "permisos_camara");
                } else {
                    //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                    //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
                    Toast.makeText(this, getString(R.string.NoPermisoCamara), Toast.LENGTH_SHORT).show();
                }
                //PEDIR EL PERMISO
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                /*
                Intent intent = new Intent(this, SubirFotoActivity.class);
                intent.putExtra("usuario", usuario);
                intent.putExtra("origen", "camara");
                startActivity(intent);
                finish();
                */
            }
            else{
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String nombrefich = "IMG_" + timeStamp + "_";
                File directorio = this.getFilesDir();
                try {
                    fichImg = File.createTempFile(nombrefich, ".jpg",directorio);
                    uriimagen = FileProvider.getUriForFile(this, "com.example.entrega2.provider", fichImg);
                } catch (IOException e) {

                }
                Intent elIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                elIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriimagen);
                startActivityForResult(elIntent, CODIGO_FOTO_ARCHIVO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODIGO_GALERIA && resultCode == RESULT_OK) {
            uriimagen = data.getData();
            imageName = uriimagen.toString().split("%2F")[uriimagen.toString().split("%2F").length-1];
            setImage(uriimagen);
        }

        else if (requestCode == CODIGO_FOTO_ARCHIVO && resultCode == RESULT_OK) {
            imageName = uriimagen.toString().split("/")[uriimagen.toString().split("/").length-1];
            setImage(uriimagen);
        }

    }

    private void setImage(Uri uriimagen) {
        try{
            Bitmap bitmapFoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriimagen);
            int anchoDestino = imageViewFoto.getWidth();
            int altoDestino = imageViewFoto.getHeight();
            int anchoImagen = bitmapFoto.getWidth();
            int altoImagen = bitmapFoto.getHeight();
            float ratioImagen = (float) anchoImagen / (float) altoImagen;
            float ratioDestino = (float) anchoDestino / (float) altoDestino;
            int anchoFinal = anchoDestino;
            int altoFinal = altoDestino;
            if (ratioDestino > ratioImagen) {
                anchoFinal = (int) ((float)altoDestino * ratioImagen);
            } else {
                altoFinal = (int) ((float)anchoDestino / ratioImagen);
            }
            Bitmap bitmapredimensionado = Bitmap.createScaledBitmap(bitmapFoto,anchoFinal,altoFinal,true);
            imageViewFoto.setImageBitmap(bitmapredimensionado);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickSubir(View v) {

    }

    public void onClickOtraGaleria(View v) {
        Intent intent = new Intent(this, SubirFotoActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("origen", "galeria");
        startActivity(intent);
        finish();
    }

    public void onClickOtraCamara(View v) {
        Intent intent = new Intent(this, SubirFotoActivity.class);
        intent.putExtra("usuario", usuario);
        intent.putExtra("origen", "camara");
        startActivity(intent);
        finish();
    }
}