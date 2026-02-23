**What were your biggest accomplishments this week?**

**POS Bookings (CIAB MLP)** - big week, mostly focused on booking details and bug fixes:
- Redesigned booking details: updated title/info section, customer section UI, new list item design per Figma specs ([#15360](https://github.com/woocommerce/woocommerce-android/pull/15360), [#15362](https://github.com/woocommerce/woocommerce-android/pull/15362), [#15371](https://github.com/woocommerce/woocommerce-android/pull/15371))
- Added attendance toggle and booking cancellation from details screen ([#15357](https://github.com/woocommerce/woocommerce-android/pull/15357), [#15354](https://github.com/woocommerce/woocommerce-android/pull/15354))
- Made collect payment button sticky and available for cancelled bookings ([#15378](https://github.com/woocommerce/woocommerce-android/pull/15378), [#15376](https://github.com/woocommerce/woocommerce-android/pull/15376))
- Fixed payment section currency formatting ([#15365](https://github.com/woocommerce/woocommerce-android/pull/15365))
- Fixed bookings pagination and loading indicator ([#15387](https://github.com/woocommerce/woocommerce-android/pull/15387))
- Fixed PTR and error screen strings - was showing "orders" instead of "bookings" ([#15389](https://github.com/woocommerce/woocommerce-android/pull/15389))
- Flagged that bookable products should not show in POS product lists
- Set "View Order" as primary action in overflow menu (in review, [#15406](https://github.com/woocommerce/woocommerce-android/pull/15406))
- Team member avatar in bookings list (in review, [#15381](https://github.com/woocommerce/woocommerce-android/pull/15381))
- Posted progress tracking update in #woo-pos
- PR reviews

Started **BetterOn Video** Foundations course.

**What did you learn?**

When the user completes an action (payment, refund, cancellation), the UI should reflect the change right away using optimistic updates, not through pull-to-refresh. Pull-to-refresh is for checking new data, not for confirming something the user just did.

Started **BetterOn Video Foundations** - going through the baseline exercises on camera presence, eye contact, and delivery habits. Interesting to see yourself on video and notice things you don't think about normally.

**What are your three top goals for the next week?**

- POS Bookings - wrap up open PRs and remaining MLP items
- BetterOn Video - first assignment
- Backlog
