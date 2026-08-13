# Login Flow

Uses Fragment navigation inside `LoginActivity`. Multiple paths depending on login method.

## Agent auto-login

Preserve and reuse a coherent authenticated session before attempting login. If login is needed, follow
[Agent auto-login](agent-auto-login.md) for profile setup, invocation, safety constraints, outcomes, and recovery.

## Screen Detection

| Screen | How to Detect |
|--------|---------------|
| Prologue | text: "Log In" button and "Starting a new store?" link |
| WPCom Email | text: "Log in with your WordPress.com account email address to manage your WooCommerce stores.", hint: "Email address" |
| Password | text: password input field, "Reset your password" link |
| 2FA / Verification Code | text: "Almost there! Please enter the verification code", hint: "Verification code" |
| Magic Link Sent | text: "Check your email on this device" and "Open Mail" button |
| Site Address | text: "Enter the address of the WooCommerce store you'd like to connect.", hint: "Site address" |
| Site Credentials | text: "Log in with your [site URL] site credentials" with "Username" and "Password" fields |
| Site Picker | selectable store list with "Continue" button |
| Account Mismatch | text: "It looks like [site] is connected to a different WordPress.com account." with avatar |
| Discovery Error | title: "Connection error" with "Try again" and "Read our troubleshooting tips" options |
| Jetpack Activation | text: "Please install the free Jetpack plugin" with "Install Jetpack" or "Connect Jetpack" button |
| Not WordPress Error | text: "We were not able to detect a WordPress site at the address you entered." |

## Workflows

### Happy Path: WPCom Email + Password

| Step | Action | Element |
|---|--------|---------|
| 1 | Tap "Log In" | id: `button_login_store` |
| 2 | Enter store URL | URL text field |
| 3 | Tap Continue | continue button |
| 4 | Enter WPCom email (if Jetpack connected) | email text field |
| 5 | Enter password | password text field |
| 6 | Tap Continue | continue button |
| 7 | Login success | |

### 2FA Flow (after password)

| Step | Action | Element |
|------|--------|---------|
| 1 | Enter 6-digit code | OTP input field |
| 2 | Tap Continue | continue button |
| 3 | (Alternative) Tap "Use a security key" | id: `login_security_key_button` |
| 4 | (Alternative) Tap "Text me a code instead" | id: `login_otp_button` |

### Magic Link Flow

| Step | Action | Element |
|---|--------|---------|
| 1 | Tap "Log In" | id: `button_login_store` |
| 2 | Enter store URL | URL text field |
| 3 | Tap Continue | continue button |
| 4 | Enter email | email text field |
| 5 | Request magic link | magic link button |
| 6 | Tap "Open Mail" | id: `login_open_email_client` |
| 7 | (Magic link opened from email) | |

### Google SSO

| Step | Action | Element |
|---|--------|---------|
| 1 | Tap "Log In" | id: `button_login_store` |
| 2 | Enter store URL | URL text field |
| 3 | Tap Continue | continue button |
| 4 | Tap "Continue with Google" | id: `continue_with_google` |
| 5 | Complete Google auth | Google account picker |

### Jetpack Activation (when Jetpack not installed or not activated)

| Step | Action | Element |
|------|--------|---------|
| 1 | Site discovered without Jetpack | |
| 2 | Tap "Install Jetpack" or "Connect Jetpack" | jetpack action button |
| 3 | Enter site credentials (if needed) | username/password fields |
| 4 | Jetpack installation progress | progress indicator |
| 5 | Authorize connection | WebView auth |

### Error Flows

| Error | Detection |
|-------|-----------|
| Invalid URL | text: error + "Try again" button |
| Not WordPress | text: "We were not able to detect a WordPress site at the address you entered." |
| Wrong account | account mismatch screen with avatar |
| No WPCom account | dialog: account not found |
| Login failed | error message on password screen |
| Insufficient role | permission error |
| Site discovery failed | error + "Troubleshoot" link |
