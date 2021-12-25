package net.yakavenka.trialsscore.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RiderScoreDao {
    @Query("SELECT * FROM rider_score")
    fun getAll(): Flow<List<RiderScoreAggregate>>
}