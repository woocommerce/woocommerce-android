package com.woocommerce.android.cardreader.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

internal class CardReaderRemoteNsd(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val nsdManager: NsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    suspend fun advertise(
        port: Int,
        fingerprint: String,
        deviceName: String,
        siteId: Long,
    ): CardReaderRemoteNsdRegistration =
        withContext(ioDispatcher) {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = uniqueServiceName()
                serviceType = SERVICE_TYPE
                setPort(port)
                setAttribute(TXT_KEY_FINGERPRINT, fingerprint)
                setAttribute(TXT_KEY_VERSION, PROTOCOL_VERSION)
                setAttribute(TXT_KEY_DEVICE_NAME, deviceName)
                setAttribute(TXT_KEY_SITE_ID, siteId.toString())
            }

            val listener = object : NsdManager.RegistrationListener {
                private var registrationContinuation: CancellableContinuation<NsdServiceInfo>? = null

                fun bind(cont: CancellableContinuation<NsdServiceInfo>) {
                    registrationContinuation = cont
                }

                override fun onServiceRegistered(registered: NsdServiceInfo) {
                    registrationContinuation?.takeIf { it.isActive }?.resume(registered)
                    registrationContinuation = null
                }

                override fun onRegistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                    val cont = registrationContinuation
                    registrationContinuation = null
                    if (cont != null && cont.isActive) {
                        cont.resumeWithException(IOException("NSD registration failed (errorCode=$errorCode)"))
                    } else {
                        Log.w(TAG, "NSD registration failed (errorCode=$errorCode)")
                    }
                }

                override fun onServiceUnregistered(info: NsdServiceInfo?) {
                    Log.d(TAG, "NSD service unregistered: ${info?.serviceName}")
                }

                override fun onUnregistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                    Log.w(TAG, "NSD unregistration failed (errorCode=$errorCode)")
                }
            }

            val registered = suspendCancellableCoroutine<NsdServiceInfo> { cont ->
                listener.bind(cont)
                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
                cont.invokeOnCancellation {
                    runCatching { nsdManager.unregisterService(listener) }
                }
            }

            CardReaderRemoteNsdRegistration(registered.serviceName) {
                runCatching { nsdManager.unregisterService(listener) }
            }
        }

    fun discover(): Flow<CardReaderRemoteNsdEvent> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "NSD discovery started: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(IOException("NSD start discovery failed (errorCode=$errorCode)"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "NSD stop discovery failed (errorCode=$errorCode)")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                resolve(service) { resolved -> trySend(CardReaderRemoteNsdEvent.Found(resolved)) }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                trySend(CardReaderRemoteNsdEvent.Lost(service.serviceName))
            }
        }

        runCatching {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }.onFailure { close(it) }

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
    }.flowOn(ioDispatcher)

    private fun resolve(service: NsdServiceInfo, emit: (CardReaderRemoteResolvedHost) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            resolveWithCallback(service, emit)
        } else {
            resolveLegacy(service, emit)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun resolveWithCallback(service: NsdServiceInfo, emit: (CardReaderRemoteResolvedHost) -> Unit) {
        val executor = Executor { it.run() }
        val callback = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                Log.w(TAG, "NSD service info callback registration failed (errorCode=$errorCode)")
            }

            override fun onServiceInfoCallbackUnregistered() = Unit

            override fun onServiceLost() {
                nsdManager.unregisterServiceInfoCallback(this)
            }

            override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                val resolved = serviceInfo.toResolvedHost()
                nsdManager.unregisterServiceInfoCallback(this)
                if (resolved != null) emit(resolved)
            }
        }
        nsdManager.registerServiceInfoCallback(service, executor, callback)
    }

    private fun resolveLegacy(service: NsdServiceInfo, emit: (CardReaderRemoteResolvedHost) -> Unit) {
        @Suppress("DEPRECATION")
        nsdManager.resolveService(
            service,
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    Log.w(TAG, "NSD resolve failed (errorCode=$errorCode)")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    serviceInfo.toResolvedHost()?.let(emit)
                }
            }
        )
    }

    private fun NsdServiceInfo.toResolvedHost(): CardReaderRemoteResolvedHost? {
        val version = attributes[TXT_KEY_VERSION]?.toString(Charsets.UTF_8)
        val fingerprint = attributes[TXT_KEY_FINGERPRINT]?.toString(Charsets.UTF_8)
        val host = firstHost()
        val siteId = attributes[TXT_KEY_SITE_ID]?.toString(Charsets.UTF_8)?.toLongOrNull()
        val isValid = version == PROTOCOL_VERSION && !fingerprint.isNullOrEmpty() && host != null && siteId != null
        if (!isValid) {
            Log.w(
                TAG,
                "Dropping NSD service $serviceName " +
                    "(version=$version, fingerprintLen=${fingerprint?.length ?: 0}, host=$host, siteId=$siteId)"
            )
            return null
        }
        val deviceName = attributes[TXT_KEY_DEVICE_NAME]?.toString(Charsets.UTF_8)
        return CardReaderRemoteResolvedHost(
            name = serviceName,
            host = host,
            port = port,
            fingerprintBase64 = fingerprint,
            version = version,
            deviceName = deviceName,
            siteId = siteId,
        )
    }

    private fun NsdServiceInfo.firstHost(): InetAddress? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hostAddresses.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            host
        }

    private fun uniqueServiceName(): String {
        val suffix = Random.nextInt(0, SUFFIX_RANGE_EXCLUSIVE)
            .toString(radix = HEX_RADIX)
            .padStart(SUFFIX_LENGTH, '0')
        return "$SERVICE_NAME_PREFIX-$suffix"
    }

    companion object {
        private const val TAG = "CardReaderRemoteNsd"
        const val SERVICE_TYPE = "_wooposremote._tcp."
        const val TXT_KEY_FINGERPRINT = "fp"
        const val TXT_KEY_VERSION = "ver"
        const val TXT_KEY_DEVICE_NAME = "name"
        const val TXT_KEY_SITE_ID = "siteId"
        const val PROTOCOL_VERSION = "1"
        private const val SERVICE_NAME_PREFIX = "woopos-remote"
        private const val SUFFIX_RANGE_EXCLUSIVE = 0x10000
        private const val HEX_RADIX = 16
        private const val SUFFIX_LENGTH = 4
    }
}

internal class CardReaderRemoteNsdRegistration internal constructor(
    val serviceName: String,
    private val onClose: () -> Unit,
) : AutoCloseable {
    override fun close() {
        onClose()
    }
}

internal data class CardReaderRemoteResolvedHost(
    val name: String,
    val host: InetAddress,
    val port: Int,
    val fingerprintBase64: String,
    val version: String,
    val deviceName: String?,
    val siteId: Long,
)

internal sealed class CardReaderRemoteNsdEvent {
    data class Found(val host: CardReaderRemoteResolvedHost) : CardReaderRemoteNsdEvent()
    data class Lost(val serviceName: String) : CardReaderRemoteNsdEvent()
}
