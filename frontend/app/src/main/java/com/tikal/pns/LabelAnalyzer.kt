package com.tikal.pns

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Threshold for MLKT labels confidence.
 */
private const val MLKT_CONFIDENCE_THRESHOLD = 0.8

/**
 * Thumbnail dimensions.
 */
private const val THUMB_WIDTH = 640
private const val THUMB_HEIGHT = 640


class LabelAnalyzer: ImageAnalysis.Analyzer {

    private val isBusy = AtomicBoolean(false)

    /**
     * Analyze the next image from CameraX.
     */
    override fun analyze(imageProxy: ImageProxy, rotationDegrees: Int) {

        // Don't process new images until label submitted to backend
        if (!isBusy.compareAndSet(false, true))
            return

        // Get image planes
        val y = imageProxy.planes[0]
        val u = imageProxy.planes[1]
        val v = imageProxy.planes[2]

        // Convert to color map
        val Yb = y.buffer.remaining()
        val Ub = u.buffer.remaining()
        val Vb = v.buffer.remaining()


        // Serialize the buffer in YUV format
        val data = ByteArray(Yb + Ub + Vb)
        y.buffer.get(data, 0, Yb)
        u.buffer.get(data, Yb, Ub)
        v.buffer.get(data, Yb + Ub, Vb)

        val metadata = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12)
            .setHeight(imageProxy.height)
            .setWidth(imageProxy.width)
            .setRotation(getRotation(rotationDegrees))
            .build()

        val labelImage = FirebaseVisionImage.fromByteArray(data, metadata)

        // Process the image
        val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler()
        labeler.processImage(labelImage)
            .addOnSuccessListener { labels ->
                // Objects recognized at certain confidence
                if (labels.size > 0) {

                    val thumb = BitmapFactory.decodeByteArray(data,0,data.size)

                    labels.forEach {
                        // If passes the confidence threshold - report on to the backend
                        if (it.confidence > MLKT_CONFIDENCE_THRESHOLD)
                            reportObject(it.text, thumb)
                    }
                }
                isBusy.set(false)
            }
    }

    /**
     * Compute image rotation.
     */
    private fun getRotation(rotationCompensation: Int): Int {
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
            }
        }
        return result
    }

    private fun reportObject(label: String, thumb: Bitmap?) {
        Log.i("PnS", "object $label")
    }

}