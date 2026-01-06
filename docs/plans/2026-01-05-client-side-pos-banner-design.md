# Client-Side POS Banner for Non-JITM Stores

## Overview

Implement a naive client-side banner for users who can't receive JITMs (non-Jetpack connected stores). The banner promotes WooCommerce POS to eligible phone users in US/UK.

## Requirements

- Show banner on My Store screen for non-Jetpack connected stores
- Target US/UK phone users only (exclude tablets)
- Exclude existing IPP users
- Percentage-based targeting (modulo on store ID), configurable locally
- Control via local feature flag (prepare for remote flag later)
- Dismiss persists forever (single flag for dismiss OR CTA click)

## Architecture

```
JitmFragment → JitmViewModel → BannerMessageRepository
                                        │
                        ┌───────────────┴───────────────┐
                        ▼                               ▼
              JitmBannerAdapter              ClientSideBannerProvider
              (wraps JitmStoreInMemoryCache) (non-wpcom - hardcoded)
                        │
                        ▼
              JitmStoreInMemoryCache
              (unchanged)
```

Repository pattern with `BannerMessageProvider` interface. Repository decides which provider to use based on site type (Jetpack connected or not). JitmStoreInMemoryCache remains unchanged.

## New Components

### 1. BannerMessageProvider Interface (ui/jitm/)

```kotlin
interface BannerMessageProvider {
    suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse>
    suspend fun dismissMessage(messagePath: String, jitmId: String, featureClass: String): Boolean
    fun onCtaClicked(messagePath: String)
}
```

### 2. BannerMessageRepository (ui/jitm/)

```kotlin
@Singleton
class BannerMessageRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val jitmBannerAdapter: JitmBannerAdapter,
    private val clientSideBannerProvider: ClientSideBannerProvider,
) : BannerMessageProvider {
    // Delegates to JitmBannerAdapter if Jetpack connected, else ClientSideBannerProvider
}
```

### 3. JitmBannerAdapter (ui/jitm/)

```kotlin
@Singleton
class JitmBannerAdapter @Inject constructor(
    private val jitmStoreInMemoryCache: JitmStoreInMemoryCache,
) : BannerMessageProvider {
    // Wraps JitmStoreInMemoryCache, keeps it unchanged
}
```

### 4. ClientSideBannerProvider (ui/jitm/clientside/)

```kotlin
@Singleton
class ClientSideBannerProvider @Inject constructor(
    private val context: Context,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val wooPosIsScreenSizeAllowed: WooPosIsScreenSizeAllowed,
    private val dismissalStorage: ClientSideBannerDismissalStorage,
) : BannerMessageProvider
```

### 5. ClientSideBannerDismissalStorage (ui/jitm/clientside/)

```kotlin
@Singleton
class ClientSideBannerDismissalStorage @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
) {
    fun isBannerHidden(bannerId: String): Boolean
    fun hideBanner(bannerId: String)
}
```

## Targeting Logic

In `ClientSideBannerProvider.shouldShowBanner()`:

```kotlin
private fun shouldShowBanner(): Boolean {
    // 1. Feature flag
    if (!FeatureFlag.CLIENT_SIDE_POS_BANNER.isEnabled()) return false

    // 2. Phone only (screen too small for POS)
    if (wooPosIsScreenSizeAllowed()) return false

    // 3. US/UK stores only
    val countryCode = wooStore.getStoreCountryCode(site)
    if (countryCode !in listOf("US", "GB")) return false

    // 4. Not existing IPP user
    if (appPrefsWrapper.getCardReaderPreferredPlugin(...) != null) return false

    // 5. Not hidden (dismissed or CTA clicked)
    if (dismissalStorage.isBannerHidden(POS_BANNER_ID)) return false

    // 6. Percentage targeting
    if (siteId % PERCENTAGE_DIVISOR >= TARGETING_PERCENTAGE) return false

    return true
}
```

Note: Jetpack connection check is done in `BannerMessageRepository.getProvider()`, not in targeting logic.

## Banner Content

- **Title**: "Run WooCommerce POS on tablets"
- **Description**: "Take in‑person payments with WooCommerce POS. Set up on a tablet and start selling today."
- **CTA**: "Learn more" → https://woocommerce.com/in-person-payments/

## Strings

```xml
<string name="pos_client_side_banner_title">Run WooCommerce POS on tablets</string>
<string name="pos_client_side_banner_description">Take in‑person payments with WooCommerce POS. Set up on a tablet and start selling today.</string>
<string name="pos_client_side_banner_cta">Learn more</string>
```

## Feature Flag

Add to `FeatureFlag.kt`:
```kotlin
CLIENT_SIDE_POS_BANNER(false, "client-side-pos-banner")
```

## Files to Create

1. `ui/jitm/BannerMessageProvider.kt` - Interface
2. `ui/jitm/BannerMessageRepository.kt` - Orchestrates providers
3. `ui/jitm/JitmBannerAdapter.kt` - Wraps JitmStoreInMemoryCache
4. `ui/jitm/clientside/ClientSideBannerProvider.kt` - Client-side banner implementation
5. `ui/jitm/clientside/ClientSideBannerDismissalStorage.kt` - Persistence

## Files to Modify

1. `util/FeatureFlag.kt` - Add CLIENT_SIDE_POS_BANNER
2. `ui/jitm/JitmViewModel.kt` - Use BannerMessageRepository instead of JitmStoreInMemoryCache
3. `res/values/strings.xml` - Add banner strings
4. `AppPrefs.kt` / `AppPrefsWrapper.kt` - Add dismissal storage methods

## Files Unchanged

- `ui/jitm/JitmStoreInMemoryCache.kt` - No changes required
