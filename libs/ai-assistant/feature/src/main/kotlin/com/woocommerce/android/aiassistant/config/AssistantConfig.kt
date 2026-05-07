package com.woocommerce.android.aiassistant.config

/**
 * Pins the completion stack used at runtime: model id, system prompt version,
 * and tool catalog version. Collapsing the triple into one named-constant
 * object makes a version bump reviewable in a single diff.
 *
 * Release rules:
 *  - Bumping [MODEL_ID] is release-blocking and gated on the regression suite
 *    (see Linear ticket WOOMOB-2922).
 *  - Bumping [PROMPT_VERSION] or [TOOL_CATALOG_VERSION] triggers the same
 *    regression suite. Prompt and catalog edits change model behavior as much
 *    as a model swap can.
 */
internal object AssistantConfig {
    const val MODEL_ID: String = "gpt-4o-mini"
    const val PROMPT_VERSION: String = "v1.1.0"
    const val TOOL_CATALOG_VERSION: String = "v1.1.0"
    const val FEATURE_NAME: String = "woo-ai-assistant"

    const val completionStack: String =
        "model=$MODEL_ID;prompt=$PROMPT_VERSION;catalog=$TOOL_CATALOG_VERSION"
}
