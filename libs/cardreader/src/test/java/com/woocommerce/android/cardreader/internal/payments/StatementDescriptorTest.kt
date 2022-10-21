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
        val originalStatementDescriptor = "aaa<aaa>aaa'aaa\"aaa"

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals("aaa-aaa-aaa-aaa-aaa", statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor contains illegal chars only then replace them with -`() {
        // given
        val originalStatementDescriptor = "<>'\""

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals("----", statementDescriptor.value)
    }
}
