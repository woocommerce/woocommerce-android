package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.data.WCLocationModel

@Dao
internal abstract class LocationsDao {
    @Query(
        """
        SELECT * FROM LocationEntity
        WHERE parentCode = ""
        """
    )
    abstract suspend fun getCountries(): List<WCLocationModel>

    @Query(
        """
        SELECT * FROM LocationEntity
        WHERE parentCode = :country
        """
    )
    abstract suspend fun getStates(country: String): List<WCLocationModel>

    @Upsert
    abstract suspend fun upsertLocations(locations: List<WCLocationModel>)
}
