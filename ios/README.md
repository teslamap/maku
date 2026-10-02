# Makasia iOS WebView

A minimal native SwiftUI + WKWebView iOS wrapper for the existing mobile storefront.

- Loads `https://teslamap.github.io/maku/mobile-index.html`
- JavaScript and normal WebKit web storage are available by default.
- Back/forward swipe gestures are enabled.
- Tapping links to other hosts opens them outside the app.
- Minimum iOS: 16.0.

## Build

Open `Makasia.xcodeproj` in Xcode on macOS and select an iPhone simulator or a signing-enabled iPhone destination. The GitHub Actions workflow builds an **unsigned Simulator .app** for validation; it is not an installable iPhone IPA. Device installation, TestFlight, and App Store distribution require Apple signing credentials and the applicable Apple developer setup.

The admin site is intentionally not included in this storefront target. It should be a separately secured admin app/target if an iOS admin build is needed.
