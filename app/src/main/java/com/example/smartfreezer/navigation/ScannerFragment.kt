package com.example.smartfreezer.navigation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartfreezer.ProfileActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.databinding.FragmentScannerBinding
import com.example.smartfreezer.models.Detection
import com.example.smartfreezer.models.UserProduct

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.FirebasePerformance
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer


class ScannerFragment : Fragment() {
    private lateinit var binding: FragmentScannerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var ortSession: OrtSession? = null
    private lateinit var ortEnv: OrtEnvironment

    private val labels = listOf(
        "Apple__Healthy", "Apple__Rotten", "Banana__Healthy", "Banana__Rotten",
        "Bellpepper__Healthy", "Bellpepper__Rotten", "Carrot__Healthy", "Carrot__Rotten",
        "Cucumber__Healthy", "Cucumber__Rotten", "Grape__Healthy", "Grape__Rotten",
        "Guava__Healthy", "Guava__Rotten", "Jujube__Healthy", "Jujube__Rotten",
        "Mango__Healthy", "Mango__Rotten", "Orange__Healthy", "Orange__Rotten",
        "Pomegranate__Healthy", "Pomegranate__Rotten", "Potato__Healthy", "Potato__Rotten",
        "Strawberry__Healthy", "Strawberry__Rotten", "Tomato__Healthy", "Tomato__Rotten"
    )


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) startCamera() else showError("Permiso de cámara requerido") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupGreeting()

        binding.btnAccountScanner.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettingsScanner.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        loadOnnxModel()
        binding.btnScan.setOnClickListener { checkCameraPermission() }

        setupInitialUI() //Configurar UI inicial
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    binding.tvGreetingScanner.text = getString(R.string.hola, name)
                }
            }
    }

    //Configura el estado inicial de la UI
    private fun setupInitialUI() {
        updateScannedImageDisplay(null) // Muestra el layout por defecto (sin imagen escaneada)
        updateTextStatus(getString(R.string.esperando_escaneo)) // Texto inicial
    }

    //Actualiza la vista de la imagen escaneada
    private fun updateScannedImageDisplay(bitmap: Bitmap?) {
        if (bitmap != null) {
            binding.imagePreview.setImageBitmap(bitmap)
            binding.imagePreview.visibility = View.VISIBLE
            binding.defaultImageLayout.visibility = View.GONE
        } else {
            binding.imagePreview.setImageBitmap(null)
            binding.imagePreview.visibility = View.GONE
            binding.defaultImageLayout.visibility = View.VISIBLE
            // Si tienes un ImageView específico dentro de defaultImageLayout con id "ScannedImage"
            // y quieres resetear su src, lo harías aquí. Ej:
            // (binding.defaultImageLayout.findViewById(R.id.ScannedImage) as ImageView).setImageResource(R.drawable.ic_error_placeholder)
            // O si el binding lo genera directamente: binding.ScannedImage.setImageResource(R.drawable.ic_error_placeholder)
        }
    }

    // NUEVA FUNCIÓN: Actualiza el texto de estado y la barra de progreso
    private fun updateTextStatus(message: String, isLoading: Boolean = false) {
        binding.textStatus.text = message
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }


    private fun loadOnnxModel() {
        try {
            updateTextStatus(getString(R.string.cargando_modelo_de_ia), true)
            val modelPath = assetFilePath(requireContext(), "best.onnx")
            ortEnv = OrtEnvironment.getEnvironment()
            ortSession = ortEnv.createSession(modelPath, OrtSession.SessionOptions())
            Log.d("ONNX", "Modelo ONNX cargado correctamente")
            updateTextStatus(getString(R.string.esperando_escaneo), false) // Modelo cargado, listo para escanear
        } catch (e: Exception) {
            Log.e("ScannerFragment", "Error al cargar el modelo: ${e.message}", e)
            showError(getString(R.string.error_al_cargar_el_modelo, e.message)) // Muestra diálogo de error
            updateTextStatus(getString(R.string.error_al_cargar_modelo_reinicie_la_aplicacion), false) // Actualiza TextView de estado
        }
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        return file.absolutePath
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 123) // Usar el ActivityResultLauncher moderno es preferible, pero esto funciona
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap ?: return
            updateScannedImageDisplay(bitmap) // Usa la nueva función para mostrar la imagen
            updateTextStatus(getString(R.string.analizando_imagen), true) // Actualiza estado y muestra progreso
            processImage(bitmap)
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val trace = FirebasePerformance.getInstance().newTrace("image_processing")
        trace.start()
        try {

            val resized = Bitmap.createScaledBitmap(bitmap, 416, 416, true)
            val bufferSize = 3 * 416 * 416
            val inputBuffer = FloatBuffer.allocate(bufferSize)
            TensorImageUtils.bitmapToFloatBuffer(resized, 0, 0, 416, 416, floatArrayOf(0f,0f,0f), floatArrayOf(1f,1f,1f), inputBuffer, 0)
            inputBuffer.rewind()
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, longArrayOf(1, 3, 416, 416))

            val confidenceThreshold = 0.5f // Umbral de confianza

            ortSession?.run(mapOf("images" to inputTensor))?.use { results ->
                val output = results[0].value as Array<Array<FloatArray>>
                val rawOutput = output[0]
                val numClasses = 28
                val numPredictions = 3549
                val detections = mutableListOf<Detection>()
                // (Tu bucle de detección ...)
                for (i in 0 until numPredictions) {
                    val x = rawOutput[0][i]; val y = rawOutput[1][i]; val w = rawOutput[2][i]; val h = rawOutput[3][i]
                    var maxClass = -1; var maxScore = -Float.MAX_VALUE
                    for (c in 0 until numClasses) {
                        val classIndexInRawOutput = 4 + c
                        if (classIndexInRawOutput >= rawOutput.size) continue
                        val classScore = rawOutput[classIndexInRawOutput][i]
                        if (classScore > maxScore) { maxScore = classScore; maxClass = c }
                    }
                    if (maxScore < confidenceThreshold) continue
                    if (maxClass !in labels.indices) continue
                    val label = labels[maxClass]
                    detections.add(Detection(label, maxScore, RectF((x - w/2f) * bitmap.width, (y - h/2f) * bitmap.height, (x + w/2f) * bitmap.width, (y + h/2f) * bitmap.height)))
                }


                if (detections.isNotEmpty()) {
                    val bitmapWithDetections = showDetectionsOnImage(bitmap, detections) // Devuelve el bitmap con las cajas
                    updateScannedImageDisplay(bitmapWithDetections) // Muestra la imagen con las detecciones
                    updateTextStatus("Se encontraron ${detections.size} producto(s).", false)
                    showMultipleDialogs(detections.sortedByDescending { it.confidence }, index = 0, processedClasses = mutableSetOf())
                    trace.incrementMetric("detected_products", detections.size.toLong())
                } else {
                    updateScannedImageDisplay(bitmap) // Muestra la imagen original sin detecciones
                    updateTextStatus(getString(R.string.no_se_detectaron_productos_reintentelo_de_nuevo), false)

                }

            } ?: run {
                updateTextStatus(getString(R.string.fallo_en_la_inferencia_del_modelo), false)
                showError(getString(R.string.fallo_en_la_inferencia_del_modelo)) // Dialogo de error
            }
            inputTensor.close() // Cerrar tensor para liberar recursos

        } catch (e: Exception) {
            updateTextStatus(getString(R.string.error_al_procesar_la_imagen), false)
            showError(getString(R.string.error_al_procesar_imagen, e.message)) // Dialogo de error
            // Considerar resetear la imagen a la original o al placeholder en caso de error grave
            updateScannedImageDisplay(bitmap)
            FirebaseCrashlytics.getInstance().recordException(e)
        } finally {

            trace.stop()
            // Asegurar que el progressBar se oculte si alguna ruta no lo hizo
            if (binding.progressBar.visibility == View.VISIBLE) {
                updateTextStatus(binding.textStatus.text.toString(), false) // Mantiene el mensaje actual pero oculta el loader
            }
        }
    }


    // Devolver el Bitmap con las detecciones dibujadas
    private fun showDetectionsOnImage(originalBitmap: Bitmap, detections: List<Detection>): Bitmap {
        val bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        for (detection in detections) {
            // Código para dibujar las detecciones
            val rect = detection.rect; val label = detection.label; val confidence = detection.confidence
            paint.color = Color.RED; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
            canvas.drawRect(rect, paint)
            paint.color = Color.WHITE; paint.style = Paint.Style.FILL; paint.textSize = 14f
            canvas.drawText("${label.split("__")[0]} (${"%.2f".format(confidence * 100)}%)", rect.left, rect.top - 5, paint)
        }
         binding.imagePreview.setImageBitmap(bitmap) // Se hará en updateScannedImageDisplay
        return bitmap // Devolver el bitmap modificado
    }

    private fun showMultipleDialogs(detections: List<Detection>, index: Int = 0, processedClasses: MutableSet<String>) {
        if (index >= detections.size) {
            updateTextStatus(getString(R.string.proceso_completado_listo_para_nuevo_escaneo), false)
            return
        }

        val detection = detections[index]
        val classNameWithCondition = detection.label
        val confidence = detection.confidence
        val parts = classNameWithCondition.split("__")

        if (parts.size != 2) {
            showMultipleDialogs(detections, index + 1, processedClasses)
            return
        }

        val englishName = parts[0]
        val baseClassName = englishName.lowercase()

        if (processedClasses.contains(baseClassName)) {
            showMultipleDialogs(detections, index + 1, processedClasses)
            return
        }
        processedClasses.add(baseClassName)


        val name = translateToSpanish(englishName.lowercase())
        val condition = if (parts[1].equals("Healthy", ignoreCase = true)) "fresco" else "podrido"
        val category = if (isFruit(englishName.lowercase())) "fruta" else "verdura"

        // Actualizar estado para reflejar que se está interactuando con un diálogo
        val statusText = getString(R.string.estado_interaccion_dialogo, name, condition)
        updateTextStatus(statusText, false)

        val message = getString(
            R.string.mensaje_dialogo_producto,
            name,
            condition,
            category,
            confidence * 100
        )


        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.agregar_al_inventario))
            .setMessage(message)
            .setPositiveButton(getString(R.string.agregar)) { _, _ ->
                saveProductToInventory(englishName, name, condition, category)
                showMultipleDialogs(detections, index + 1, processedClasses)
            }
            .setNegativeButton(getString(R.string.omitir)) { _, _ ->
                showMultipleDialogs(detections, index + 1, processedClasses)
            }
            .setCancelable(false)
            .show()
    }

    private fun saveProductToInventory(
        englishNameForIcon: String,
        spanishName: String,
        condition: String,
        category: String
    ) {
        val userId = auth.currentUser?.uid ?: run {
            showError(getString(R.string.usuario_no_autenticado))
            updateTextStatus(getString(R.string.error_usuario_no_autenticado), false)
            return
        }
        val productLocation = "nevera"
        val productsRef = db.collection("users").document(userId).collection("products")

        updateTextStatus(getString(R.string.guardando, spanishName), true)

        productsRef
            .whereEqualTo("name", spanishName)
            .whereEqualTo("condition", condition)
            .whereEqualTo("category", category)
            .whereEqualTo("location", productLocation)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    val currentQuantity = (documentSnapshot.getLong("quantity") ?: 0L).toInt()
                    val newQuantity = currentQuantity + 1
                    documentSnapshot.reference.update("quantity", newQuantity)
                        .addOnSuccessListener {
                            showSuccess(
                                getString(
                                    R.string.cantidad_actualizada_en_el_inventario,
                                    spanishName
                                ))

                            // Registrar como desperdiciado si está podrido
                            if (condition == "podrido") {
                                val product = UserProduct(
                                    idProduct = documentSnapshot.id,
                                    name = spanishName,
                                    icon = englishNameForIcon.lowercase(),
                                    category = category,
                                    condition = condition,
                                    location = productLocation,
                                    quantity = newQuantity
                                )
                                registerWastedProduct(product)
                            }
                        }
                        .addOnFailureListener { e ->
                            showError(getString(R.string.error_al_actualizar_cantidad))
                            updateTextStatus(getString(R.string.error_al_actualizar, spanishName), false)
                        }
                } else {
                    val productData = hashMapOf(
                        "name" to spanishName, "icon" to englishNameForIcon.lowercase(),
                        "category" to category, "condition" to condition,
                        "location" to productLocation, "quantity" to 1
                    )
                    productsRef.add(productData)
                        .addOnSuccessListener { documentReference ->
                            showSuccess(getString(R.string.agregado_al_inventario, spanishName))

                            // Registrar como desperdiciado si está podrido
                            if (condition == "podrido") {
                                val product = UserProduct(
                                    idProduct = documentReference.id,
                                    name = spanishName,
                                    icon = englishNameForIcon.lowercase(),
                                    category = category,
                                    condition = condition,
                                    location = productLocation,
                                    quantity = 1
                                )
                                registerWastedProduct(product)
                            }
                        }
                        .addOnFailureListener { e ->
                            showError(getString(R.string.error_al_agregar_al_inventario))
                            updateTextStatus(getString(R.string.error_al_agregar, spanishName), false)
                        }
                }
            }
            .addOnFailureListener { e ->
                showError(getString(R.string.error_al_verificar_el_inventario))
                updateTextStatus(getString(R.string.error_al_verificar_inventario), false)
            }
    }

    private fun registerWastedProduct(product: UserProduct) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("wasted_products")
            .whereEqualTo("original_product_id", product.idProduct)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val wastedProduct = hashMapOf(
                        "name" to product.name,
                        "icon" to product.icon,
                        "category" to product.category,
                        "original_product_id" to product.idProduct,
                        "date" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users").document(userId)
                        .collection("wasted_products")
                        .add(wastedProduct)
                        .addOnSuccessListener {
                            Log.d("ScannerFragment", "Producto podrido registrado como desperdiciado: ${product.name}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ScannerFragment", "Error al registrar producto desperdiciado", e)
                            showError("Error al registrar producto desperdiciado")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ScannerFragment", "Error al verificar producto desperdiciado", e)
            }
    }


    private fun showError(message: String) {
        if (isAdded && context != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } else {
            Log.e("ScannerFragment", "Error dialog not shown, fragment not attached: $message")
        }
    }

    private fun showSuccess(message: String) {
        if (isAdded && context != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Éxito")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } else {
            Log.e("ScannerFragment", "Success dialog not shown, fragment not attached: $message")
        }
    }

    private fun isFruit(name: String): Boolean {
        val fruits = listOf("apple", "banana", "grape", "guava", "mango", "orange", "pomegranate", "strawberry")
        return fruits.contains(name.lowercase())
    }

    private fun translateToSpanish(englishName: String): String {
        val translationMap = mapOf(
            "apple" to "manzana", "banana" to "platano", "bellpepper" to "pimiento",
            "carrot" to "zanahoria", "cucumber" to "pepino", "grape" to "uva",
            "guava" to "guayaba", "jujube" to "azufaifo", "mango" to "mango",
            "orange" to "naranja", "pomegranate" to "granada", "potato" to "patata",
            "strawberry" to "fresa", "tomato" to "tomate"
        )
        return translationMap[englishName.lowercase()] ?: englishName
    }
}