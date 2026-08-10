package com.woocommerce.android.ui.products.details

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductDetailTitleFieldStateTest {
    @Test
    fun `given the title is unfocused, when the external text changes, then the field is updated`() {
        // GIVEN
        val currentState = givenTitleFieldState(text = ORIGINAL_TITLE)

        // WHEN
        val result = synchronizeTitleFieldState(
            externalText = UPDATED_TITLE,
            isFocused = false,
            shouldFocus = false,
            currentState = currentState,
        )

        // THEN
        assertThat(result.value).isEqualTo(TextFieldValue(UPDATED_TITLE, TextRange.Zero))
    }

    @Test
    fun `given an in-progress focused edit, when the external text changes, then the edit is preserved`() {
        // GIVEN
        val currentState = givenTitleFieldState(
            text = EDITED_TITLE,
            selection = EDIT_SELECTION,
            restoreFocus = true,
            hasEditedWhileFocused = true,
        )

        // WHEN
        val result = synchronizeTitleFieldState(
            externalText = UPDATED_TITLE,
            isFocused = true,
            shouldFocus = false,
            currentState = currentState,
        )

        // THEN
        assertThat(result).isEqualTo(currentState)
    }

    @Test
    fun `given focus should be restored after recreation, when synchronized, then the edit and cursor are preserved`() {
        // GIVEN
        val currentState = givenTitleFieldState(
            text = EDITED_TITLE,
            selection = EDIT_SELECTION,
            restoreFocus = true,
            hasEditedWhileFocused = true,
        )

        // WHEN
        val result = synchronizeTitleFieldState(
            externalText = UPDATED_TITLE,
            isFocused = false,
            shouldFocus = false,
            currentState = currentState,
        )

        // THEN
        assertThat(result).isEqualTo(currentState)
    }

    @Test
    fun `given the title should focus, when the external text changes, then the cursor moves to the end`() {
        // GIVEN
        val currentState = givenTitleFieldState(text = ORIGINAL_TITLE)

        // WHEN
        val result = synchronizeTitleFieldState(
            externalText = UPDATED_TITLE,
            isFocused = false,
            shouldFocus = true,
            currentState = currentState,
        )

        // THEN
        assertThat(result.value).isEqualTo(TextFieldValue(UPDATED_TITLE, TextRange(UPDATED_TITLE.length)))
    }

    private fun givenTitleFieldState(
        text: String,
        selection: TextRange = TextRange.Zero,
        restoreFocus: Boolean = false,
        hasEditedWhileFocused: Boolean = false,
    ) = ProductDetailTitleFieldState(
        value = TextFieldValue(text, selection),
        restoreFocus = restoreFocus,
        hasEditedWhileFocused = hasEditedWhileFocused,
    )

    private companion object {
        const val ORIGINAL_TITLE = "Original title"
        const val UPDATED_TITLE = "Updated title"
        const val EDITED_TITLE = "Edited title"
        val EDIT_SELECTION = TextRange(2, 7)
    }
}
