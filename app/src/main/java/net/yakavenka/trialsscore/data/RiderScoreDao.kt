package net.yakavenka.trialsscore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RiderScoreDao {
    @Query("SELECT * FROM rider_score")
    fun getAll(): Flow<List<RiderScoreAggregate>>

    @Query("SELECT * FROM section_score WHERE riderId = :riderId")
    fun sectionScores(riderId: Int): Flow<List<SectionScore>>

    @Update
    suspend fun updateSectionScore(sectionScore: SectionScore)

    @Query("DELETE FROM section_score WHERE riderId = :riderId")
    suspend fun deleteRiderScores(riderId: Int)

    @Insert
    suspend fun insertAll(sectionScores: List<SectionScore>)

    @Insert
    suspend fun addRider(riderScore: RiderScore)
}