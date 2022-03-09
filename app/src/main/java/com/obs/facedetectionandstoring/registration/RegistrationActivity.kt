package com.obs.facedetectionandstoring.registration

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Pair
import android.util.Size
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.obs.facedetectionandstoring.R
import com.obs.facedetectionandstoring.SimilarityClassifier
import org.checkerframework.checker.nullness.qual.NonNull
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.ReadOnlyBufferException
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.experimental.inv


class RegistrationActivity : AppCompatActivity() {

    var detector: FaceDetector? = null

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    var previewView: PreviewView? = null
    var face_preview: ImageView? = null

    //    var tfLite: Interpreter? = null
    var reco_name: TextView? = null

    //    var preview_info: TextView? = null
    var recognize: Button? = null
    var camera_switch: Button? = null
    var actions: Button? = null
    var add_face: Button? = null
    var cameraSelector: CameraSelector? = null
    var start = true
    var flipX: Boolean = false
    var context: Context = this
    var cam_face = CameraSelector.LENS_FACING_BACK

    lateinit var intValues: IntArray
    var inputSize = 112
    var isModelQuantized = false
    lateinit var embeedings: Array<FloatArray>
    var IMAGE_MEAN = 128.0f
    var IMAGE_STD = 128.0f
    var OUTPUT_SIZE = 192
    private val SELECT_PICTURE = 1
    var cameraProvider: ProcessCameraProvider? = null
    private val MY_CAMERA_REQUEST_CODE = 100
    private var SECRET_KEY = "123456789"
    private var SALTVALUE = "abcdefg"

    private var fromActivity: String? = ""
    var tfLite: Interpreter? = null

    var modelFile = "mobile_face_net.tflite"
    private var registered: HashMap<String, SimilarityClassifier.Recognition> =
        HashMap<String, SimilarityClassifier.Recognition>()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registered = readFromSP()
        setContentView(R.layout.activity_registration)
        face_preview = findViewById(R.id.imageView)
        reco_name = findViewById(R.id.textView)
//        preview_info = findViewById(R.id.textView2)
        add_face = findViewById(R.id.imageButton)
//        add_face?.visibility = View.INVISIBLE

        face_preview?.visibility = View.INVISIBLE
        recognize = findViewById(R.id.button3)
        camera_switch = findViewById(R.id.button5)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                MY_CAMERA_REQUEST_CODE
            )
        }
        camera_switch?.setOnClickListener(View.OnClickListener {
            if (cam_face == CameraSelector.LENS_FACING_BACK) {
                cam_face = CameraSelector.LENS_FACING_FRONT
                flipX = true
            } else {
                cam_face = CameraSelector.LENS_FACING_BACK
                flipX = false
            }
            cameraProvider!!.unbindAll()
            cameraBind()
        })

        add_face?.setOnClickListener(View.OnClickListener {
//            addFace()
            getReferDialog()
        })

//        recognize?.setOnClickListener(View.OnClickListener {
//            /* if (recognize?.getText().toString() == "Recognize") {
//                 start = true
//                 recognize?.text = "Add Face"
//                 add_face?.visibility = View.INVISIBLE
//                 reco_name?.visibility = View.VISIBLE
//                 face_preview?.visibility = View.INVISIBLE
//                 //                    preview_info.setText("\n    Recognized Face:");
//             } else {
//                 recognize?.text = "Recognize"
//                 add_face?.visibility = View.VISIBLE
//                 reco_name?.visibility = View.INVISIBLE
//                 face_preview?.visibility = View.VISIBLE
//             }*/
//
//            add_face?.visibility = View.VISIBLE
//            reco_name?.visibility = View.INVISIBLE
//            face_preview?.visibility = View.INVISIBLE
//
//        })

