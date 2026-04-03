package org.wordpress.android.fluxc.wc.user

import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserMapper
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.user.WCUserRestClient
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCUserStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCUserStoreTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val restClient = mock<WCUserRestClient>()
    private val site = SiteModel().apply { id = 321 }
    private val errorSite = SiteModel().apply { id = 123 }
    private lateinit var store: WCUserStore

    private val sampleUserApiResponse = WCUserTestUtils.generateSampleUApiResponse()
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    @Before
    fun setUp() {
        store = WCUserStore(
            restClient,
            initCoroutineEngine(),
            databaseRule.db.userDao
        )
    }

    @Test
    fun `fetch user role`() = test {
        val result = fetchUserRole()

        val userRole = WCUserMapper.map(sampleUserApiResponse!!, site)
        assertThat(result.model).isEqualTo(userRole)

        val invalidRequestResult = store.fetchUserRole(errorSite)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get user role from db`() = test {
        fetchUserRole()
        val userRole = WCUserMapper.map(sampleUserApiResponse!!, site)

        val savedUser = store.getUserByEmail(site, userRole.email)
        assertThat(savedUser).isEqualTo(userRole)
    }

    private suspend fun fetchUserRole(): WooResult<WCUserModel> {
        val fetchUserRolePayload = WooPayload(sampleUserApiResponse)
        whenever(restClient.fetchUserInfo(site)).thenReturn(fetchUserRolePayload)
        whenever(restClient.fetchUserInfo(errorSite)).thenReturn(WooPayload(error))
        return store.fetchUserRole(site)
    }
}
