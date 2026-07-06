package org.wordpress.android.fluxc.persistence

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.dao.AccountDao
import org.wordpress.android.fluxc.persistence.entity.AccountEntity

@RunWith(RobolectricTestRunner::class)
class AccountDaoTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var dao: AccountDao

    @Before
    fun setUp() {
        dao = wpDatabaseRule.db.accountDao()
    }

    @Test
    fun `given empty database, when get default account, then returns null`() = runTest {
        val result = dao.getDefaultAccount()

        assertThat(result).isNull()
    }

    @Test
    fun `given account inserted, when get default account, then returns account`() = runTest {
        val account = createTestAccount()
        dao.upsert(account)

        val result = dao.getDefaultAccount()

        assertThat(result).isEqualTo(account)
    }

    @Test
    fun `given account doesn't exist, when upsert, then account is inserted`() = runTest {
        val account = createTestAccount()

        dao.upsert(account)

        val result = dao.getDefaultAccount()
        assertThat(result).isEqualTo(account)
    }

    @Test
    fun `given account exists, when upsert with same id, then account is updated`() = runTest {
        val account = createTestAccount()
        dao.upsert(account)

        val updated = account.copy(displayName = "Updated Name")
        dao.upsert(updated)

        val result = dao.getDefaultAccount()
        assertThat(result).isEqualTo(updated)
    }

    @Test
    fun `given account exists, when update username, then only username changes`() = runTest {
        val account = createTestAccount(userName = "oldUsername", firstName = "John")
        dao.upsert(account)

        dao.updateDefaultUsername("newUsername")

        val result = dao.getDefaultAccount()
        assertThat(result?.userName).isEqualTo("newUsername")
        assertThat(result?.firstName).isEqualTo("John")
    }

    @Test
    fun `given account exists, when delete account, then account is removed`() = runTest {
        val account = createTestAccount()
        dao.upsert(account)

        dao.deleteDefaultAccount()

        val result = dao.getDefaultAccount()
        assertThat(result).isNull()
    }

    /* HELPER */

    private fun createTestAccount(
        userName: String = "testUser",
        firstName: String = "Test",
    ) = AccountEntity(
        id = AccountDao.DEFAULT_ACCOUNT_LOCAL_ID,
        userName = userName,
        userId = 123L,
        displayName = "Test User",
        profileUrl = "https://profile.url",
        avatarUrl = "https://avatar.url",
        primarySiteId = 456L,
        emailVerified = true,
        siteCount = 5,
        visibleSiteCount = 3,
        email = "test@example.com",
        hasUnseenNotes = false,
        firstName = firstName,
        lastName = "User",
        aboutMe = "About me",
        date = "2024-01-01",
        newEmail = "",
        pendingEmailChange = false,
        twoStepEnabled = true,
        webAddress = "https://web.address",
        tracksOptOut = false,
        crashReportingOptOut = null,
        usernameCanBeChanged = true,
    )
}
