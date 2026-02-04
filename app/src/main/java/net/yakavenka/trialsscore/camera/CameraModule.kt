package net.yakavenka.trialsscore.camera

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing camera and scanning dependencies.
 *
 * Binds CardScannerService to its implementation.
 * In Phase 3, this module will bind to OpenCVCardScannerService for real CV.
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCardScannerService(): CardScannerService {
        return MockCardScannerService()
    }
}
