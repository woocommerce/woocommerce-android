package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.converters.CurrencyPositionConverter
import org.wordpress.android.fluxc.utils.FakeOrderSummaryGenerator.asOrderSummaries
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class OrderSummaryDaoChunksTest {

    private lateinit var db: WCAndroidDatabase
    private lateinit var dao: OrderSummaryDao
    private val logs = StringBuilder()

    companion object Companion {
        val siteId = LocalId(42)
    }

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            WCAndroidDatabase::class.java
        )
            .addTypeConverter(CurrencyPositionConverter(Mockito.mock()))
            .setQueryCallback({ sqlQuery, _ ->
                logs.append(sqlQuery)
                    .append("\n")
            }, Executors.newSingleThreadExecutor())
            .build()
        dao = db.orderSummaryDao
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `given more than 200 order IDs when getOrderSummaries is called then it chunks input and returns all results in a single transaction`() =
        runTest {
            val orderSummaries = (1..250).asOrderSummaries(siteId)
            dao.upsertOrderSummaries(orderSummaries)

            val remoteIds = orderSummaries.map(WCOrderSummaryModel::orderId)

            // Call your method that chunks with a for loop and is annotated with @Transaction
            val result = dao.getOrderSummaries(siteId, remoteIds)

            val expectedFirstChunk = createExpectedQuery(200)
            val expectedSecondChunk = createExpectedQuery(50)

            val normalizedLogs = logs.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
            assertThat(normalizedLogs).contains(
                buildString {
                    appendLine(expectedFirstChunk)
                    appendLine(expectedSecondChunk)
                    appendLine("TRANSACTION SUCCESSFUL")
                    appendLine("END TRANSACTION")
                }
            )
            assertThat(result).containsExactlyInAnyOrderElementsOf(orderSummaries)
        }

    private fun createExpectedQuery(itemCount: Int): String {
        val placeholders = List(itemCount) { "?" }.joinToString(",")
        return "SELECT * FROM OrderSummaryEntity\nWHERE siteId = ?\nAND orderId IN ($placeholders)"
    }
}
