# Privacy Policy

Last updated: April 7, 2026

## Overview

Solana Remote is designed to provide mobile SSH access and wallet-assisted transaction flows. This repository is a public source-code distribution and does not itself operate a managed hosted service unless a separate operator deploys one.

This Privacy Policy is intended to describe, in a transparent way, how user data may be collected, accessed, used, stored, and disclosed when the app or any related deployment is used.

## Data We May Process

Depending on configuration and user actions, the app may process:

- Server connection details entered by the user, such as host, port, username, and labels
- Authentication material provided by the user, such as passwords or private keys
- Wallet addresses, account public keys, and transaction signatures returned by a compatible wallet
- Terminal input and output generated during active SSH sessions
- Device-level app data required for local storage, session state, and app functionality
- Limited technical and operational data necessary for debugging, security, or compliance if a separate operator adds backend services

## How Data Is Used

User data may be used only to:

- Save and manage local server profiles
- Establish SSH connections requested by the user
- Support wallet connection and transaction signing workflows
- Display terminal output, connection state, and transaction status inside the app
- Maintain app security, stability, debugging, fraud prevention, and lawful compliance where applicable

We do not use deceptive, coercive, or fraudulent methods to obtain consent for data processing. If consent is required by applicable law, it should be obtained in a lawful and clear manner.

## Local Storage and Deletion

The app may store certain user-provided credentials and configuration data locally on the device.

At the current repository level:

- No hosted user account is required by default
- Saved server profiles may be deleted by the user from within the app
- Local app data may also be removed by uninstalling the app or clearing app storage on the device

If a future deployment adds hosted accounts or cloud-backed storage, the operator of that deployment must provide a lawful method for users to request account deletion and deletion of associated user data, except where retention is required for legal, security, fraud-prevention, tax, or regulatory reasons.

## Third-Party Services

The app may interact with third-party systems chosen by the user, including:

- SSH servers
- Solana RPC endpoints
- Solana-compatible wallets through Solana Mobile Wallet Adapter
- Other infrastructure selected by the developer or deployment operator

Any third-party service with access to user data should be contractually or operationally required to handle such data in a manner consistent with applicable law and the privacy commitments made for the app.

Each third-party service remains governed by its own privacy practices and terms.

## Sharing and Disclosure

This repository does not provide a general-purpose hosted data-sharing service. Data is transmitted only as needed to carry out user-requested functionality, such as:

- Connecting to an SSH server
- Requesting a wallet session
- Submitting a blockchain transaction
- Accessing a user-selected infrastructure endpoint

We do not sell user data as part of the default repository implementation.

## Regulated and Sensitive Data

The app is not intended to collect more regulated or sensitive data than is reasonably necessary for its function.

If any deployment collects or processes regulated or sensitive data, that deployment should:

- Collect only the minimum data necessary for the stated purpose
- Use encryption and industry-standard safeguards in transit and at rest
- Avoid disclosure or sale of such data except with lawful authority, express user consent where required, and compliance with applicable law

Users should not enter highly regulated data into the app unless doing so is necessary, lawful, and supported by an appropriate deployment environment.

## Minors

The app is not directed to children, and user data from minors should not be collected without any consent required by applicable law, including parental or guardian consent where required.

## Security

Reasonable efforts may be made to store sensitive information securely on-device and to protect data against unauthorized access, disclosure, alteration, or destruction. However, no software, wallet flow, network, or storage method can guarantee absolute security.

Users remain responsible for validating server fingerprints, remote hosts, wallet prompts, and transaction details before proceeding.

## International and Legal Compliance

Developers and deployment operators are responsible for complying with applicable privacy, consumer protection, data protection, and security laws in the jurisdictions where the app is offered or used.

## Changes

This Privacy Policy may be updated from time to time. Continued use of the app after changes are published constitutes acceptance of the revised policy, to the extent permitted by applicable law.

## Contact

For ownership, licensing, privacy, or deletion-related questions, use the contact details maintained by the repository owner or the relevant deployment operator.
