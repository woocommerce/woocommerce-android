package com.woocommerce.android

import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class AppPrefsTest {
    @Before
    fun setup() {
        AppPrefs.init(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
    }

    @Test
    fun whenSetLastConnectedCardReaderIdThenIdStored() {
        val readerId = "id"

        AppPrefs.setLastConnectedCardReaderId(readerId)

        assertThat(AppPrefs.getLastConnectedCardReaderId()).isEqualTo(readerId)
    }

    @Test
    fun whenRemoveLastConnectedCardReaderIdThenIdIsNull() {
        val readerId = "id"
        AppPrefs.setLastConnectedCardReaderId(readerId)

        AppPrefs.removeLastConnectedCardReaderId()

        assertThat(AppPrefs.getLastConnectedCardReaderId()).isNull()
    }
}
