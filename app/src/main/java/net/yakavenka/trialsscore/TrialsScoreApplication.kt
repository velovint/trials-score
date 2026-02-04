package net.yakavenka.trialsscore

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class TrialsScoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize OpenCV native libraries
        if (!OpenCVLoader.initLocal()) {
            Log.e("TrialsScore", "OpenCV initialization failed")
        } else {
            Log.d("TrialsScore", "OpenCV initialized successfully")
        }
    }
}