# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Build Commands

### Development Build Commands
```bash
# Build debug APK for development (preferred variant)
./gradlew assembleWasabiDebug

# Install debug APK to connected device/emulator
./gradlew installWasabiDebug

# Build release/beta APK (production variant)
./gradlew assembleVanillaDebug
./gradlew assembleVanillaRelease
```

### Testing Commands
```bash
# Run unit tests
./gradlew :WooCommerce:testWasabiDebugUnitTest

# Run Android instrumentation tests
./gradlew :WooCommerce:connectedWasabiDebugAndroidTest

# Run Firebase Test Lab tests
./gradlew runFlank
```

### Code Quality Commands
```bash
# Run Detekt linting (required before commits). --auto-correct param attempts to modify code by detekt, however we need to re-run the check to see if it succeeded.
./gradlew detektAll --auto-correct

# Clean build artifacts
./gradlew clean
```

## Project Architecture

### Module Structure
This is a multi-module Android project with the following key modules:

- **WooCommerce**: Main application module containing UI, business logic, and core features
- **WooCommerce-Wear**: Wear OS companion app module
- **libs/fluxc**: Data layer using FluxC architecture (WordPress networking and data management)
- **libs/fluxc-plugin**: WooCommerce-specific FluxC plugin for store operations
- **libs/cardreader**: Payment card reader functionality
- **libs/login**: WordPress.com login flows
- **libs/commons**: Shared utilities and common code

### Build Variants & Flavors
The project uses three main product flavors:
- **wasabi**: Development builds with `com.woocommerce.android.dev` package (default for local development)
- **vanilla**: Production builds with `com.woocommerce.android` package (for releases)
- **jalapeno**: PR/CI builds with `com.woocommerce.android.prealpha` package

### Configuration Requirements
Before building, you must:
1. Copy `defaults.properties` to `~/.configure/woocommerce-android/secrets/secrets.properties`
2. Set up OAuth2 credentials (`wc.oauth.app_id` and `wc.oauth.app_secret`) in secrets.properties
3. Ensure `google-services.json` exists (example file will be auto-copied if missing)

### Technology Stack
- **Language**: Kotlin with Java 11 target
- **UI**: Mix of traditional Android Views and Jetpack Compose (migration in progress)
- **Architecture**: FluxC (Flux pattern) for data layer, MVVM for presentation
- **DI**: Dagger Hilt for dependency injection
- **Navigation**: Android Navigation Component with Safe Args
- **Networking**: WordPress.com REST APIs and WooCommerce APIs
- **Database**: Room with FluxC data layer
- **Testing**: JUnit, Mockito, Espresso, Firebase Test Lab integration

### Key Libraries & Integrations
- **FluxC**: WordPress data layer and networking
- **WordPress/Automattic**: Login flows, utils, Aztec editor, media picker
- **Payment Processing**: Stripe Terminal for card readers, WooPayments integration
- **Analytics**: Automattic Tracks for event tracking
- **ML Kit**: Barcode scanning and text recognition
- **Compose**: Material3, Navigation Compose, Hilt Navigation Compose

## Development Notes

### Code Quality
- Detekt is used for Kotlin linting and must pass before commits
- All warnings are treated as errors in Kotlin compilation
- Proguard is enabled for release builds (minification only, no obfuscation)

### Authentication
The app supports two authentication methods:
1. WordPress.com OAuth2 (requires app credentials)
2. Application Passwords for self-hosted sites

### Performance
- Remote build cache is available for faster builds (configure in `developer.properties`)
- Requires Java 17 Amazon Corretto for optimal build cache efficiency
- Build optimization and measurement tools are configured

### Testing Strategy
- Unit tests using JUnit and Mockito
- UI tests using Espresso and UI Automator
- Firebase Test Lab integration via Fladle plugin
- Screenshot testing with Fastlane Screengrab
