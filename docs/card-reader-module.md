## Warning
If reading this you find anything is not up-to-date, please fix it or report to the team if not sure how to do that

## Useful Links To Get Started
* Store Setup for Card Present Payment Testing - P91TBi-4BH-p2
* Stripe terminal In-Person Payments - https://stripe.com/docs/terminal
* Our very own P2 - dfdoF-p2
* Before creating of taptopay P2 we were using general WooMobile P2 with tag #card-present 91TBi-p2
* WCPay Server repo. If something goes wrong with the API most probably issue has to be created there - https://github.com/Automattic/woocommerce-payments-server
* Stripe Android SDK github repo - https://github.com/stripe/stripe-android
* Designs - vaw3DvewbUwuQnPXpdNGGH-fi-8%3A0

## Dictionary
* **CPP** - Card Present Payments
* **Stripe** - Third party company we use as payments processing for In-Person Payments
* **WCPay** - Or WooCommerce Payments is a plugin for WooCommerce (which itself is a plugin for Wordpress) that contains integrations with different payments platform including Stripe
* **Card Reader** - At the moment only one reader is supported *BBPOS Chipper 2X BT* (ask Aaron to order your own reader)
* **COD** - Cash On Delivery. Paying with a card is also "cash", so CPP is COD 🤷
* **POS** - Point Of Sale
* **KYC** - Know Your Customer
* **Card Present Payments / In-Person Payments / Card Reader Payments** - are interchangeable terms in our internal documentation and code, but only “In-Person Payments” is used in user facing features.

## CardReader Module

This module provides an abstraction from a provider-specific SDK to connect and accept payments using card readers. The module currently supports only Stripe Terminal SDK for Android and most of this documentation is specific only to this SDK. However, the module tries to hide as many details as possible from the client app.

### Public Interfaces
`CardReaderManager` - The main interface for communicating with a card reader from the client application.
`CardReaderStore` - Abstraction of some network requests made by the `CardReaderModule` (only some since Stripe Terminal SDK performs some requests internally). The abstraction was introduced so the client app can decide how to handle network requests (WCAndroid delegates all requests to FluxC).
`CardReaderManagerFactory` - Simple implementation of a factory which creates a concrete implementation of CardReaderManager.
### Model Objects
`CardReader` - Abstraction of a card reader with basic info such as an id and a battery level.
`CardReaderDiscoveryEvents` - Events that can be emitted during bluetooth discovery.
`CardPaymentStatus` - Events that can be emitted when accepting a payment.
`CardReaderStatus` - Current state of a connection status to a card reader.
`PaymentInfo` - Model containing data necessary to initiate a payment.
`SoftwareUpdateAvailability` - Events that can be emitted during a check for software updates.
`SoftwareUpdateStatus` - Events that can be emitted during an ongoing software update.

### Overview
#### Initialization
The first step a client app needs to take is initialize the CardReaderModule. `CardReaderManager` provides a simple `initialize(..)` method which takes care of the initialization.

#### Connecting to a card reader
The `CardReaderManager` provides `discoverReaders(..)` method that starts a discovery of nearby readers. The discovery process is mandatory no matter if the app was connected to a card reader before or not.
When a card reader is discovered, the client app is informed and can invoke `connectToReader(..)` method to connect to the card reader. This method will work only when the discovery is still in progress and a recently found reader is provided (it is not allowed to cache any discovered readers by the client app).
When a successful connection to a card reader is established, the SDK invokes `CardReaderStore.getConnectionToken(..)`. The client app is responsible for obtaining the token and passing it back to the SDK. The token is required to pair the reader with the user’s Stripe account. The card reader is ready to accept payments now.

Most of the related code is encapsulated in ConnectionManager and its dependencies.

#### Accepting Payments
The payment process is consisted of 4 steps:
1. *Create PaymentIntent* - the payment data is set and the payment is initialized.
2. *Collect Payment* - The user needs to tap/swipe/insert a card into the card reader.
3. *Process Payment* - The Stripe Terminal SDK verifies the payment and processes it. The SDK internally passes the PaymentIntent into Stripe’s backend. The owner of the card should see the payment in “on-hold” state until the payment is captured or canceled (or it times out after +- N days).
4. *Capture Payment* - the payment is captured and the user gets charged. Offline payments are not currently supported.

Most of the related code is encapsulated in PaymentManager and its dependencies.

#### Switching accounts or stores
In order to switch accounts or to just reset the integration with the Stripe Terminal SDK it is necessary to first disconnect from connected readers, via a call to `disconnect()` and, after that call is completed, call `clear()`.

The method clear() will reset the integration, clear all the caches, and will make the Stripe Terminal ready to fetch a new connection token.

P.S. This document was inspired by and some parts even copied from https://github.com/woocommerce/woocommerce-ios/blob/develop/docs/HARDWARE.md.
















