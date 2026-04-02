package com.example.sshclient.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ssh_credentials",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(alias: String, password: String) {
        prefs.edit().putString("pw_$alias", password).apply()
    }

    fun getPassword(alias: String): String? = prefs.getString("pw_$alias", null)

    fun savePrivateKey(alias: String, pemContent: String) {
        prefs.edit().putString("pk_$alias", pemContent).apply()
    }

    fun getPrivateKey(alias: String): String? = prefs.getString("pk_$alias", null)

    fun deleteAlias(alias: String) {
        prefs.edit()
            .remove("pw_$alias")
            .remove("pk_$alias")
            .apply()
    }
}
