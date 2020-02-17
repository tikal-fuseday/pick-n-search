package com.tikal.pns

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
private const val MLKT_SUCCESS_THRESHOLD = 0.8


class LabelAnalyzer() : ImageAnalysis.Analyzer {

    private val isBusy = AtomicBoolean(false)

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {

        if (!isBusy.compareAndSet(false, true))
            return

        // Get image planes
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]

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
            .setHeight(image.height)
            .setWidth(image.width)
            .setRotation(getRotation(rotationDegrees))
            .build()

        val labelImage = FirebaseVisionImage.fromByteArray(data, metadata)

        val labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler()
        labeler.processImage(labelImage)
            .addOnSuccessListener { labels ->
                if (labels.size > 0) {
                    labels.forEach {
                        // If passes the threshold - report on to the backend
                        if (it.confidence > MLKT_SUCCESS_THRESHOLD)
                            reportObject(it.text)
                    }
                }
                isBusy.set(false)
            }
    }

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

    private fun reportObject(label: String) {
        Log.i("PnS", "object $label")
    }
}