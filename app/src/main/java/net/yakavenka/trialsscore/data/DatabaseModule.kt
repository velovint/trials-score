package net.yakavenka.trialsscore.data

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideScoreDatabase(@ApplicationContext context: Context): ScoreDatabase {
        return ScoreDatabase.getDatabase(context)
    }

    @Provides
    fun provideRiderScoreDao(scoreDatabase: ScoreDatabase): RiderScoreDao {
        return scoreDatabase.riderScoreDao()
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            migrations = listOf(SharedPreferencesMigration(context, USER_PREFERENCES_NAME)),
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES_NAME) }
        )
    }

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
}

@InstallIn(SingletonComponent::class)
@Module
abstract class FileStorageModule {
    @Binds
    @Singleton
    abstract fun bindFileStorageDao(
        impl: ContentResolverFileStorage
    ): FileStorageDao
}