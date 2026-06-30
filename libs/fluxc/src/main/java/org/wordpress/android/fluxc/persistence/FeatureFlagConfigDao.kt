package org.wordpress.android.fluxc.persistence

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE

@Dao
abstract class FeatureFlagConfigDao {
    @Query("SELECT * from FeatureFlagConfigurations")
    abstract fun getFeatureFlagList(): List<FeatureFlag>

    @Query("SELECT * from FeatureFlagConfigurations WHERE local_site_id = :localSiteId")
    abstract fun observeFeatureFlagList(localSiteId: LocalId): Flow<List<FeatureFlag>>

    @Query("SELECT * from FeatureFlagConfigurations WHERE `key` = :key AND local_site_id = :localSiteId")
    abstract fun getFeatureFlag(key: String, localSiteId: LocalId): List<FeatureFlag>

    @Transaction
    @Suppress("SpreadOperator")
    open fun insert(featureFlags: Map<String, Boolean>, localSiteId: LocalId) {
        featureFlags.forEach {
            insert(
                    FeatureFlag(
                            key = it.key,
                            localSiteId = localSiteId,
                            value = it.value,
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis(),
                            source = REMOTE
                    )
            )
        }
    }

    @Query("DELETE FROM FeatureFlagConfigurations")
    abstract fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(offer: FeatureFlag)

    @Entity(
            tableName = "FeatureFlagConfigurations",
            primaryKeys = ["key", "local_site_id"]
    )
    data class FeatureFlag(
        val key: String,
        @ColumnInfo(name = "local_site_id") val localSiteId: LocalId,
        val value: Boolean,
        @ColumnInfo(name = "created_at") val createdAt: Long,
        @ColumnInfo(name = "modified_at") val modifiedAt: Long,
        @ColumnInfo(name = "source") val source: FeatureFlagValueSource
    )

    enum class FeatureFlagValueSource(value: Int) {
        BUILD_CONFIG(0),
        REMOTE(1),
    }

    class FeatureFlagValueSourceConverter {
        @TypeConverter
        fun toFeatureFlagValueSource(value: Int): FeatureFlagValueSource =
                enumValues<FeatureFlagValueSource>()[value]

        @TypeConverter
        fun fromFeatureFlagValueSource(value: FeatureFlagValueSource): Int = value.ordinal
    }
}
