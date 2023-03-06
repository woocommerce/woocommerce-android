package com.woocommerce.android.apifaker.models

import androidx.room.Embedded
import androidx.room.Relation

internal data class EndpointWithResponse(
    @Embedded val endpoint: Endpoint,
    @Relation(
        parentColumn = "id",
        entityColumn = "endpointId"
    )
    val response: FakeResponse
)
