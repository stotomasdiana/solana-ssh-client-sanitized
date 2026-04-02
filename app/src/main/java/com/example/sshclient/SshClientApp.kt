package com.example.sshclient

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.schmizz.sshj.common.SecurityUtils

@HiltAndroidApp
class SshClientApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // On Android, prefer the platform crypto provider instead of sshj's default BC registration.
        SecurityUtils.setRegisterBouncyCastle(false)
        SecurityUtils.setSecurityProvider(null)
    }
}
