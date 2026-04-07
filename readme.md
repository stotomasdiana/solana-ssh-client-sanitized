# Solana Remote

Solana Remote is built for people who use vibe coding AI, remote servers, and wallet-based actions, but do not want to stay glued to a desktop just to keep the workflow moving.

With this app, you can continue interacting with your remote environment directly from your phone instead of sitting in front of a computer waiting for the next step in the loop. Open a server, check terminal output, send commands, and handle mobile wallet signing flows while you are away from your desk.

## Why It Matters

- Stay in control of your remote workflow from your phone
- Keep up with vibe coding AI driven tasks without being tied to a laptop
- Monitor SSH sessions and respond immediately when action is needed
- Trigger wallet-assisted flows in the same mobile experience

## What This Repository Contains

This repository is a sanitized public export of the Android project. Sensitive local files, signing materials, build outputs, screenshots, and original hardcoded transfer details were intentionally removed before publication.

## Documentation

- [Copyright Notice](copyright.md)
- [Privacy Policy](privacy-policy.md)
- [License](license.md)
- [Terms and Conditions](terms-conditions.md)
- [SSH Server Setup Guide](ssh-server-setup.md)

## How To Use The SSH Client

1. Enable SSH on the machine you want to access.
2. Make sure the target device is reachable from your phone over the network.
3. Open Solana Remote on your phone.
4. Tap **Add Connection**.
5. Enter the host, port, username, and authentication method.
6. If you use password authentication, enter the password.
8. Save the connection profile.
9. Tap the saved node from the connection list to open the terminal session.
10. Review the host fingerprint on first connection and trust it only after verification.
11. Start sending commands from the live terminal screen.
12. Use the built-in quick keys for common terminal actions like `TAB`, `ESC`, arrow keys, and control shortcuts.

If SSH is not yet enabled on your server, read the [SSH Server Setup Guide](ssh-server-setup.md) or open the in-app setup guide for macOS, Windows, and Ubuntu instructions.

## Add Connection Example

Here is a concrete example of what to enter in the **Add Connection** screen.

Example target machine:

- Local IP address: `192.168.1.23`
- SSH port: `22`
- Username: `alex`
- Authentication: password login

Fill the fields like this:

- Name: `MacBook Office`
- Host: `192.168.1.23`
- Port: `22`
- Username: `alex`
- Authentication: `Password`
- Password: your login password for that machine

If you use key-based authentication instead, keep the same Host, Port, and Username, then switch Authentication to `Private Key` and import the PEM private key file that matches the remote machine account.

## Setup Notes

- Add your own `local.properties` before building locally
- Add your own signing configuration and keystore before creating signed release builds
- Review the wallet transfer placeholders before using this code in production
