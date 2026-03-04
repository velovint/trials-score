package net.yakavenka.trialsscore.camera

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.yakavenka.cardscanner.CardScannerService
import net.yakavenka.cardscanner.OpenCVCardScannerService

/**
 * Hilt module for providing camera and scanning dependencies.
 *
 * Binds CardScannerService to OpenCVCardScannerService for real CV + TFLite inference.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCardScannerService(
        @ApplicationContext context: Context
    ): CardScannerService {
        return OpenCVCardScannerService(context)
    }
}
