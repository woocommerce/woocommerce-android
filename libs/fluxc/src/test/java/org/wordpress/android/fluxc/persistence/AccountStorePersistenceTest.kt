package org.wordpress.android.fluxc.persistence

import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.AccountModel

@RunWith(RobolectricTestRunner::class)
class AccountStorePersistenceTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: AccountStorePersistence

    @Before
    fun setUp() {
        sut = AccountStorePersistence(
            wpDatabaseRule.db,
            AccountMapper()
        )
    }

    @Test
    fun `given empty database, when get default account, then returns null`() {
        val result = sut.getDefaultAccount()

        assertThat(result).isNull()
    }

    @Test
    fun `given account inserted, when get default account, then returns account`() {
        val account = createTestAccount()
        sut.insertOrUpdateDefaultAccount(account)

        val result = sut.getDefaultAccount()

        assertThat(result).isNotNull()
        assertThat(result?.userId).isEqualTo(account.userId)
        assertThat(result?.displayName).isEqualTo(account.displayName)
    }

    @Test
    fun `given account inserted, when insert again, then account is updated`() {
        val account = createTestAccount()
        sut.insertOrUpdateDefaultAccount(account)

        account.displayName = "Updated Name"
        sut.insertOrUpdateDefaultAccount(account)

        val result = sut.getDefaultAccount()
        assertThat(result?.displayName).isEqualTo("Updated Name")
    }

    @Test
    fun `given account inserted, when update username, then only username changes`() {
        val account = createTestAccount()
        account.userName = "oldUsername"
        account.firstName = "John"
        sut.insertOrUpdateDefaultAccount(account)

        sut.updateUsername("newUsername")

        val result = sut.getDefaultAccount()
        assertThat(result?.userName).isEqualTo("newUsername")
        assertThat(result?.firstName).isEqualTo("John")
    }

    @Test
    fun `given account inserted, when delete account, then account is removed`() {
        val account = createTestAccount()
        sut.insertOrUpdateDefaultAccount(account)

        sut.deleteAccount()

        assertThat(sut.getDefaultAccount()).isNull()
    }

    /* HELPER */

    private fun createTestAccount() = AccountModel().apply {
        userId = 123L
        displayName = "Test User"
        userName = "testuser"
        email = "test@example.com"
        firstName = "Test"
        lastName = "User"
        primarySiteId = 456L
    }
}
