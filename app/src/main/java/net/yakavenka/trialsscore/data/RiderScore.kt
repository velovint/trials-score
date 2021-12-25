package net.yakavenka.trialsscore.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rider_score")
data class RiderScore(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "rider_name") val riderName: String,
)