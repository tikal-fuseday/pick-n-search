package com.tikal.pns

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.tikal.pns.network.ApiClient
import com.tikal.pns.network.ApiService
import com.tikal.pns.network.GeneralResponse
import com.tikal.pns.network.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream


// Permissions for camera
private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity(), LabelAnalysisListener {

    private lateinit var viewFinder: TextureView
    private lateinit var btnShoot: ImageView
    private lateinit var reDetected: View
    private lateinit var tvDetectedLabel: TextView
    private lateinit var button: Button
    private lateinit var service: ApiService
    private lateinit var storageRef: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        // Setup controls
        viewFinder = findViewById(R.id.tvViewFinder)
        reDetected = findViewById(R.id.reDetected)
        button = findViewById(R.id.button)
        reDetected.visibility = View.INVISIBLE
        tvDetectedLabel = findViewById(R.id.tvDetectedLabel)

//        btnShoot = findViewById(R.id.btnShoot)

//        btnShoot.setOnClickListener { shoot() }

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }

        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        service = ApiClient.create()
        val storage = FirebaseStorage.getInstance()

        // Create a storage reference from our app
        storageRef = storage.reference

        button.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Sets up the camera and the image analyzer.
     */
    private fun startCamera() {
        // Create configuration for image analyzer
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            val analyzerThread = HandlerThread(
                "LabelAnalysis"
            ).apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))

            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        // Create MLKIT analysis use case
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = LabelAnalyzer(viewFinder, this@MainActivity)
        }

        // Build the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(16, 9))
        }.build()
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Bind use case to lifecycle
        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    private fun shoot() {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)
        Log.i("PnS", "shooting frame")
    }

    //
    // METHODS
    //
    // API from LabelAnalysisListener

    override fun onObjectDetected(labels: Map<String, Float>, thumb: Bitmap?) {

        uploadImageToStorage(thumb)

        runOnUiThread {
            Log.i("PnS", "object detected")
            var max = 0f
            var label = "searching ...."
            labels.forEach {
                if (it.value > max) {
                    max = it.value
                    label = it.key
                }
                Log.i("PnS", "object: ${it.key}, confidence: ${it.value}")
            }

            // Update UI
            reDetected.visibility = View.VISIBLE
            tvDetectedLabel.text = label

            // Remove after a while
            reDetected.postDelayed({
                reDetected.visibility = View.INVISIBLE
                tvDetectedLabel.text = "searching ...."
            }, 1000)
        }
    }

    private fun uploadImageToStorage(thumb: Bitmap?) {
        val mountainsRef = storageRef.child(System.currentTimeMillis().toString() + ".jpg")

        val baos = ByteArrayOutputStream()
        thumb?.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val data = baos.toByteArray()

        var uploadTask = mountainsRef.putBytes(data)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            mountainsRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result

                sendUrlToServer(downloadUri)
            } else {
                println("viewFinder = ${viewFinder}")
            }
        }
    }

    private fun sendUrlToServer(downloadUri: Uri?) {
        val body = Request(downloadUri.toString())
        val call = service.sendToServer(body)

        call.enqueue(object : Callback<GeneralResponse> {
            override fun onResponse(
                call: Call<GeneralResponse>,
                response: Response<GeneralResponse>
            ) {
                if (response.code() == 200) {
                    val weatherResponse = response.body()!!
                    println("viewFinder = ${viewFinder}")
                }
            }

            override fun onFailure(call: Call<GeneralResponse>, t: Throwable) {
                println("viewFinder = ${viewFinder}")
            }
        })
    }
}
