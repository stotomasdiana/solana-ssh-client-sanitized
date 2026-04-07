# SSH Server Setup Guide

This guide explains how to enable an SSH server on the target computer before connecting from Solana Remote. It also includes how to find the target machine's IP address and which SSH port to use.

## Before You Start

To connect from the mobile SSH client, you usually need:

- The target computer's local IP address, such as `192.168.1.23`
- The SSH port, which is usually `22`
- A valid username on that machine
- Either a password or a private key that matches the account

## How To Find The Target IP Address

### macOS

Open Terminal and run:

```bash
ipconfig getifaddr en0
```

If you are on Ethernet instead of Wi-Fi, try:

```bash
ipconfig getifaddr en1
```

You can also open:

`System Settings -> Wi-Fi -> Details`

and look for the local IP address.

### Windows 10/11

Open PowerShell or Command Prompt and run:

```powershell
ipconfig
```

Look for:

- `IPv4 Address`

under your active network adapter.

### Ubuntu / Linux

Open Terminal and run:

```bash
hostname -I
```

or:

```bash
ip addr
```

Look for the local IPv4 address on the active network interface.

## How To Check The SSH Port

By default, most SSH servers use:

`22`

If you changed the SSH configuration, use the custom port instead.

### macOS / Linux

Check the SSH server configuration file:

```bash
sudo grep ^Port /etc/ssh/sshd_config
```

If nothing is returned, the default port is usually `22`.

### Windows

Check the OpenSSH configuration:

```powershell
Get-Content C:\ProgramData\ssh\sshd_config | Select-String "^Port"
```

If no custom value is set, use `22`.

## macOS

### Enable SSH (GUI)

1. Open `System Settings`
2. Go to `General -> Sharing`
3. Turn on `Remote Login`

### Enable SSH (Terminal)

```bash
sudo systemsetup -setremotelogin on
```

### Disable SSH

```bash
sudo systemsetup -setremotelogin off
```

## Windows 10/11

### Install OpenSSH Server

```powershell
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0
```

### Start SSH Service

```powershell
Start-Service sshd
```

### Enable Auto Start

```powershell
Set-Service -Name sshd -StartupType 'Automatic'
```

### Allow Firewall

```powershell
New-NetFirewallRule -Name sshd -DisplayName "OpenSSH Server" -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22
```

## Ubuntu

### Install SSH Server

```bash
sudo apt update
sudo apt install openssh-server
```

### Start SSH

```bash
sudo systemctl start ssh
```

### Enable Auto Start

```bash
sudo systemctl enable ssh
```

### Check Status

```bash
sudo systemctl status ssh
```

### Allow Firewall (UFW)

```bash
sudo ufw allow ssh
```

## Example Connection Values

If your MacBook has local IP `192.168.1.23`, your username is `alex`, and SSH is using the default port:

- Host: `192.168.1.23`
- Port: `22`
- Username: `alex`
- Authentication: `Password` or `Private Key`

That is the same information you will enter in the Solana Remote **Add Connection** screen.
