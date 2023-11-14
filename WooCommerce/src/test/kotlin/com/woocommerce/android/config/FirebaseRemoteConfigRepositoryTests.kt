package com.woocommerce.android.config

import android.app.Activity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseRemoteConfigRepositoryTests : BaseUnitTest() {
    private val remoteConfig = mock<FirebaseRemoteConfig>()

    lateinit var repository: FirebaseRemoteConfigRepository

    private val testKey = "key"
    private val testValue = "value"

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        @Suppress("UNCHECKED_CAST")
        whenever(remoteConfig.setDefaultsAsync(any<Map<String, Any>>()))
            .thenReturn(StaticTask(null))

        repository = FirebaseRemoteConfigRepository(
            remoteConfig = remoteConfig
        )
    }

    @Test
    fun `when fetch config is called, then fetch remote config`() = testBlocking {
        setup {
            whenever(remoteConfig.fetchAndActivate())
                .thenReturn(StaticTask(true))
        }

        repository.fetchRemoteConfig()

        verify(remoteConfig).fetchAndActivate()
    }

    @Test
    fun `given there a new changes, when remote config is fetched, then notify observers`() = testBlocking {
        setup {
            whenever(remoteConfig.fetchAndActivate())
                .thenReturn(StaticTask(true))
            whenever(remoteConfig.getString(testKey))
                .thenReturn(testValue)
        }

        val values = repository.observeStringRemoteValue(testKey).runAndCaptureValues {
            repository.fetchRemoteConfig()
        }

        assertThat(values.size).isEqualTo(2)
    }

    @Test
    fun `given there are no new changes, when remote config is fetched, then skip notifying observers`() =
        testBlocking {
            setup {
                whenever(remoteConfig.fetchAndActivate())
                    .thenReturn(StaticTask(false))
                whenever(remoteConfig.getString(testKey))
                    .thenReturn(testValue)
            }

            val values = repository.observeStringRemoteValue(testKey).runAndCaptureValues {
                repository.fetchRemoteConfig()
            }

            assertThat(values.size).isEqualTo(1)
        }

    @Test
    fun `when remote config is initiated, then expose a pending fetch status`() = testBlocking {
        setup()

        val status = repository.fetchStatus.first()

        assertThat(status).isEqualTo(RemoteConfigFetchStatus.Pending)
    }

    @Test
    fun `when remote config fetch succeeds, then expose a success fetch status`() = testBlocking {
        setup {
            whenever(remoteConfig.fetchAndActivate())
                .thenReturn(StaticTask(false))
        }

        val status = repository.fetchStatus.runAndCaptureValues {
            repository.fetchRemoteConfig()
        }.last()

        assertThat(status).isEqualTo(RemoteConfigFetchStatus.Success)
    }

    @Test
    fun `when remote config fetch fails, then expose a failure fetch status`() = testBlocking {
        setup {
            whenever(remoteConfig.fetchAndActivate())
                .thenReturn(StaticTask(false, Exception()))
        }

        val status = repository.fetchStatus.runAndCaptureValues {
            repository.fetchRemoteConfig()
        }.last()

        assertThat(status).isEqualTo(RemoteConfigFetchStatus.Failure)
    }
}

class StaticTask<T>(private val value: T, private val exception: Exception? = null) : Task<T>() {
    override fun addOnCompleteListener(p0: OnCompleteListener<T>): Task<T> {
        p0.onComplete(this)
        return this
    }

    override fun addOnCompleteListener(p0: Executor, p1: OnCompleteListener<T>): Task<T> {
        return addOnCompleteListener(p1)
    }

    override fun addOnFailureListener(p0: OnFailureListener): Task<T> {
        exception?.let { p0.onFailure(it) }
        return this
    }

    override fun addOnFailureListener(p0: Executor, p1: OnFailureListener): Task<T> = addOnFailureListener(p1)

    override fun addOnFailureListener(p0: Activity, p1: OnFailureListener): Task<T> = addOnFailureListener(p1)

    override fun addOnSuccessListener(p0: OnSuccessListener<in T>): Task<T> {
        p0.onSuccess(value)
        return this
    }

    override fun addOnSuccessListener(p0: Executor, p1: OnSuccessListener<in T>): Task<T> = addOnSuccessListener(p1)

    override fun addOnSuccessListener(p0: Activity, p1: OnSuccessListener<in T>): Task<T> = addOnSuccessListener(p1)

    override fun getException(): Exception? = exception

    override fun getResult(): T = value

    override fun <X : Throwable?> getResult(p0: Class<X>): T = value

    override fun isComplete(): Boolean = true

    override fun isSuccessful(): Boolean = true

    override fun isCanceled(): Boolean = false
}
