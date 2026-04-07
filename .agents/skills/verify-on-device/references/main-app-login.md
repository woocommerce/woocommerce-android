# Login Flow

Uses Fragment navigation inside `LoginActivity`. Multiple paths depending on login method.
Logcat events are prefixed with `woocommerceandroid_`.

## Screen Detection

| Screen | How to Detect |
|--------|---------------|
| Prologue | text: "Log in with WordPress.com" and "Log in with site address" |
| WPCom Email | text: email input field, WooCommerce/Jetpack logo |
| Password | text: password input field, "Forgot password?" link |
| 2FA / Verification Code | text: "Enter verification code" or OTP input |
| Magic Link Sent | text: "Check your email" or "Open email client" |
| Site Address | text: "Enter your store URL" |
| Site Credentials | text: "Enter credentials for site:" with username/password fields |
| Site Picker | store list with "Continue" button |
| Account Mismatch | text: account mismatch error with avatar |
| Discovery Error | error message with "Try again" button |
| Jetpack Activation | text about Jetpack with "Continue" or "Install Jetpack" button |
| Not WordPress Error | dialog: "This site is not a WordPress site" |

## Workflows

### Happy Path: WPCom Email + Password

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Log in with WordPress.com" | id: `buttonLoginWpcom` | |
| 2 | Enter email | email text field | `login_email_form_viewed` |
| 3 | Tap Continue | continue button | |
| 4 | Enter password | password text field | `login_password_form_viewed` |
| 5 | Tap Continue | continue button | `signed_in` |
| 6 | Select store (if multi-site) | store list row | `site_picker_stores_shown` |
| 7 | Tap Continue | text: "Continue" | `site_picker_continue_tapped` |

### Happy Path: Site Address

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Log in with site address" | id: `buttonLoginStore` | |
| 2 | Enter store URL | URL text field | `login_url_form_viewed` |
| 3 | Tap Continue | continue button | `login_site_address_site_info_requested` |
| 4 | Enter WPCom email (if Jetpack connected) | email text field | `login_email_form_viewed` |
| 5 | Enter password | password text field | `login_password_form_viewed` |
| 6 | Login success | | `signed_in` |

### 2FA Flow (after password)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Enter 6-digit code | OTP input field | `login_two_factor_form_viewed` |
| 2 | Tap Continue | continue button | |
| 3 | (Alternative) Tap "Use security key" | security key button | `login_security_key_success` |
| 4 | (Alternative) Tap "Send SMS" | SMS link | |

### Magic Link Flow

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Enter email | email text field | `login_email_form_viewed` |
| 2 | Request magic link | magic link button | `login_magic_link_requested` |
| 3 | Tap "Open email client" | id: `login_open_email_client` | `login_magic_link_open_email_client_clicked` |
| 4 | (Magic link opened from email) | | `login_magic_link_succeeded` |

### Google SSO

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap Google sign-in button | Google button | `login_social_button_click` |
| 2 | Complete Google auth | Google account picker | `login_social_success` |

### Jetpack Activation (when Jetpack not installed)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Site discovered without Jetpack | | `login_jetpack_required_screen_viewed` |
| 2 | Tap "Continue" / "Install Jetpack" | continue button | `login_jetpack_setup_button_tapped` |
| 3 | Enter site credentials (if needed) | username/password fields | `login_jetpack_site_credential_screen_viewed` |
| 4 | Jetpack installation progress | progress indicator | `login_jetpack_setup_completed` |
| 5 | Authorize connection | WebView auth | |

### Error Flows

| Error | Detection | Logcat Event |
|-------|-----------|-------------|
| Invalid URL | text: error + "Try again" button | `login_discovery_error_screen_viewed` |
| Not WordPress | dialog: "not a WordPress site" | |
| Wrong account | account mismatch screen with avatar | |
| No WPCom account | dialog: account not found | |
| Login failed | error message on password screen | `login_failed` |
| Insufficient role | permission error | `login_insufficient_role` |
| Site discovery failed | error + "Troubleshoot" link | `login_site_address_site_info_failed` |
