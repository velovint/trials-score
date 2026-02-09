# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }

# Keep OpenCV classes
-keep class org.opencv.** { *; }

# Keep card scanner service interface
-keep interface net.yakavenka.cardscanner.CardScannerService { *; }
-keep class net.yakavenka.cardscanner.ScanResult { *; }
-keep class net.yakavenka.cardscanner.ScanResult$* { *; }
