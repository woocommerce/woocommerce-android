package com.woocommerce.android.apifaker.models

import androidx.room.Embedded
import androidx.room.Relation

internal data class MockedEndpoint(
    @Embedded val request: Request,
    @Relation(
        parentColumn = "id",
        entityColumn = "endpointId"
    )
    val response: Response
)
