# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep OpenCV classes that are actually used
-keep class org.opencv.core.Mat { *; }
-keep class org.opencv.core.Core { *; }
-keep class org.opencv.core.MatOfPoint { *; }
-keep class org.opencv.core.Point { *; }
-keep class org.opencv.core.Rect { *; }
-keep class org.opencv.core.Scalar { *; }
-keep class org.opencv.core.Size { *; }
-keep class org.opencv.core.CvType { *; }
-keep class org.opencv.imgproc.Imgproc { *; }
-keep class org.opencv.android.Utils { *; }
