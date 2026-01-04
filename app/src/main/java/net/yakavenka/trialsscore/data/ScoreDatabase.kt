package net.yakavenka.trialsscore.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RiderScore::class, SectionScore::class], version = 5)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun riderScoreDao(): RiderScoreDao

    companion object {
        @Volatile
        private var INSTANCE: ScoreDatabase? = null

        fun getDatabase(context: Context): ScoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        ScoreDatabase::class.java,
                        "score_database"
                    )
                        .createFromAsset("database/score_database.db")
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}