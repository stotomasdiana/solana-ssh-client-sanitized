package com.example.sshclient.data.model

sealed class AuthType {
    data object Password : AuthType()
    data class PrivateKey(val alias: String) : AuthType()
}
