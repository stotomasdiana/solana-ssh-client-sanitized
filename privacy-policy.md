# Solana Remote Privacy Policy

Last updated: April 13, 2026

## 1. Overview

This Privacy Policy explains how Solana Remote handles information when the app is used.

Solana Remote is a technical mobile application that helps users connect to their own remote machines over SSH and, where supported, interact with compatible wallet flows. The app is intended for technically experienced users and is not designed as a general consumer social product.

This repository is a public source-code distribution. Unless a separate operator runs a hosted backend or support service, the default implementation works primarily on-device and through user-selected third-party infrastructure.

## 2. Information We May Process

Depending on how the app is configured and used, Solana Remote may process:

- Server profile information such as host, port, username, labels, and authentication mode
- User-provided authentication material such as passwords or private keys
- Wallet account addresses, public keys, and transaction signatures returned by compatible wallets
- Terminal input and terminal output generated during SSH sessions
- Device-level local storage needed for app settings, saved connections, and session state
- Limited operational information needed for security, debugging, abuse prevention, lawful compliance, or support if a separate operator adds hosted services

## 3. How Information Is Used

Information may be used only for legitimate app functions, including:

- Saving and managing connection profiles
- Establishing SSH sessions requested by the user
- Supporting wallet connection and transaction signing workflows requested by the user
- Displaying terminal output, session status, and transaction status
- Protecting the security, integrity, and reliability of the app
- Complying with applicable law, regulation, legal process, or platform requirements

We do not use deceptive, coercive, or fraudulent methods to obtain user consent or permission for data handling.

## 4. Local Storage

The default app stores some information locally on the user’s device, such as saved connection settings and related configuration data.

At the repository level:

- No hosted user account is required by default
- Users can delete saved connection data from within the app where supported
- Users can remove local app data by uninstalling the app or clearing app storage on the device

If a future deployment adds hosted accounts or cloud-backed services, that deployment operator must provide a lawful and reasonably accessible method for account deletion and deletion of associated personal data, except where retention is required by law, tax, fraud prevention, security, or regulatory obligations.

## 5. Third-Party Services

The app may connect to third-party services chosen by the user or deployment operator, including:

- SSH servers
- Solana RPC endpoints
- Solana-compatible wallets
- Device manufacturers, app marketplaces, or infrastructure services involved in app delivery or operation

Those third parties operate under their own terms and privacy practices. If a deployment operator gives third parties access to user data, that operator should ensure those third parties handle the data consistently with applicable law and the operator’s disclosed privacy commitments.

## 6. Sharing and Disclosure

The default repository implementation is not a general-purpose hosted data-sharing platform.

Information may be transmitted or disclosed only as needed to deliver user-requested functionality or to satisfy legal and security obligations, including:

- Connecting to a user-selected SSH host
- Communicating with a user-selected or app-configured RPC endpoint
- Requesting a wallet session or submitting a blockchain transaction
- Responding to valid legal process, law enforcement requests, or regulatory obligations where required
- Investigating fraud, abuse, or security incidents

We do not sell personal information as part of the default implementation described in this repository.

## 7. Regulated or Sensitive Data

Solana Remote is not intended to collect more regulated or sensitive data than is reasonably necessary for its technical function.

If any deployment processes regulated or sensitive data, that deployment should:

- Minimize collection to what is strictly necessary
- Protect the data using encryption and appropriate security controls
- Avoid unauthorized disclosure, misuse, or sale
- Comply with applicable privacy, financial, consumer protection, security, and other regulatory requirements

Users should avoid storing highly sensitive or regulated information in the app unless doing so is necessary, lawful, and appropriately secured.

## 8. Minors

Solana Remote is not directed to children. Personal information from minors should not be collected unless any consent required by applicable law has been obtained, including parental or guardian consent where required.

## 9. Security

We take reasonable steps to protect information against unauthorized access, misuse, disclosure, alteration, or destruction. However, no device, app, wallet flow, network, or storage system can guarantee absolute security.

Users remain responsible for verifying remote hosts, SSH fingerprints, account credentials, wallet prompts, recipients, transaction amounts, and other high-risk actions before proceeding.

## 10. International and Legal Compliance

Developers, publishers, and deployment operators are responsible for ensuring that their use of Solana Remote complies with applicable privacy, data protection, consumer protection, security, and financial laws in the jurisdictions where the app is offered or used.

## 11. Changes to This Policy

This Privacy Policy may be updated from time to time. Continued use of the app after an updated version is published constitutes acceptance of the revised policy to the extent permitted by applicable law.

## 12. Contact

For questions about privacy, deletion, compliance, or this policy, contact the repository owner or the operator of the specific deployment you are using.
