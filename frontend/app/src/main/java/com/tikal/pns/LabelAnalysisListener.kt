package com.tikal.pns

import android.graphics.Bitmap

interface LabelAnalysisListener {

    /**
     * Called upon objects being detected with list of labels and the bitmap.
     * @param labels the map of labels and the MLKIT confidence value for each.
     * @param thumb the smaller thumbnail of the image.
     */
    fun onObjectDetected(labels: Map<String, Float>, thumb: Bitmap?)
}