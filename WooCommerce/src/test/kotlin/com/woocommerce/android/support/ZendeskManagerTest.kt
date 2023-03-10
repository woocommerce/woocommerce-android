package com.woocommerce.android.support

import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.mockito.kotlin.mock

internal class ZendeskManagerTest : BaseUnitTest() {
    private lateinit var sut: ZendeskManager

    @Before
    fun setup() {
        sut = ZendeskManager(
            zendeskSettings = mock(),
            siteStore = mock(),
            dispatchers = mock()
        )
    }
}
