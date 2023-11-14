package com.woocommerce.android.ui.prefs.privacy.banner.domain

import com.woocommerce.android.ui.prefs.privacy.GeoRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class IsUsersCountryGdprCompliantTest : BaseUnitTest() {

    private val geoRepository: GeoRepository = mock()

    private val sut: IsUsersCountryGdprCompliant = IsUsersCountryGdprCompliant(
        geoRepository = geoRepository,
    )

    @Test
    fun `given user has EU ip, when asked for GDPR compliance, then show the banner`(): Unit =
        runBlocking {
            // given
            geoRepository.stub {
                onBlocking { fetchCountryCode() }.thenReturn(Result.success("DE"))
            }

            // then
            assertThat(sut()).isTrue
        }

    @Test
    fun `given user has non-EU ip, when asked for GDPR compliance, then do not show the banner`(): Unit =
        runBlocking {
            // given
            geoRepository.stub {
                onBlocking { fetchCountryCode() }.thenReturn(Result.success("US"))
            }

            // then
            assertThat(sut()).isFalse
        }

    @Test
    fun `given geo API returns error, when asked for GDPR compliance, then do not show the banner`(): Unit =
        runBlocking {
            // given
            geoRepository.stub {
                onBlocking { fetchCountryCode() }.thenReturn(Result.failure(Exception()))
            }

            // then
            assertThat(sut()).isFalse
        }

    @Test
    fun `given geo API returns empty value, when asked for GDPR compliance, then do not show the banner`(): Unit =
        runBlocking {
            // given
            geoRepository.stub {
                onBlocking { fetchCountryCode() }.thenReturn(Result.success(""))
            }

            // then
            assertThat(sut()).isFalse
        }
}
