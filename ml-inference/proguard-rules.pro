# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep TensorFlow Lite classes that are actually used
-keep class org.tensorflow.lite.Interpreter { *; }
-keep class org.tensorflow.lite.gpu.CompatibilityList { *; }
-keep class org.tensorflow.lite.gpu.GpuDelegate { *; }

# Keep card scanner service interface and types
-keep interface net.yakavenka.cardscanner.CardScannerService { *; }
-keep class net.yakavenka.cardscanner.ScanResult { *; }
-keep class net.yakavenka.cardscanner.ScanResult$* { *; }
