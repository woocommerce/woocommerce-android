package com.woocommerce.android.aiassistant.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.aiassistant.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AssistantEmptyStateSuggestionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `when suggestions are inspected, then labels match release copy and prompts are expanded`() {
        // WHEN
        val suggestions = assistantEmptyStateSuggestions()
        val labels = suggestions.map { context.getString(it.labelRes) }
        val prompts = suggestions.map { context.getString(it.promptRes) }

        // THEN
        assertThat(labels).containsExactly(
            "How's revenue this week?",
            "What's running low?",
            "Any orders need my attention?",
            "Who's new this week?",
        )
        assertThat(prompts).containsExactly(
            "How's my revenue this week? Show me total sales for this week and how it compares to last week.",
            "What's running low? List the products that are out of stock or low on inventory so I know what to " +
                "restock.",
            "Any orders that need my attention? Show me recent orders that are pending, on hold, or processing.",
            "Who's new this week? List the customers who registered or placed their first order recently.",
        )
        labels.zip(prompts).forEach { (label, prompt) ->
            assertThat(prompt).isNotEqualTo(label)
        }
        assertThat(suggestions.map { it.iconRes }).containsExactly(
            R.drawable.ic_assistant_empty_state_revenue,
            R.drawable.ic_assistant_empty_state_inventory,
            R.drawable.ic_assistant_empty_state_orders,
            R.drawable.ic_assistant_empty_state_customers,
        )
    }
}
