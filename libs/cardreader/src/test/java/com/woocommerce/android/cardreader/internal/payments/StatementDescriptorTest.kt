package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.payments.StatementDescriptor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementDescriptorTest {

    @Test
    fun `given statement descriptor longer than 22 chars first 22 chars should be returned`() {
        // given
        val originalStatementDescriptor = "khfdsjklfhjklfhdsalkjflk;asdjflk;fjlk;sadfjlsa;fjl;fjlads;"
        assertTrue(originalStatementDescriptor.length > 22)

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals(originalStatementDescriptor.take(22), statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor contains illegal chars then replace them with -`() {
        // given
        val originalStatementDescriptor = "aaa<aaa>aaa'aaa\"aaa*"

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals("aaa-aaa-aaa-aaa-aaa-", statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor contains illegal chars only then replace them with -`() {
        // given
        val originalStatementDescriptor = "<>'\"*"

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals("-----", statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor size smaller than 5 chars it should be appended to at least 5 chars`() {
        // given
        val originalStatementDescriptor = "khf"
        assertTrue(originalStatementDescriptor.length < 5)

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals(5, statementDescriptor.value?.length)
        assertEquals("$originalStatementDescriptor--", statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor null it should be transformed to 5-char long default text`() {
        // given
        val originalStatementDescriptor = null

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals("-----", statementDescriptor.value)
    }
}
