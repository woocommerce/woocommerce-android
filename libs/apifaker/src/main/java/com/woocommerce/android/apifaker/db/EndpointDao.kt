package com.woocommerce.android.apifaker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.Response
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EndpointDao {
    @Transaction
    @Query("Select * FROM Request")
    fun observeEndpoints(): Flow<List<MockedEndpoint>>

    @Transaction
    @Query("Select count(*) FROM Request")
    suspend fun endpointsCount(): Int

    @Transaction
    @Query("""Select * FROM Request WHERE
        type = :type AND
        :path LIKE path AND
        :body LIKE COALESCE(body, '%')
        """)
    fun queryEndpoint(type: ApiType, path: String, body: String): MockedEndpoint?

    @Transaction
    @Query("Select * FROM Request WHERE id = :id")
    suspend fun getEndpoint(id: Long): MockedEndpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: Request): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: Response)

    @Transaction
    suspend fun insertEndpoint(request: Request, response: Response) {
        val id = insertRequest(request)
        insertResponse(response.copy(endpointId = id))
    }
}
