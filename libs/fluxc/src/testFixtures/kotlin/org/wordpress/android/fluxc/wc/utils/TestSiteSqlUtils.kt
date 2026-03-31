package org.wordpress.android.fluxc.wc.utils

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.SiteSqlUtils

object TestSiteSqlUtils {
    private val accountStorePersistence: AccountStorePersistence = mock {
        on { getDefaultAccount() } doReturn AccountModel().apply { userId = 1L }
    }
    val siteSqlUtils = SiteSqlUtils(accountStorePersistence)
}
