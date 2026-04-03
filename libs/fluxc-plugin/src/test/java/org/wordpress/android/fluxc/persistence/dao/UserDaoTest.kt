package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class UserDaoTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: UserDao

    private companion object {
        val site = SiteModel().apply {
            email = "test@example.org"
            name = "Test Site"
            id = 24
        }

        val sampleUser = WCUserModel(
            localSiteId = LocalId(site.id),
            remoteUserId = RemoteId(1L),
            email = "user@example.com",
            firstName = "John",
            lastName = "Doe",
            username = "johndoe",
            roles = "administrator"
        )
    }

    @Before
    fun setUp() {
        sut = databaseRule.db.userDao
    }

    @Test
    fun `getUser returns user when it is stored`() = runTest {
        // when
        sut.upsertUser(sampleUser)

        // then
        val storedUser = sut.getUser(site.localId(), sampleUser.email)
        assertThat(storedUser).isEqualTo(sampleUser)
    }

    @Test
    fun `getUser returns null when user with different site is stored`() = runTest {
        // given
        val differentSiteUser = sampleUser.copy(
            localSiteId = LocalId(999)
        )

        // when
        sut.upsertUser(differentSiteUser)

        // then
        val storedUser = sut.getUser(site.localId(), sampleUser.email)
        assertThat(storedUser).isNull()
    }

    @Test
    fun `getUser returns null when user with different email is stored`() = runTest {
        // given
        val differentEmailUser = sampleUser.copy(email = "different@example.com")

        // when
        sut.upsertUser(differentEmailUser)

        // then
        val storedUser = sut.getUser(site.localId(), sampleUser.email)
        assertThat(storedUser).isNull()
    }

    @Test
    fun `upsertUser updates an existing user when it exists`() = runTest {
        // given
        sut.upsertUser(sampleUser)
        val updatedUser = sampleUser.copy(
            firstName = "Jane",
            lastName = "Smith",
            roles = "editor"
        )

        // when
        sut.upsertUser(updatedUser)

        // then
        val storedUser = sut.getUser(site.localId(), sampleUser.email)
        assertThat(storedUser).isEqualTo(updatedUser)
    }
}
