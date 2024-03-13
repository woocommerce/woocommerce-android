@file:Suppress("MaxLineLength")

package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.payments.StatementDescriptor
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatementDescriptorTest {

    @Test
    fun `given statement descriptor longer than 22 chars, when wrapped with StatementDescriptor, then it's shortened to the first 22 chars`() {
        // given
        val originalStatementDescriptor = "khfdsjklfhjklfhdsalkjflk;asdjflk;fjlk;sadfjlsa;fjl;fjlads;"
        assertTrue(originalStatementDescriptor.length > 22)

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals(originalStatementDescriptor.take(22), statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor contains illegal chars, when wrapped with StatementDescriptor, then illegal chars are replaced with -`() {
        // given
        val originalStatementDescriptor = "aaa<aaa>aaa'aaa\"aaa*"

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertEquals("aaa-aaa-aaa-aaa-aaa-", statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor length smaller then 5 chars, when wrapped with StatementDescriptor, then it is appended to 5 chars`() {
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
    fun `given null statement descriptor, when wrapped with StatementDescriptor, then it is set to null`() {
        // given
        val originalStatementDescriptor = null

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertNull(statementDescriptor.value)
    }

    @Test
    fun `given statement descriptor doesn't contain any letters, when wrapped with StatementDescriptor, then it is set to null`() {
        // given
        val originalStatementDescriptor = "./`;'+_)(*&^%$#@!="
        originalStatementDescriptor.filter { char ->
            char in 'A'..'Z' || char in 'a'..'z'
        }.length.also { lettersCount -> assertEquals(0, lettersCount) }

        // when
        val statementDescriptor = StatementDescriptor(originalStatementDescriptor)

        // then
        assertNull(statementDescriptor.value)
    }
}
