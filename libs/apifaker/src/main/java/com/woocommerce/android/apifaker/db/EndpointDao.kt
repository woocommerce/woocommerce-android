package com.woocommerce.android.apifaker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.woocommerce.android.apifaker.models.Endpoint
import com.woocommerce.android.apifaker.models.EndpointType
import com.woocommerce.android.apifaker.models.EndpointWithResponse
import com.woocommerce.android.apifaker.models.FakeResponse
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EndpointDao {
    @Transaction
    @Query("Select * FROM Endpoint")
    fun observeEndpoints(): Flow<List<EndpointWithResponse>>

    @Transaction
    @Query("Select * FROM Endpoint WHERE type = :type AND :path LIKE path AND :body LIKE COALESCE(body, '%')")
    fun queryEndpoint(type: EndpointType, path: String, body: String): EndpointWithResponse?

    @Transaction
    @Query("Select * FROM Endpoint WHERE id = :id")
    suspend fun getEndpoint(id: Long): EndpointWithResponse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: Endpoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: FakeResponse)

    @Transaction
    suspend fun insertEndpointWithResponse(endpoint: Endpoint, response: FakeResponse) {
        val id = insertEndpoint(endpoint)
        insertResponse(response.copy(endpointId = id))
    }
}
