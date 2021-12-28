package net.yakavenka.trialsscore.data

import androidx.room.*


data class RiderScoreAggregate(
    @Embedded val riderEntity: RiderScore,
    @Relation(
        parentColumn = "id",
        entityColumn = "riderId"
    )
    val sections: List<SectionScore>
) {
    val riderId: Int get() = riderEntity.id
    val riderName: String get() = riderEntity.riderName
}