//        Load model
        try {
            tfLite = Interpreter(loadModelFile(this, modelFile))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //Initialize Face Detector
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)
        cameraBind()
    }

    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity, MODEL_FILE: String): @NonNull MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun getReferDialog() {
        start = false
        val alertDialog = AlertDialog.Builder(context)
        val customLayout: View = this.layoutInflater.inflate(R.layout.layout_add_name, null)
        alertDialog.setView(customLayout)
        val alert = alertDialog.create()
        alert.setCancelable(false)
        alert.setCanceledOnTouchOutside(false)
        val edtUserName = customLayout.findViewById<EditText>(R.id.appCompatEditText)
        val submitBtn: AppCompatTextView = customLayout.findViewById(R.id.txt_add)
        val tvNo: AppCompatTextView = customLayout.findViewById(R.id.txt_cancel)
        submitBtn.setOnClickListener { v ->
            val userText = edtUserName.text.toString()
            if (userText != "") {
                alert.dismiss()
                val result = SimilarityClassifier.Recognition(
                    "0", "", -1f
                )
                result.setExtra(embeedings)
                registered[edtUserName.text.toString()] = result
                start = true
                registered.putAll(readFromSP())
                insertToSP(registered, false)
            } else {
                Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
        tvNo.setOnClickListener { v: View? ->
            start = true
            alert.dismiss()
        }
        alert.show()
    }


    private fun addFace() {
        start = false
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter Name")
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("ADD") { dialog, which ->
            val result = SimilarityClassifier.Recognition(
                "0", "", -1f
            )
            result.setExtra(embeedings)
            registered[input.text.toString()] = result
            start = true
            registered.putAll(readFromSP())
            insertToSP(registered, false)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which ->
            start = true
            dialog.cancel()
        }
        builder.show()
    }

    private fun insertToSP(
        jsonMap: HashMap<String, SimilarityClassifier.Recognition>,
        clear: Boolean
    ) {
        if (clear) jsonMap.clear() else jsonMap.putAll(readFromSP())
        val jsonString = Gson().toJson(jsonMap)

        var sharedPreferences: SharedPreferences
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sharedPreferences = EncryptedSharedPreferences.create(
                "HashMap",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            sharedPreferences =
                getSharedPreferences(
                    "HashMap",
                    Context.MODE_PRIVATE
                )
        }

        val editor = sharedPreferences.edit()
        editor.putString("map", jsonString)
        Log.v("overallname", jsonString.toString())
        //System.out.println("Input josn"+jsonString.toString());
        editor.apply()
        Toast.makeText(this, "Data stored Successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun readFromSP(): HashMap<String, SimilarityClassifier.Recognition> {

        var sharedPreferences: SharedPreferences
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sharedPreferences = EncryptedSharedPreferences.create(
                "HashMap",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            sharedPreferences =
                getSharedPreferences(
                    "HashMap",
                    Context.MODE_PRIVATE
                )
        }

//        val sharedPreferences = EncryptedSharedPreferences.create(
//            "HashMap",
//            masterKeyAlias,
//            context,
//            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//        )

        val defValue = Gson().toJson(HashMap<String, SimilarityClassifier.Recognition>())

        val json = sharedPreferences.getString("map", defValue)
        val token: TypeToken<HashMap<String?, SimilarityClassifier.Recognition?>?> =
            object : TypeToken<HashMap<String?, SimilarityClassifier.Recognition?>?>() {}
        val retrievedMap: HashMap<String, SimilarityClassifier.Recognition> =
            Gson().fromJson<HashMap<String, SimilarityClassifier.Recognition>>(json, token.type)
        for ((_, value) in retrievedMap) {
            val output = Array(1) {
                FloatArray(
                    OUTPUT_SIZE
                )
            }
            var arrayList = value.extra as ArrayList<*>
            arrayList = arrayList[0] as ArrayList<*>
            for (counter in arrayList.indices) {
                output[0][counter] = (arrayList[counter] as Double).toFloat()
            }
            value.setExtra(output)
        }
        Log.v("namelist", registered.toString())
//        Toast.makeText(context, "Recognitions Loaded", Toast.LENGTH_SHORT).show()
        return retrievedMap
    }

    private fun cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        previewView = findViewById(R.id.previewView)
        cameraProviderFuture?.addListener(Runnable {
            try {
                cameraProvider = cameraProviderFuture?.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun bindPreview(cameraProvider: ProcessCameraProvider?) {
        val preview = Preview.Builder()
            .build()
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(cam_face)
            .build()
        preview.setSurfaceProvider(previewView!!.surfaceProvider)
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
            .build()
        val executor: Executor = Executors.newSingleThreadExecutor()
        imageAnalysis.setAnalyzer(executor, { imageProxy ->
            var image: InputImage? = null
            @SuppressLint("UnsafeExperimentalUsageError") val mediaImage// Camera Feed-->Analyzer-->ImageProxy-->mediaImage-->InputImage(needed for ML kit face detection)
                    = imageProxy.image
            if (mediaImage != null) {
                image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                println("Rotation " + imageProxy.imageInfo.rotationDegrees)
            }
            println("ANALYSIS")
            //Process acquired image to detect faces
            val result = detector!!.process(
                image!!
            ).addOnSuccessListener { faces ->
                if (faces.size != 0) {
                    val face = faces[0] //Get first face from detected faces
                    println(face)
                    //mediaImage to Bitmap
                    val frame_bmp: Bitmap? = toBitmap(mediaImage!!)
                    val rot = imageProxy.imageInfo.rotationDegrees
                    val frame_bmp1: Bitmap? =
                        rotateBitmap(frame_bmp, rot, false, false)
                    //Get bounding box of face
                    val boundingBox = RectF(face.boundingBox)
                    //Crop out bounding box from whole Bitmap(image)
                    var cropped_face: Bitmap? =
                        getCropBitmapByCPU(frame_bmp1, boundingBox)
                    if (flipX) cropped_face =
                        rotateBitmap(cropped_face, 0, flipX, false)
                    val scaled: Bitmap? = getResizedBitmap(cropped_face, 112, 112)
                    if (start) recognizeImage(scaled) //Send scaled bitmap to create face embeddings.
                    println(boundingBox)
                    try {
                        Thread.sleep(10) //Camera preview refreshed every 10 millisec(adjust as required)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                } else {
                    if (registered.isEmpty())
                        Log.v("Facedata name", "Add Face")
                    else
                        Log.v("Facedata name", "No Data")
                }
            }.addOnFailureListener { }.addOnCompleteListener { imageProxy.close() }
        })
        cameraProvider?.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector!!,
            imageAnalysis,
            preview
        )
    }

    private fun toBitmap(image: Image): Bitmap? {
        val nv21: ByteArray? = YUV_420_888toNV21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun YUV_420_888toNV21(image: Image): ByteArray? {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride == 1)
        var pos = 0
        if (rowStride == width) {
            yBuffer[nv21, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong()
            while (pos < ySize) {
                yBufferPos += rowStride.toLong()
                yBuffer.position(yBufferPos.toInt())
                yBuffer[nv21, pos, width]
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        assert(rowStride == image.planes[1].rowStride)
        assert(pixelStride == image.planes[1].pixelStride)
        if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
            val savePixel = vBuffer[1]
            try {
                vBuffer.put(1, savePixel.inv() as Byte)
                if (uBuffer[0] == savePixel.inv() as Byte) {
                    vBuffer.put(1, savePixel)
                    vBuffer.position(0)
                    uBuffer.position(0)
                    vBuffer[nv21, ySize, 1]
                    uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                    return nv21
                }
            } catch (ex: ReadOnlyBufferException) {
            }
            vBuffer.put(1, savePixel)
        }
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer[vuPos]
                nv21[pos++] = uBuffer[vuPos]
            }
        }
        return nv21
    }

    private fun rotateBitmap(
        bitmap: Bitmap?, rotationDegrees: Int, flipX: Boolean, flipY: Boolean
    ): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }

    private fun getCropBitmapByCPU(source: Bitmap?, cropRectF: RectF): Bitmap? {
        val resultBitmap = Bitmap.createBitmap(
            cropRectF.width().toInt(),
            cropRectF.height().toInt(), Bitmap.Config.ARGB_8888
        )
        val cavas = Canvas(resultBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        cavas.drawRect( //from  w w  w. ja v  a  2s. c  om
            RectF(0F, 0F, cropRectF.width(), cropRectF.height()),
            paint
        )
        val matrix = Matrix()
        matrix.postTranslate(-cropRectF.left, -cropRectF.top)
        cavas.drawBitmap(source!!, matrix, paint)
        if (source != null && !source.isRecycled) {
            source.recycle()
        }
        return resultBitmap
    }

    fun getResizedBitmap(bm: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        val width = bm?.width
        val height = bm?.height
        val scaleWidth = newWidth.toFloat() / width!!
        val scaleHeight = newHeight.toFloat() / height!!
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        val resizedBitmap = Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
        bm.recycle()
        return resizedBitmap
    }

    fun recognizeImage(bitmap: Bitmap?) {
//        face_preview!!.setImageBitmap(bitmap)
        val imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())
        intValues = IntArray(inputSize * inputSize)
        bitmap?.getPixels(intValues, 0, bitmap?.width, 0, 0, bitmap.width, bitmap.height)
        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixelValue = intValues[i * inputSize + j]
                if (isModelQuantized) {
                    imgData.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                } else { // Float model
                    imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
        val inputArray = arrayOf<Any>(imgData)
        val outputMap: MutableMap<Int, Any> = HashMap()
        embeedings =
            Array(1) { FloatArray(OUTPUT_SIZE) } //output of model will be stored in this variable
        outputMap[0] = embeedings
        tfLite?.runForMultipleInputsOutputs(inputArray, outputMap) //Run model
        var distance = Float.MAX_VALUE
        val id = "0"
        var label: String? = "?"
        if (registered.size > 0) {
            val nearest: Pair<String, Float> =
                findNearest(embeedings[0]) //Find closest matching face
            if (nearest != null) {
                val name = nearest.first
                label = name
                distance = nearest.second
                if (distance < 1.000f) //If distance between Closest found face is more than 1.000 ,then output UNKNOWN face.
//                    reco_name!!.text = name else reco_name!!.text = "Unknown"
                    println("nearest: $name - distance: $distance")
            }
        }
    }

    private fun findNearest(emb: FloatArray): Pair<String, Float> {
        var ret: Pair<String, Float>? = null
        for ((name, value) in registered) {
            val knownEmb = (value.getExtra() as Array<FloatArray>)[0]
            var distance = 0f
            for (i in emb.indices) {
                val diff = emb[i] - knownEmb[i]
                distance += diff * diff
            }
            distance = Math.sqrt(distance.toDouble()).toFloat()
            if (ret == null || distance < ret.second) {
                ret = Pair(name, distance)
            }
        }
        return ret!!
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

}