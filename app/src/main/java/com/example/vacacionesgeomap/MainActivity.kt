package com.example.vacacionesgeomap

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.time.LocalDateTime


enum class Pantalla {
    FORM,
    CAMARA
}


class AppMap : ViewModel(){
    val latitud = mutableStateOf(0.0)
    val longitud =  mutableStateOf(0.0)

    var permisosUbicacionOk:() -> Unit = {}
}

class AppVM : ViewModel() {
    val pantallaActual = mutableStateOf(Pantalla.FORM)
    var onPermisoCamaraOk: () -> Unit = {}
}

class FormRegistroVM : ViewModel() {
    val nombre = mutableStateOf("")
    val fotos = mutableStateListOf<Uri>() // Lista para almacenar las fotos capturadas
    val nombresLugares = mutableStateListOf<String>() // Lista para almacenar los nombres de lugares
}

class MainActivity : ComponentActivity() {

    val camaraVM: AppVM by viewModels()

    lateinit var cameraController: LifecycleCameraController

    val lanzadorPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[android.Manifest.permission.CAMERA] ?: false) {
            // Aca ejecuto lo que quiera hacer con la camara
            camaraVM.onPermisoCamaraOk()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        setContent {
            AppUI(lanzadorPermisos, cameraController)

        }
    }
}

@Composable
fun AppUI(
    lanzadorPermisos: ActivityResultLauncher<Array<String>>,
    cameraController: LifecycleCameraController
) {
    val appVM: AppVM = viewModel()

    when (appVM.pantallaActual.value) {
        Pantalla.FORM -> {
            PantallaFormUI()
        }
        Pantalla.CAMARA -> {
            PantallaCamaraUI(lanzadorPermisos, cameraController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormUI() {
    val appVM: AppVM = viewModel()
    val formRegistroVM: FormRegistroVM = viewModel()

    // Variable para almacenar el nombre del lugar visitado
    var nombreLugar by remember { mutableStateOf("") }

    // Variable para almacenar la URI de la imagen seleccionada
    var imagenSeleccionadaUri by remember { mutableStateOf<Uri?>(null) }

    // Función para mostrar la imagen en pantalla completa
    fun mostrarImagenCompleta(uri: Uri) {
        imagenSeleccionadaUri = uri
    }

    // Función para cerrar la vista de imagen completa
    fun cerrarImagenCompleta() {
        imagenSeleccionadaUri = null
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Campo de texto para el nombre del lugar visitado
        TextField(
            value = nombreLugar,
            onValueChange = {
                nombreLugar = it
                formRegistroVM.nombre.value = it
            },
            label = { Text("Nombre del lugar visitado") },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Espacio entre el campo de texto y las miniaturas de fotos
        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar las miniaturas de fotos
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(formRegistroVM.fotos) { index, fotoUri ->
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Cambia el evento onClick para mostrar la imagen en pantalla completa
                        Image(
                            painter = rememberImagePainter(data = fotoUri),
                            contentDescription = "Miniatura de foto",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(4.dp)
                                .clickable {
                                    formRegistroVM.nombresLugares.getOrNull(index)
                                        ?: "Lugar desconocido"
                                    mostrarImagenCompleta(fotoUri)
                                }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formRegistroVM.nombresLugares.getOrNull(index) ?: "Lugar desconocido",
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Botón para tomar una foto
        Button(
            onClick = {
                appVM.pantallaActual.value = Pantalla.CAMARA
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text("Tomar Foto")
        }

        // Mostrar la imagen en pantalla completa usando un AlertDialog
        imagenSeleccionadaUri?.let { uri ->
            AlertDialog(
                onDismissRequest = { cerrarImagenCompleta() },
                title = { Text("Imagen Completa") },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberImagePainter(data = uri),
                            contentDescription = "Imagen en pantalla completa",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { cerrarImagenCompleta() },
                    ) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}


fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime
    .now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)


fun crearArchivoImagenPrivada(contexto: Context):File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${generarNombreSegunFechaHastaSegundo()}.jpg"
)


fun capturarFotografia(
    cameraController: LifecycleCameraController,
    archivo: File,
    contexto: Context,
    onImagenGuardada: (uri:Uri) -> Unit
){

    val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(
        opciones,
        ContextCompat.getMainExecutor(contexto),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let {
                    onImagenGuardada(it)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Capturar Fotografia::OnImageSavedCallBack::onError", exception.message?:"Error")
            }

        }
    )
}

@Composable
fun PantallaCamaraUI(
    lanzadorPermisos: ActivityResultLauncher<Array<String>>,
    cameraController: LifecycleCameraController
){

    val contexto = LocalContext.current
    val formRegistroVm: FormRegistroVM = viewModel()
    val appVM: AppVM = viewModel()

    lanzadorPermisos.launch(arrayOf(android.Manifest.permission.CAMERA))
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PreviewView(it).apply {
                controller = cameraController

            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = {
                PreviewView(it).apply {
                    controller = cameraController
                }
            }
        )

        Button(
            onClick = {
                // Captura la foto y asocia el nombre del lugar antes de volver a la pantalla anterior
                val nombreLugar = formRegistroVm.nombre.value
                capturarFotografia(
                    cameraController,
                    crearArchivoImagenPrivada(contexto),
                    contexto,
                ) { uri ->
                    formRegistroVm.fotos.add(uri) // Agregar la URI de la foto
                    formRegistroVm.nombresLugares.add(nombreLugar) // Agregar el nombre del lugar
                    appVM.pantallaActual.value = Pantalla.FORM
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        ) {
            Text("Capturar Foto")
        }
    }

    class FaltaPermisosException(mensaje:String):Exception(mensaje)

    fun conseguirUbicacion(contexto:Context, onSuccess:(ubicacion: Location) -> Unit) {
        try {
            val servicio = LocationServices.getFusedLocationProviderClient(contexto)
            val tarea = servicio.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            tarea.addOnSuccessListener {ubicacion ->
                onSuccess(ubicacion)
            }
        }catch (se:SecurityException){
            throw FaltaPermisosException("Sin permisoso de ubicacion")
        }
    }
}





