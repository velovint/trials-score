# Consumer ProGuard rules for ml-inference library

# Keep public API
-keep public interface net.yakavenka.cardscanner.CardScannerService { *; }
-keep public class net.yakavenka.cardscanner.ScanResult { *; }
-keep public class net.yakavenka.cardscanner.ScanResult$* { *; }
-keep public class net.yakavenka.cardscanner.OpenCVCardScannerService {
    public <init>(android.content.Context);
}
