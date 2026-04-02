# SSH Client

Sanitized public copy of the Android project.

What was removed from this export:
- Local SDK configuration in `local.properties`
- Release signing files such as `keystore.properties` and `*.jks`
- Build outputs such as `app/build/` and generated APKs
- Local screenshots and debug images
- The original hardcoded transfer recipient and display amount in the wallet flow

Before building a signed release, add your own local signing config and keystore outside version control.
