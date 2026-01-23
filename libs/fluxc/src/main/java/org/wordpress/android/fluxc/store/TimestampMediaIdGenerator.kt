package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class TimestampMediaIdGenerator @Inject constructor(private val clock: Clock) : MediaIdGenerator {
    override fun generate(filePath: String): LocalId {
        val combined = "$filePath:${clock.now().toEpochMilliseconds()}"
        return LocalId(combined.hashCode())
    }
}
