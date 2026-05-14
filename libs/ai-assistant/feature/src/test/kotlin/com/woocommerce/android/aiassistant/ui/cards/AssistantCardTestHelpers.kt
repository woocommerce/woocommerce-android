package com.woocommerce.android.aiassistant.ui.cards

internal fun AssistantCard.Stats.metric(type: AssistantCard.Stats.MetricType): AssistantCard.Stats.Metric =
    metrics.single { it.type == type }
