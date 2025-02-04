package org.wordpress.android.fluxc.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

/**
 * A utility class that handles polling, it allows polling a request, with a defined delay between the requests
 * and automatic retrying on errors, the logic is as follows:
 * 1. Invoke the request.
 * 2. If the request fails, retry for the number of times specified by maxRetries after the specified delay
 * 3. If the request succeeds, check the predicate against the result
 * 4. If it's true, then return the result.
 * 5. If it's false, repeat from 1.
 *
 * @param delayInMs: the delay between requests in milliseconds
 * @param maxRetries: the maximum number of retries when an error occurs
 */
class Poller(private val delayInMs: Long, private val maxRetries: Int) {
    suspend fun <T : WooPayload<*>> poll(
        request: suspend () -> T,
        predicate: (T) -> Boolean
    ): T {
        return pollInternal(request)
                .filter { predicate.invoke(it) }
                .first()
    }

    private suspend fun <T : WooPayload<*>> pollInternal(
        request: suspend () -> T
    ): Flow<T> {
        var retries = 0
        return flow {
            while (retries < maxRetries) {
                val result = request.invoke()
                if (result.isError) {
                    retries++
                    if (retries == maxRetries) {
                        emit(result)
                    }
                } else {
                    retries = 0
                    emit(result)
                }
                delay(delayInMs)
            }
        }
    }
}
