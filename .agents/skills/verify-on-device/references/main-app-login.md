# Login Flow

Uses Fragment navigation inside `LoginActivity`. Multiple paths depending on login method.

## Screen Detection

| Screen | How to Detect |
|--------|---------------|
| Prologue | text: "Log In" button and "Starting a new store?" link |
| WPCom Email | text: email input field, "Log in to WordPress.com using an email address" |
| Password | text: password input field, "Reset your password" link |
| 2FA / Verification Code | text: "Verification code" or OTP input |
| Magic Link Sent | text: "Check your email on this device" and "Open Mail" button |
| Site Address | text: "Enter your store URL" |
| Site Credentials | text: "Enter credentials for site:" with username/password fields |
| Site Picker | store list with "Continue" button |
| Account Mismatch | text: account mismatch error with avatar |
| Discovery Error | error message with "Try again" button |
| Jetpack Activation | text about Jetpack with "Continue" or "Install Jetpack" button |
| Not WordPress Error | dialog: "This site is not a WordPress site" |

## Workflows

### Happy Path: WPCom Email + Password

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Log In" | id: `buttonLoginStore` |
| 2 | Enter store URL | URL text field |
| 3 | Tap Continue | continue button |
| 4 | Enter WPCom email (if Jetpack connected) | email text field |
| 5 | Enter password | password text field |
| 6 | Tap Continue | continue button |
| 7 | Select store (if multi-site) | store list row |
| 8 | Tap Continue | text: "Continue" |

### Happy Path: Site Address

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Log In" | id: `buttonLoginStore` |
| 2 | Enter store URL | URL text field |
| 3 | Tap Continue | continue button |
| 4 | Enter WPCom email (if Jetpack connected) | email text field |
| 5 | Enter password | password text field |
| 6 | Login success | |

### 2FA Flow (after password)

| Step | Action | Element |
|------|--------|---------|
| 1 | Enter 6-digit code | OTP input field |
| 2 | Tap Continue | continue button |
| 3 | (Alternative) Tap "Use security key" | security key button |
| 4 | (Alternative) Tap "Send SMS" | SMS link |

### Magic Link Flow

| Step | Action | Element |
|------|--------|---------|
| 1 | Enter email | email text field |
| 2 | Request magic link | magic link button |
| 3 | Tap "Open Mail" | id: `login_open_email_client` |
| 4 | (Magic link opened from email) | |

### Google SSO

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap Google sign-in button | Google button |
| 2 | Complete Google auth | Google account picker |

### Jetpack Activation (when Jetpack not installed)

| Step | Action | Element |
|------|--------|---------|
| 1 | Site discovered without Jetpack | |
| 2 | Tap "Continue" / "Install Jetpack" | continue button |
| 3 | Enter site credentials (if needed) | username/password fields |
| 4 | Jetpack installation progress | progress indicator |
| 5 | Authorize connection | WebView auth |

### Error Flows

| Error | Detection |
|-------|-----------|
| Invalid URL | text: error + "Try again" button |
| Not WordPress | dialog: "not a WordPress site" |
| Wrong account | account mismatch screen with avatar |
| No WPCom account | dialog: account not found |
| Login failed | error message on password screen |
| Insufficient role | permission error |
| Site discovery failed | error + "Troubleshoot" link |
