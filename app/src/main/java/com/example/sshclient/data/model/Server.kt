package com.example.sshclient.data.model

data class Server(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authType: AuthType,
    val privateKeyAlias: String?
)
