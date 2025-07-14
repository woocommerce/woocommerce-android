package com.woocommerce.android.background

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class LastUpdateDataStoreTest : BaseUnitTest() {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var currentTimeProvider: CurrentTimeProvider

    private lateinit var sut: LastUpdateDataStore

    @Test
    fun `given shouldUpdateData is called, when time elapsed is enough, then return true`() = testBlocking {
        // Given
        createLastUpdateScenarioWith(
            lastUpdateTimestamp = 1000,
            currentTimestamp = 2000
        )
        val maxOutdatedTime = 500L

        val testKey = "1-ORDERS-123456"

        // When
        val result = sut.shouldUpdateData(
            key = testKey,
            maxOutdatedTime = maxOutdatedTime
        ).single()

        // Then
        Assertions.assertThat(result).isTrue
    }

    @Test
    fun `given shouldUpdateData is called, when time elapsed is not enough, then return false`() = testBlocking {
        // Given
        createLastUpdateScenarioWith(
            lastUpdateTimestamp = 1000,
            currentTimestamp = 1200
        )
        val maxOutdatedTime = 500L

        val testKey = "1-ORDERS-123456"

        // When
        val result = sut.shouldUpdateData(
            key = testKey,
            maxOutdatedTime = maxOutdatedTime
        ).single()

        // Then
        Assertions.assertThat(result).isFalse
    }

    @Test
    fun `given shouldUpdateData is called, when no previous update exists, then return true`() = testBlocking {
        // Given
        createLastUpdateScenarioWith(
            lastUpdateTimestamp = null,
            currentTimestamp = 100
        )
        val maxOutdatedTime = 500L

        val testKey = "1-ORDERS-123456"

        // When
        val result = sut.shouldUpdateData(
            key = testKey,
            maxOutdatedTime = maxOutdatedTime
        ).single()

        // Then
        Assertions.assertThat(result).isTrue
    }

    @Test
    fun `given data is updated, then last update observation emits new value`() = testBlocking {
        // Given
        var timestampUpdate: Long? = null
        createLastUpdateScenarioWith(
            lastUpdateTimestamp = 2000,
            currentTimestamp = 3000
        )

        val testKey = "1-ORDERS-123456"

        // When
        sut.observeLastUpdate(key = testKey).onEach {
            timestampUpdate = it
        }.launchIn(this)

        // Then
        Assertions.assertThat(timestampUpdate).isNotNull()
        Assertions.assertThat(timestampUpdate).isEqualTo(2000)
    }

    private fun createLastUpdateScenarioWith(
        lastUpdateTimestamp: Long?,
        currentTimestamp: Long
    ) {
        val lastUpdatePreferences = mock<Preferences> {
            on { get(any<Preferences.Key<Long>>()) } doReturn lastUpdateTimestamp
        }

        dataStore = mock {
            on { data } doReturn flowOf(lastUpdatePreferences)
        }

        val mockDate = mock<Date> {
            on { time } doReturn currentTimestamp
        }
        currentTimeProvider = mock {
            on { currentDate() } doReturn mockDate
        }

        val selectedSite: SelectedSite = mock()

        sut = LastUpdateDataStore(
            dataStore = dataStore,
            currentTimeProvider = currentTimeProvider,
            selectedSite = selectedSite
        )
    }
}
