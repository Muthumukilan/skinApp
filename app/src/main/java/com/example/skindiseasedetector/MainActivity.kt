package com.example.skindiseasedetector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var needCAmera: MutableState<Boolean> = mutableStateOf(false)

    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)
    private var dission: MutableState<Boolean> = mutableStateOf(false)
    private var photoTaken: MutableState<Boolean> = mutableStateOf(false)
    private var photoUri: Uri? = null
    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("kilo", "Permission granted")
            shouldShowCamera.value = true
        } else {
            Log.i("kilo", "Permission denied")
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("kilo", "Permission previously granted")
                shouldShowCamera.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("kilo", "Show camera permissions dialog")

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }



    private fun handleImageCapture(uri: Uri) {
        Log.i("kilo", "Image captured: $uri")
        shouldShowCamera.value = false

        photoUri = uri
        shouldShowPhoto.value = true
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if(shouldShowPhoto.value)
                needCAmera.value=false
            if(!needCAmera.value) {

                needCAmera.value = bottomNavBar(photoUri)
                photoTaken.value=false
            }
            else if (!photoTaken.value && needCAmera.value && !shouldShowPhoto.value){
                photoTaken.value=CameraView(
                    outputDirectory = outputDirectory,
                    executor = cameraExecutor,
                    onImageCaptured = ::handleImageCapture,
                    onError = { Log.e("kilo", "View error:", it) }
                )
            }
            //in
//            if(needCAmera.value==false){
//                MainContent(newImage = false)
//            }
//            if (shouldShowCamera.value && needCAmera.value) {
//                CameraView(
//                    outputDirectory = outputDirectory,
//                    executor = cameraExecutor,
//                    onImageCaptured = ::handleImageCapture,
//                    onError = { Log.e("kilo", "View error:", it) }
//                )
//            }
        }



        requestCameraPermission()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

}

