package net.yakavenka.trialsscore.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RiderScoreDao {
    @Query("SELECT * FROM rider_score ORDER BY class")
    @Transaction
    fun getAll(): Flow<List<RiderScoreAggregate>>

    @Query("SELECT * FROM rider_score WHERE id = :riderId")
    fun getRider(riderId: Int): Flow<RiderScore>

    @Query("SELECT * FROM section_score WHERE riderId = :riderId")
    fun sectionScores(riderId: Int): Flow<List<SectionScore>>

    @Update
    suspend fun updateSectionScore(sectionScore: SectionScore)

    @Query("DELETE from rider_score")
    suspend fun deleteAllRiders()

    @Query("DELETE FROM section_score")
    suspend fun deleteAllScores()

    @Query("DELETE FROM section_score WHERE riderId = :riderId")
    suspend fun deleteRiderScores(riderId: Int)

    @Insert
    suspend fun insertAll(sectionScores: List<SectionScore>)
    @Insert
    suspend fun addRider(riderScore: RiderScore)
    @Query("SELECT rs.id, rs.name, rs.class,\n" +
            "        COUNT(CASE WHEN ss.points >= 0 THEN 1 ELSE NULL END) as sections_ridden,\n" +
            "        COUNT(ss.points) as total_sections,\n" +
            "        SUM(CASE WHEN ss.points >= 0 THEN ss.points ELSE 0 END) as points,\n" +
            "        SUM(CASE ss.points WHEN 0 THEN 1 ELSE 0 END) AS cleans\n" +
            "      FROM rider_score AS rs\n" +
            "      LEFT JOIN section_score AS ss ON ss.riderId = rs.id\n" +
            "      GROUP BY rs.id\n" +
            "      ORDER BY class, sections_ridden DESC, points ASC, cleans DESC")
    fun fetchSummary(): Flow<List<RiderScoreSummary>>
    @Update
    suspend fun updateRider(riderScore: RiderScore)
}