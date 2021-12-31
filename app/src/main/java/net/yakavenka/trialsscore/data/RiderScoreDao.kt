package net.yakavenka.trialsscore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RiderScoreDao {
    @Query("SELECT * FROM rider_score ORDER BY class")
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

    @Query("SELECT rs.id, rs.name, rs.class, COUNT(ss.points) as sections_ridden,  SUM(ss.points) as points,\n" +
            "        SUM(CASE ss.points  WHEN 0 THEN 1 ELSE 0 END) AS cleans\n" +
            "      FROM rider_score AS rs\n" +
            "      LEFT JOIN section_score AS ss ON ss.riderId = rs.id AND ss.points >= 0\n" +
            "      GROUP BY rs.id\n" +
            "      ORDER BY class, sections_ridden DESC, points ASC, cleans DESC")
    fun fetchSummary(): Flow<List<RiderScoreSummary>>
}