@Composable
fun LazyColumnWithCardsAndDropdown(items1: List<String>,extraInfo: List<String>) {
    Spacer(modifier = Modifier.height(25.dp))
    LazyColumn {
        itemsIndexed(items1) {index,item ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable(onClick = { expanded = !expanded }),

                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = item , fontSize = 20.sp )
                    Spacer(modifier = Modifier.height(1.dp))
                    if (expanded) {
                        Text(
                            text = extraInfo.getOrNull(index) ?: "",
                            color = Color.Gray,fontSize = 17.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            modifier = Modifier
                                .size(24.dp)
                                .padding(4.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun infoCompos() {
    val items = listOf("Basal Cell Carcinoma", "Dermatofibroma", "Melanoma","Actinic Keratoses","Benign keratosis","Melanocytic nevi","Vascular skin lesions")
    val extraInfo = listOf(" Basal Cell Carcinoma is the most common type of skin cancer, typically developing on areas exposed to the sun. It often appears as a waxy bump or a scar-like area and rarely metastasizes but can cause local tissue destruction if left untreated.",
        "Dermatofibroma is a benign skin lesion that typically presents as a firm, round, or oval nodule on the lower extremities. It is usually painless and may have a dimpled or depressed center when pinched.",
        "Melanoma is a type of skin cancer that develops from melanocytes, the pigment-producing cells of the skin. It can arise from existing moles or appear as new pigmented growths. Melanoma is known for its potential to metastasize and can be life-threatening if not diagnosed and treated early.",
        "Actinic keratoses are precancerous growths caused by prolonged exposure to ultraviolet radiation from the sun. They typically appear as rough, scaly patches on sun-exposed areas such as the face, scalp, ears, and hands. If left untreated, actinic keratoses may develop into squamous cell carcinoma.",
        "Benign keratosis, also known as seborrheic keratosis, is a common non-cancerous skin growth. It usually appears as a waxy, stuck-on lesion with a slightly elevated surface. Benign keratoses can vary in color from light tan to black and often increase in number with age.",
        "Melanocytic nevi, commonly known as moles, are benign skin growths composed of melanocytes. They can vary in size, shape, and color and may appear anywhere on the body. While most moles are harmless, changes in size, shape, or color should be evaluated by a dermatologist as they may indicate melanoma.",
        " Vascular skin lesions are abnormalities in blood vessels that can manifest as various types of skin discolorations or growths. Examples include hemangiomas, port-wine stains, and cherry angiomas. Treatment options for vascular skin lesions depend on the specific type and may include laser therapy or surgical excision.")

    LazyColumnWithCardsAndDropdown(items,extraInfo)
}


//display image
@Composable
fun displayImage(photoUri:Uri){
    var painter:Painter=rememberImagePainter(photoUri)
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}


//main content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(newImage: Boolean, image: Uri?,select:Int): Int{
    var camerarequest by rememberSaveable {
        mutableStateOf(0)
    }
    var isImage by rememberSaveable {
        if (image == null) {
            mutableStateOf(false)
        }
        else{
            mutableStateOf(true)
        }
    }
    val context = LocalContext.current
    var name by remember {
        mutableStateOf("")
    }
    var age by remember {
        mutableStateOf("")
    }
    val coroutineScope = rememberCoroutineScope()
    var loadingState by remember {
        mutableStateOf(false)
    }
    var outputSelecter by remember {
        mutableStateOf(false)
    }
    var payload by remember {
        mutableStateOf("")
    }
    var outputContent by remember {
        mutableStateOf("")
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(!loadingState && !outputSelecter) {
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 400.dp)
                    .fillMaxSize()
            ) {
                if (isImage == false) {
                    // Image
                    Image(
                        painter = painterResource(id = R.drawable.photo), // Replace with your image resource
                        contentDescription = "Image", modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (image != null && select == 0) {
                        displayImage(photoUri = image)
                    }
                }
            }


            // Button
            Button(onClick = {
                if (select == 0) {
                    camerarequest = 1
                }
            }

            ) {
                Text(text = "take picture")
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Text fields
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") })
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") })

            Spacer(modifier = Modifier.height(10.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // Disabled Button
            Button(onClick = {
                loadingState = true
                payload= convertImageToByteArrayAndString(context = context, imageUri = image)
                coroutineScope.launch(Dispatchers.IO) {
                    sendRequest(payload)
                    delay(10000)
                    outputContent=sendPostRequest("","")
                    loadingState = false
                    outputSelecter=true
                }
            }, enabled = (isImage && age != "" && name != "")) {
                Text(text = "submit Button")
            }
        }
        else if(outputSelecter){
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 400.dp)
                    .fillMaxSize()
            ) {
                if (image != null && select == 0) {
                    displayImage(photoUri = image)
                }
            }
            Output(outputContent)
            Button(onClick = {outputSelecter=false}) {
                Text(text = "<")
            }
        }
        else{
            LoadingScreen()
        }
    }
    return camerarequest
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContentGallery(){
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var name by remember {
        mutableStateOf("")
    }
    var age by remember {
        mutableStateOf("")
    }
    val coroutineScope = rememberCoroutineScope()
    var loadingState by remember {
        mutableStateOf(false)
    }
    var outputSelecter by remember {
        mutableStateOf(false)
    }
    var payload by remember {
        mutableStateOf("")
    }
    var outputContent by remember {
        mutableStateOf("")
    }

    //gallery
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            imageUri = uri
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 1.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(!loadingState && !outputSelecter) {
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 400.dp)
                    .fillMaxSize()
            ) {
                if (imageUri == null) {
                    // Image
                    Image(
                        painter = painterResource(id = R.drawable.photo), // Replace with your image resource
                        contentDescription = "Image", modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (imageUri != null) {
                        displayImage(photoUri = imageUri!!)
                    }
                }
            }
            // Button
            Button(onClick = {
                    launcher.launch("image/*")
            }
            ) {
                Text(text = "take picture")
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Text fields
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") })
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") })

            Spacer(modifier = Modifier.height(10.dp))
            Spacer(modifier = Modifier.height(10.dp))

            // Disabled Button
            Button(onClick = {
                loadingState = true
                payload= convertImageToByteArrayAndString(context = context, imageUri = imageUri)
                coroutineScope.launch(Dispatchers.IO) {
                    sendRequest(payload)
                    delay(10000)
                    outputContent=sendPostRequest("","")
                    loadingState = false
                    outputSelecter=true
                }
            }, enabled = (age != "" && name != "" && imageUri!=null)) {
                Text(text = "submit Button")
            }
        }
        else if(outputSelecter){
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 250.dp)
                    .fillMaxSize()
            ) {
                if (imageUri != null) {
                    displayImage(photoUri = imageUri!!)
                }
            }
            Output(outputContent)
            Button(onClick = {outputSelecter=false}) {
                Text(text = "<")
            }
        }
        else{
            LoadingScreen()
        }
    }
}

//Loading screen
@Composable
fun LoadingScreen() {
    Surface(color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Processing...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

//bottom navbar
data class NavItemState(
    val title: String,
    val selectedState: Int,
    val unSelectedState: Int,

    )

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun bottomNavBar(image: Uri?):Boolean {
    //selection of cameara of gallary or info
    var select by rememberSaveable {
        mutableStateOf(0)
    }
    //
    var intDes by rememberSaveable {
        mutableStateOf(0)
    }
    //navigation
    var stateNav by rememberSaveable {
        mutableStateOf(0)
    }
    //
    var dission by rememberSaveable {
        mutableStateOf(false)
    }
    var galaryRequest by rememberSaveable {
        mutableStateOf(false)
    }
    var image1: Uri? by rememberSaveable {
        mutableStateOf(image)
    }

    val items = listOf(
        NavItemState(
            title = "Camera",
            selectedState = R.drawable.camera,
            unSelectedState = R.drawable.camera_enhance
        ),
        NavItemState(
            title = "Galary",
            selectedState = R.drawable.image,
            unSelectedState = R.drawable.image_search
        ),
        NavItemState(
            title = "info",
            selectedState = R.drawable.info,
            unSelectedState = R.drawable.info
        )
    )
    Scaffold(bottomBar = {
        NavigationBar {
            items.forEachIndexed { index, item ->
                NavigationBarItem(selected = stateNav == index,
                    onClick = {
                        stateNav = index
                        if (item.title == "Camera") select = 0
                        else if (item.title == "Galary") select = 1
                        else select = 2
                        /*DO NOthing*/
                    },
                    icon = {
                        if (stateNav == index) Icon(
                            painter = painterResource(id = item.selectedState),
                            contentDescription = "image_search", modifier = Modifier.size(24.dp)
                        )
                        else Icon(
                            painter = painterResource(id = item.unSelectedState),
                            contentDescription = "image_search", modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
        }
    }
    ) {
        if (select == 0) {
            intDes = MainContent(false, image = image1, select = select)
        } else if (select == 1) {
            MainContentGallery()
        } else if (select == 2) {
            //Output()
            infoCompos()
        }
    }
    if (intDes == 1) {
        dission = true
    }
    return dission
}


@Composable
fun Output(outputContent:String) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val jsonObject = JSONObject(outputContent)
        val bodyObject = jsonObject.getJSONObject("body")

        val predict = bodyObject.getString("predict")
        val confidence = bodyObject.getDouble("confidence")
        val top3Object = bodyObject.getJSONObject("top3")
        val conceptObject = bodyObject.getJSONObject("concept")
        Text("Predict: $predict")
        Text("Confidence: ${String.format("%.4f", confidence.toDouble())}")
        Text("Top 3:")
        top3Object.keys().forEach { key ->
            Text("$key: ${String.format("%.4f", top3Object.getDouble(key))}")
        }
        Text("Concept:")
        conceptObject.keys().forEach { key ->
            Text("$key: ${String.format("%.4f", conceptObject.getDouble(key))}")
        }
    }
}


fun convertImageToByteArrayAndString(context: Context, imageUri: Uri?): String {
    val inputStream: InputStream? = context.contentResolver.openInputStream(Uri.parse(imageUri.toString()))
    inputStream?.use { stream ->
        val bitmap = BitmapFactory.decodeStream(stream)
        if (bitmap != null) {
            val byteArray = bitmap.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }
    return ""
}

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}


suspend fun sendRequest(image:String) {
    val url = URL("https://o0pq9342xj.execute-api.ap-south-1.amazonaws.com/stag1")
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        // Assuming jsonObject is defined elsewhere
        val jsonObject = JSONObject()
        jsonObject.put("key1", image)

        val requestBody = jsonObject.toString()

        val outputStream = connection.outputStream
        outputStream.write(requestBody.toByteArray())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
            val response = inputStream.use(BufferedReader::readText)
            inputStream.close()
        }
    } finally {
        connection.disconnect()
    }
}


suspend fun sendPostRequest(textToSend: String, textToSend1: String): String {
    val url = URL("https://w3lb7akpuh.execute-api.ap-south-1.amazonaws.com/stage1")
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        // Assuming jsonObject is defined elsewhere
        val jsonObject = JSONObject()
        jsonObject.put("key1", textToSend)
        jsonObject.put("key2", textToSend1)
        val requestBody = jsonObject.toString()

        val outputStream = connection.outputStream
        outputStream.write(requestBody.toByteArray())
        outputStream.flush()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
            val response = inputStream.use(BufferedReader::readText)
            inputStream.close()

            return response
        } else {
            return "Error: ${connection.responseMessage}"
        }
    } finally {
        connection.disconnect()
    }
}
