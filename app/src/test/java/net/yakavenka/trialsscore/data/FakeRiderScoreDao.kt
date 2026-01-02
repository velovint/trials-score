package net.yakavenka.trialsscore.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Configurable fake DAO for testing.
 *
 * Supports two patterns:
 * 1. Mutable storage: Tests can populate existingScores and summary lists for simple scenarios
 * 2. Configurable Flows: Tests can override getAllFlow and fetchSummaryFlow for complex scenarios
 *
 * Example usage:
 * ```
 * // Pattern 1: Mutable storage
 * val dao = FakeRiderScoreDao()
 * dao.existingScores.add(SectionScore(...))
 *
 * // Pattern 2: Configurable Flows
 * val dao = FakeRiderScoreDao()
 * dao.getAllFlow = flowOf(data1, data2)  // Multiple emissions
 * ```
 */
class FakeRiderScoreDao : RiderScoreDao {
    /**
     * Mutable storage for section scores.
     * Tests can populate this for simple scenarios.
     * Used by sectionScores() and insertAll() methods.
     */
    val existingScores = mutableListOf<SectionScore>()

    /**
     * Mutable storage for rider score summaries.
     * Tests can populate this for simple scenarios.
     * Used by default fetchSummaryFlow.
     */
    val summary = mutableListOf<RiderScoreSummary>()

    /**
     * Configurable Flow for getAll().
     * Tests can set this to control behavior (e.g., multiple emissions).
     * Defaults to emitting empty list.
     */
    var getAllFlow: Flow<List<RiderScoreAggregate>> = flow { emit(emptyList()) }

    /**
     * Configurable Flow for fetchSummary().
     * Tests can set this to control behavior (e.g., multiple emissions).
     * Defaults to emitting from summary mutable list.
     */
    var fetchSummaryFlow: Flow<List<RiderScoreSummary>> = flow { emit(summary) }

    override fun getAll(): Flow<List<RiderScoreAggregate>> = getAllFlow

    override fun fetchSummary(): Flow<List<RiderScoreSummary>> = fetchSummaryFlow

    override fun sectionScores(riderId: Int, loopNumber: Int): Flow<List<SectionScore>> = flow {
        // Filter existingScores by riderId and loopNumber
        val scores = existingScores.filter { it.riderId == riderId && it.loopNumber == loopNumber }
        emit(scores)
    }

    override suspend fun insertAll(sectionScores: List<SectionScore>) {
        existingScores.addAll(sectionScores)
    }

    // Unimplemented methods throw NotImplementedError
    override fun getRider(riderId: Int): Flow<RiderScore> {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun updateSectionScore(sectionScore: SectionScore) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun deleteAllRiders() {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun deleteAllScores() {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun deleteRiderScores(riderId: Int) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun addRider(riderScore: RiderScore) {
        throw NotImplementedError("Not configured for this test")
    }

    override suspend fun updateRider(riderScore: RiderScore) {
        throw NotImplementedError("Not configured for this test")
    }
}
