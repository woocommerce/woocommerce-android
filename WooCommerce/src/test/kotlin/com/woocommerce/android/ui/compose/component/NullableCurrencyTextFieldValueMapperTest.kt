package com.woocommerce.android.ui.compose.component

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class NullableCurrencyTextFieldValueMapperTest : BaseUnitTest() {

    @Test
    fun `when user enters accepted characters, then return the entered text`() {
        val sut = NullableCurrencyTextFieldValueMapper(".", 2)
        assertThat(".33").isEqualTo(sut.transformText("123", ".33"))
    }

    @Test
    fun `given no of decimals allowed is 2, when the user enters more than 2 decimals, then return the old string`() {
        val sut = NullableCurrencyTextFieldValueMapper(".", 2)
        assertThat("123.01").isEqualTo(sut.transformText("123.01", "123.011234"))
    }

    @Test
    fun `when printing value, should strip trailing zeros`() {
        val sut = NullableCurrencyTextFieldValueMapper(".", 2)
        val result = sut.printValue(BigDecimal("123.00"))
        assertThat(result).isEqualTo("123")
    }

    @Test
    fun `when user enters accepted characters with comma, then return the entered text`() {
        val sut = NullableCurrencyTextFieldValueMapper(",", 2)
        assertThat(",33").isEqualTo(sut.transformText("123", ",33"))
    }

    @Test
    fun `given decimal separator is comma when user enters more than allowed decimals then return the old string`() {
        val sut = NullableCurrencyTextFieldValueMapper(",", 2)
        assertThat("123,01").isEqualTo(sut.transformText("123,01", "123,011234"))
    }

    @Test
    fun `when printing value with comma, should strip trailing zeros`() {
        val sut = NullableCurrencyTextFieldValueMapper(",", 2)
        val result = sut.printValue(BigDecimal("123.00"))
        assertThat(result).isEqualTo("123")
    }

    @Test
    fun `when printing value with comma and decimals, should format correctly`() {
        val sut = NullableCurrencyTextFieldValueMapper(",", 2)
        val result = sut.printValue(BigDecimal("123.45"))
        assertThat(result).isEqualTo("123,45")
    }

    @Test
    fun `when transforming text with comma, should correctly map it to BigDecimal`() {
        val sut = NullableCurrencyTextFieldValueMapper(",", 2)
        val result = sut.parseText("123,45")
        assertThat(result).isEqualTo(BigDecimal("123.45"))
    }

    @Test
    fun `given decimal separator is comma when transforming text with multiple commas then normalize`() {
        val sut = NullableCurrencyTextFieldValueMapper(",", 2)
        val transformedText = sut.transformText("123,,45", "123,45")
        assertThat(transformedText).isEqualTo("123,45")
    }
}
