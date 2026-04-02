# Android SSH 客户端中级版 — 完整规格文档

> **目标读者**：编程 AI（Cursor / Claude / Copilot 等）。本文档即可直接作为实现输入，无需额外解释。

---

## 0. 技术栈与版本锁定

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.x | 全代码库使用 Kotlin，禁止 Java 混用 |
| Android minSdk | 26 | Android 8.0+ |
| Android targetSdk | 34 | |
| Jetpack Compose | BOM 2024.02.x | 全 UI 使用 Compose，禁止 XML layout |
| Compose Navigation | 2.7.x | |
| Hilt | 2.51.x | DI 框架 |
| Room | 2.6.x | 本地数据库 |
| sshj | 0.38.0 | SSH 连接库 |
| AndroidX Security Crypto | 1.1.0-alpha06 | EncryptedSharedPreferences |
| Kotlin Coroutines | 1.8.x | 异步处理 |
| ViewModel + StateFlow | Lifecycle 2.7.x | |
| DataStore Preferences | 1.0.x | 应用设置 |

---

## 1. 项目结构

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/example/sshclient/
│   │   ├── SshClientApp.kt                  # Application，Hilt 入口
│   │   ├── MainActivity.kt                  # 单 Activity，Compose host
│   │   │
│   │   ├── data/
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.kt           # Room Database
│   │   │   │   ├── entity/
│   │   │   │   │   ├── ServerEntity.kt
│   │   │   │   │   └── KnownHostEntity.kt
│   │   │   │   └── dao/
│   │   │   │       ├── ServerDao.kt
│   │   │   │       └── KnownHostDao.kt
│   │   │   ├── model/
│   │   │   │   ├── Server.kt                # Domain model
│   │   │   │   ├── AuthType.kt              # sealed class
│   │   │   │   └── HostKeyAction.kt         # enum
│   │   │   ├── repository/
│   │   │   │   ├── ServerRepository.kt
│   │   │   │   └── KnownHostRepository.kt
│   │   │   └── security/
│   │   │       └── CredentialStore.kt       # EncryptedSharedPreferences 封装
│   │   │
│   │   ├── ssh/
│   │   │   ├── SshConnectionManager.kt      # 连接生命周期管理
│   │   │   ├── SshSession.kt               # 单连接会话（含 shell channel）
│   │   │   ├── HostKeyVerifier.kt          # 指纹校验逻辑
│   │   │   └── TerminalBuffer.kt           # 终端行缓冲
│   │   │
│   │   ├── ui/
│   │   │   ├── theme/
│   │   │   │   ├── Theme.kt
│   │   │   │   ├── Color.kt
│   │   │   │   └── Type.kt
│   │   │   ├── navigation/
│   │   │   │   └── AppNavGraph.kt
│   │   │   ├── screen/
│   │   │   │   ├── serverlist/
│   │   │   │   │   ├── ServerListScreen.kt
│   │   │   │   │   └── ServerListViewModel.kt
│   │   │   │   ├── serveredit/
│   │   │   │   │   ├── ServerEditScreen.kt
│   │   │   │   │   └── ServerEditViewModel.kt
│   │   │   │   ├── terminal/
│   │   │   │   │   ├── TerminalScreen.kt
│   │   │   │   │   ├── TerminalViewModel.kt
│   │   │   │   │   ├── TerminalOutputView.kt   # 滚动输出组件
│   │   │   │   │   └── QuickKeyBar.kt          # 快捷键工具条
│   │   │   │   └── hostkey/
│   │   │   │       └── HostKeyDialog.kt
│   │   │   └── component/
│   │   │       ├── PasswordField.kt
│   │   │       └── LoadingOverlay.kt
│   │   │
│   │   └── di/
│   │       ├── DatabaseModule.kt
│   │       ├── RepositoryModule.kt
│   │       ├── SecurityModule.kt
│   │       └── SshModule.kt
│   │
│   └── res/
│       └── raw/
│           └── (存放私钥文件示例，运行时由用户导入)
└── build.gradle.kts
```

---

## 2. 数据层

### 2.1 ServerEntity.kt

```kotlin
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String,          // "PASSWORD" | "PRIVATE_KEY"
    val privateKeyAlias: String?,  // CredentialStore 中的 key alias
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.2 KnownHostEntity.kt

```kotlin
@Entity(tableName = "known_hosts", indices = [Index(value = ["host", "port"], unique = true)])
data class KnownHostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int,
    val keyType: String,       // e.g. "ssh-rsa", "ecdsa-sha2-nistp256"
    val fingerprint: String,   // SHA-256 hex fingerprint
    val addedAt: Long = System.currentTimeMillis()
)
```

### 2.3 ServerDao.kt

```kotlin
@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: ServerEntity): Long

    @Delete
    suspend fun delete(server: ServerEntity)
}
```

### 2.4 KnownHostDao.kt

```kotlin
@Dao
interface KnownHostDao {
    @Query("SELECT * FROM known_hosts WHERE host = :host AND port = :port LIMIT 1")
    suspend fun find(host: String, port: Int): KnownHostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KnownHostEntity)

    @Query("DELETE FROM known_hosts WHERE host = :host AND port = :port")
    suspend fun delete(host: String, port: Int)
}
```

### 2.5 AppDatabase.kt

```kotlin
@Database(
    entities = [ServerEntity::class, KnownHostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun knownHostDao(): KnownHostDao
}
```

### 2.6 Domain Model

```kotlin
// Server.kt
data class Server(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authType: AuthType,
    val privateKeyAlias: String?
)

// AuthType.kt
sealed class AuthType {
    object Password : AuthType()
    data class PrivateKey(val alias: String) : AuthType()
}

// HostKeyAction.kt
enum class HostKeyAction { ACCEPT, REJECT, ACCEPT_ONCE }
```

---

## 3. 安全存储

### 3.1 CredentialStore.kt

使用 `androidx.security.crypto.EncryptedSharedPreferences`，AES256_GCM 加密 value，AES256_SIV 加密 key。

```kotlin
@Singleton
class CredentialStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ssh_credentials",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** 存储密码，key = "pw_<serverId>" */
    fun savePassword(alias: String, password: String) {
        prefs.edit().putString("pw_$alias", password).apply()
    }

    fun getPassword(alias: String): String? = prefs.getString("pw_$alias", null)

    /** 存储私钥 PEM 内容，key = "pk_<alias>" */
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
```

---

## 4. SSH 层

### 4.1 HostKeyVerifier.kt

实现 `net.schmizz.sshj.transport.verification.HostKeyVerifier`。

```kotlin
class HostKeyVerifier @Inject constructor(
    private val knownHostRepo: KnownHostRepository
) : HostKeyVerifier {

    /**
     * 回调给 UI 层，让用户决定是否信任新 key。
     * 通过 CompletableDeferred 挂起连接协程。
     */
    var onUnknownHost: (suspend (host: String, port: Int, keyType: String, fingerprint: String) -> HostKeyAction)? = null

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        // 在协程中阻塞执行（调用方已在 IO dispatcher）
        return runBlocking { verifyAsync(hostname, port, key) }
    }

    private suspend fun verifyAsync(host: String, port: Int, key: PublicKey): Boolean {
        val fp = computeFingerprint(key)           // SHA-256:Base64
        val keyType = key.algorithm                // "RSA", "EC" 等
        val stored = knownHostRepo.find(host, port)

        return when {
            stored == null -> {
                // 新主机，询问用户
                val action = onUnknownHost?.invoke(host, port, keyType, fp)
                    ?: HostKeyAction.REJECT
                if (action == HostKeyAction.ACCEPT || action == HostKeyAction.ACCEPT_ONCE) {
                    if (action == HostKeyAction.ACCEPT) {
                        knownHostRepo.upsert(KnownHostEntity(host = host, port = port,
                            keyType = keyType, fingerprint = fp))
                    }
                    true
                } else false
            }
            stored.fingerprint == fp -> true
            else -> {
                // TOFU 冲突，弹窗告警，默认拒绝
                false
            }
        }
    }

    private fun computeFingerprint(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        return "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}
```

### 4.2 SshSession.kt

```kotlin
/**
 * 封装单个 SSH 连接及其 interactive shell channel。
 * 线程安全：所有 IO 操作在 Dispatchers.IO 上执行。
 */
class SshSession(
    private val server: Server,
    private val credentialStore: CredentialStore,
    private val hostKeyVerifier: HostKeyVerifier
) {

    private lateinit var client: SSHClient
    private lateinit var shell: Session.Shell
    private lateinit var outputReader: BufferedReader

    // 对外暴露的终端输出流
    private val _outputFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 512)
    val outputFlow: SharedFlow<String> = _outputFlow

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    /**
     * 建立连接并启动 shell，完成后开始读取输出。
     * 调用方负责在 IO dispatcher 调用此函数。
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        client = SSHClient(DefaultConfig())
        client.addHostKeyVerifier(hostKeyVerifier)
        client.connect(server.host, server.port)

        // 认证
        when (server.authType) {
            is AuthType.Password -> {
                val pw = credentialStore.getPassword(server.id.toString())
                    ?: error("No password stored for server ${server.id}")
                client.authPassword(server.username, pw)
            }
            is AuthType.PrivateKey -> {
                val pem = credentialStore.getPrivateKey(server.authType.alias)
                    ?: error("No private key stored for alias ${server.authType.alias}")
                val keyProvider = client.loadKeys(pem, null, null)
                client.authPublickey(server.username, keyProvider)
            }
        }

        // 开启 interactive shell
        val session = client.startSession()
        session.allocatePTY("xterm-256color", 220, 50, 0, 0)
        shell = session.startShell()

        outputReader = BufferedReader(InputStreamReader(shell.inputStream, Charsets.UTF_8))
        _connected.value = true

        // 持续读取输出
        launch { readOutput() }
    }

    private suspend fun readOutput() = withContext(Dispatchers.IO) {
        try {
            val buf = CharArray(4096)
            while (isActive && !shell.isEOF) {
                val n = outputReader.read(buf)
                if (n > 0) {
                    _outputFlow.emit(String(buf, 0, n))
                }
            }
        } catch (e: Exception) {
            _outputFlow.emit("\r\n[Session closed: ${e.message}]\r\n")
        } finally {
            _connected.value = false
        }
    }

    /** 向 shell stdin 写入字符串（命令或按键序列）。 */
    suspend fun send(text: String) = withContext(Dispatchers.IO) {
        if (::shell.isInitialized && !shell.isEOF) {
            shell.outputStream.write(text.toByteArray(Charsets.UTF_8))
            shell.outputStream.flush()
        }
    }

    /** 更新终端窗口大小（列 x 行）。 */
    suspend fun resize(cols: Int, rows: Int) = withContext(Dispatchers.IO) {
        if (::shell.isInitialized) {
            shell.changeWindowDimensions(cols, rows)
        }
    }

    fun disconnect() {
        runCatching { shell.close() }
        runCatching { client.disconnect() }
        _connected.value = false
    }
}
```

### 4.3 TerminalBuffer.kt

```kotlin
/**
 * 环形行缓冲，最多保留 MAX_LINES 行，支持 ANSI 转义序列保留（不解析，直接传递给渲染层）。
 */
class TerminalBuffer(private val maxLines: Int = 3000) {

    private val lines = ArrayDeque<String>(maxLines)
    private var currentLine = StringBuilder()

    @Synchronized
    fun append(raw: String): List<String> {
        for (ch in raw) {
            when (ch) {
                '\n' -> {
                    lines.addLast(currentLine.toString())
                    if (lines.size > maxLines) lines.removeFirst()
                    currentLine = StringBuilder()
                }
                '\r' -> { /* CR: 光标回首，简单处理直接忽略独立 CR */ }
                else -> currentLine.append(ch)
            }
        }
        return snapshot()
    }

    @Synchronized
    fun snapshot(): List<String> {
        val result = ArrayList<String>(lines.size + 1)
        result.addAll(lines)
        if (currentLine.isNotEmpty()) result.add(currentLine.toString())
        return result
    }

    @Synchronized
    fun clear() {
        lines.clear()
        currentLine = StringBuilder()
    }
}
```

### 4.4 SshConnectionManager.kt

```kotlin
/**
 * 全局单例，管理所有 SshSession 的生命周期。
 * 一个 serverId 对应一个 session（复用连接）。
 */
@Singleton
class SshConnectionManager @Inject constructor(
    private val credentialStore: CredentialStore,
    private val knownHostRepo: KnownHostRepository
) {
    private val sessions = ConcurrentHashMap<Long, SshSession>()

    fun getSession(serverId: Long): SshSession? = sessions[serverId]

    fun createSession(server: Server, hostKeyVerifier: HostKeyVerifier): SshSession {
        val session = SshSession(server, credentialStore, hostKeyVerifier)
        sessions[server.id] = session
        return session
    }

    fun removeSession(serverId: Long) {
        sessions.remove(serverId)?.disconnect()
    }

    fun disconnectAll() {
        sessions.values.forEach { it.disconnect() }
        sessions.clear()
    }
}
```

---

## 5. Repository 层

### 5.1 ServerRepository.kt

```kotlin
@Singleton
class ServerRepository @Inject constructor(
    private val dao: ServerDao,
    private val credentialStore: CredentialStore
) {
    fun observeAll(): Flow<List<Server>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Server? = dao.getById(id)?.toDomain()

    /**
     * 保存服务器配置及凭据。
     * @param password 密码认证时传入，私钥认证时传 null
     * @param privateKeyPem 私钥认证时传入 PEM 内容，密码认证时传 null
     */
    suspend fun save(server: Server, password: String?, privateKeyPem: String?): Long {
        val entity = server.toEntity()
        val id = dao.upsert(entity)
        password?.let { credentialStore.savePassword(id.toString(), it) }
        privateKeyPem?.let {
            val alias = server.privateKeyAlias ?: id.toString()
            credentialStore.savePrivateKey(alias, it)
        }
        return id
    }

    suspend fun delete(server: Server) {
        dao.delete(server.toEntity())
        credentialStore.deleteAlias(server.id.toString())
        server.privateKeyAlias?.let { credentialStore.deleteAlias(it) }
    }

    // Mapping extensions
    private fun ServerEntity.toDomain() = Server(
        id = id, name = name, host = host, port = port, username = username,
        authType = if (authType == "PASSWORD") AuthType.Password
                   else AuthType.PrivateKey(privateKeyAlias ?: id.toString()),
        privateKeyAlias = privateKeyAlias
    )

    private fun Server.toEntity() = ServerEntity(
        id = id, name = name, host = host, port = port, username = username,
        authType = when (authType) { is AuthType.Password -> "PASSWORD"; else -> "PRIVATE_KEY" },
        privateKeyAlias = privateKeyAlias
    )
}
```

### 5.2 KnownHostRepository.kt

```kotlin
@Singleton
class KnownHostRepository @Inject constructor(private val dao: KnownHostDao) {
    suspend fun find(host: String, port: Int): KnownHostEntity? = dao.find(host, port)
    suspend fun upsert(entity: KnownHostEntity) = dao.upsert(entity)
    suspend fun delete(host: String, port: Int) = dao.delete(host, port)
}
```

---

## 6. DI 模块

### 6.1 DatabaseModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ssh_client.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideServerDao(db: AppDatabase): ServerDao = db.serverDao()
    @Provides fun provideKnownHostDao(db: AppDatabase): KnownHostDao = db.knownHostDao()
}
```

### 6.2 SecurityModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    @Provides @Singleton
    fun provideCredentialStore(@ApplicationContext ctx: Context): CredentialStore =
        CredentialStore(ctx)
}
```

### 6.3 SshModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SshModule {
    @Provides @Singleton
    fun provideHostKeyVerifier(repo: KnownHostRepository): HostKeyVerifier =
        HostKeyVerifier(repo)

    @Provides @Singleton
    fun provideConnectionManager(
        credentialStore: CredentialStore,
        knownHostRepo: KnownHostRepository
    ): SshConnectionManager = SshConnectionManager(credentialStore, knownHostRepo)
}
```

---

## 7. ViewModel 层

### 7.1 ServerListViewModel.kt

```kotlin
@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val serverRepo: ServerRepository
) : ViewModel() {

    val servers: StateFlow<List<Server>> = serverRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteServer(server: Server) {
        viewModelScope.launch { serverRepo.delete(server) }
    }
}
```

### 7.2 ServerEditViewModel.kt

```kotlin
@HiltViewModel
class ServerEditViewModel @Inject constructor(
    private val serverRepo: ServerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serverId: Long = savedStateHandle.get<Long>("serverId") ?: -1L

    // UI State
    data class EditState(
        val name: String = "",
        val host: String = "",
        val port: String = "22",
        val username: String = "",
        val authType: String = "PASSWORD",    // "PASSWORD" | "PRIVATE_KEY"
        val password: String = "",
        val privateKeyPem: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val saved: Boolean = false
    )

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state

    init {
        if (serverId != -1L) {
            viewModelScope.launch {
                serverRepo.getById(serverId)?.let { server ->
                    _state.update {
                        it.copy(
                            name = server.name, host = server.host,
                            port = server.port.toString(), username = server.username,
                            authType = when (server.authType) {
                                is AuthType.Password -> "PASSWORD"
                                else -> "PRIVATE_KEY"
                            }
                        )
                    }
                }
            }
        }
    }

    fun update(block: EditState.() -> EditState) = _state.update(block)

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val s = _state.value
                val portInt = s.port.toIntOrNull() ?: throw IllegalArgumentException("Invalid port")
                val authType = if (s.authType == "PASSWORD") AuthType.Password
                               else AuthType.PrivateKey(serverId.toString())
                val server = Server(
                    id = if (serverId == -1L) 0L else serverId,
                    name = s.name.ifBlank { s.host },
                    host = s.host.trim(),
                    port = portInt,
                    username = s.username.trim(),
                    authType = authType,
                    privateKeyAlias = if (authType is AuthType.PrivateKey) serverId.toString() else null
                )
                serverRepo.save(
                    server = server,
                    password = if (s.authType == "PASSWORD") s.password else null,
                    privateKeyPem = if (s.authType == "PRIVATE_KEY") s.privateKeyPem else null
                )
            }.onSuccess {
                _state.update { it.copy(isLoading = false, saved = true) }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
```

### 7.3 TerminalViewModel.kt

```kotlin
@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val serverRepo: ServerRepository,
    private val connectionManager: SshConnectionManager,
    private val hostKeyVerifier: HostKeyVerifier,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serverId: Long = checkNotNull(savedStateHandle["serverId"])

    // 终端缓冲
    private val buffer = TerminalBuffer(maxLines = 3000)

    data class TerminalState(
        val lines: List<String> = emptyList(),
        val isConnecting: Boolean = false,
        val isConnected: Boolean = false,
        val error: String? = null,
        // Host key 验证对话框
        val pendingHostKey: PendingHostKey? = null
    )

    data class PendingHostKey(
        val host: String,
        val port: Int,
        val keyType: String,
        val fingerprint: String,
        val deferred: CompletableDeferred<HostKeyAction>
    )

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state

    private var session: SshSession? = null

    init { connect() }

    private fun connect() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }

            // 注入 host key 回调
            hostKeyVerifier.onUnknownHost = { host, port, keyType, fp ->
                val deferred = CompletableDeferred<HostKeyAction>()
                _state.update { it.copy(pendingHostKey = PendingHostKey(host, port, keyType, fp, deferred)) }
                deferred.await().also {
                    _state.update { s -> s.copy(pendingHostKey = null) }
                }
            }

            val server = serverRepo.getById(serverId)
            if (server == null) {
                _state.update { it.copy(isConnecting = false, error = "Server not found") }
                return@launch
            }

            val newSession = connectionManager.createSession(server, hostKeyVerifier)
            session = newSession

            runCatching {
                newSession.connect()
            }.onFailure { e ->
                _state.update { it.copy(isConnecting = false, isConnected = false, error = e.message) }
                return@launch
            }

            _state.update { it.copy(isConnecting = false, isConnected = true) }

            // 收集输出
            newSession.outputFlow.collect { raw ->
                val lines = buffer.append(raw)
                _state.update { it.copy(lines = lines) }
            }
        }
    }

    /** 发送任意文本/命令至 shell stdin */
    fun send(text: String) {
        viewModelScope.launch { session?.send(text) }
    }

    /** 发送单行命令（自动追加 \n） */
    fun sendLine(cmd: String) = send("$cmd\n")

    /** 快捷控制键 */
    fun sendCtrl(char: Char) = send(String(charArrayOf((char.code - 'A'.code + 1).toChar())))

    /** 用户回应 host key 对话框 */
    fun resolveHostKey(action: HostKeyAction) {
        _state.value.pendingHostKey?.deferred?.complete(action)
    }

    /** 清屏（本地 buffer 清空） */
    fun clearBuffer() {
        buffer.clear()
        _state.update { it.copy(lines = emptyList()) }
    }

    fun disconnect() {
        connectionManager.removeSession(serverId)
        session = null
        _state.update { it.copy(isConnected = false) }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
```

---

## 8. UI 层

### 8.1 导航图 AppNavGraph.kt

```kotlin
sealed class Screen(val route: String) {
    object ServerList : Screen("server_list")
    object ServerEdit : Screen("server_edit?serverId={serverId}") {
        fun createRoute(serverId: Long? = null) =
            if (serverId == null) "server_edit" else "server_edit?serverId=$serverId"
    }
    object Terminal : Screen("terminal/{serverId}") {
        fun createRoute(serverId: Long) = "terminal/$serverId"
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.ServerList.route) {
        composable(Screen.ServerList.route) {
            ServerListScreen(
                onAddServer = { navController.navigate(Screen.ServerEdit.createRoute()) },
                onEditServer = { id -> navController.navigate(Screen.ServerEdit.createRoute(id)) },
                onConnectServer = { id -> navController.navigate(Screen.Terminal.createRoute(id)) }
            )
        }
        composable(
            route = Screen.ServerEdit.route,
            arguments = listOf(navArgument("serverId") {
                type = NavType.LongType; defaultValue = -1L
            })
        ) {
            ServerEditScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Terminal.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) {
            TerminalScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

### 8.2 ServerListScreen.kt

```kotlin
@Composable
fun ServerListScreen(
    onAddServer: () -> Unit,
    onEditServer: (Long) -> Unit,
    onConnectServer: (Long) -> Unit,
    viewModel: ServerListViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SSH 客户端") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = "添加服务器")
            }
        }
    ) { padding ->
        if (servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无服务器，点击 + 添加", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(servers, key = { it.id }) { server ->
                    ServerListItem(
                        server = server,
                        onConnect = { onConnectServer(server.id) },
                        onEdit = { onEditServer(server.id) },
                        onDelete = { viewModel.deleteServer(server) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ServerListItem(
    server: Server,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(server.name) },
        supportingContent = { Text("${server.username}@${server.host}:${server.port}") },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onConnect)
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除确认") },
            text = { Text("确定要删除 ${server.name} 吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}
```

### 8.3 ServerEditScreen.kt

```kotlin
@Composable
fun ServerEditScreen(
    onBack: () -> Unit,
    viewModel: ServerEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { /* 读取私钥文件内容，调用 viewModel.update { copy(privateKeyPem = content) } */ }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.name.isEmpty()) "新建服务器" else "编辑服务器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.isLoading) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 基础信息
            OutlinedTextField(state.name, { viewModel.update { copy(name = it) } },
                label = { Text("名称（可选）") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.host, { viewModel.update { copy(host = it) } },
                label = { Text("主机地址 *") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            OutlinedTextField(state.port, { viewModel.update { copy(port = it) } },
                label = { Text("端口") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(state.username, { viewModel.update { copy(username = it) } },
                label = { Text("用户名 *") }, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text("认证方式", style = MaterialTheme.typography.titleSmall)

            // 认证类型切换
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.authType == "PASSWORD",
                    onClick = { viewModel.update { copy(authType = "PASSWORD") } },
                    label = { Text("密码") })
                FilterChip(state.authType == "PRIVATE_KEY",
                    onClick = { viewModel.update { copy(authType = "PRIVATE_KEY") } },
                    label = { Text("私钥") })
            }

            if (state.authType == "PASSWORD") {
                PasswordField(
                    value = state.password,
                    onValueChange = { viewModel.update { copy(password = it) } },
                    label = "密码"
                )
            } else {
                OutlinedButton(onClick = { launcher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.privateKeyPem.isNotEmpty()) "已选择私钥文件" else "选择私钥文件")
                }
                if (state.privateKeyPem.isNotEmpty()) {
                    Text("✓ 私钥已加载", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (state.isLoading) LoadingOverlay()
}
```

### 8.4 TerminalScreen.kt

```kotlin
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    // Host key 对话框
    state.pendingHostKey?.let { pending ->
        HostKeyDialog(
            host = pending.host,
            port = pending.port,
            keyType = pending.keyType,
            fingerprint = pending.fingerprint,
            onAccept = { viewModel.resolveHostKey(HostKeyAction.ACCEPT) },
            onAcceptOnce = { viewModel.resolveHostKey(HostKeyAction.ACCEPT_ONCE) },
            onReject = { viewModel.resolveHostKey(HostKeyAction.REJECT) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("终端", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.isConnecting) "连接中…"
                            else if (state.isConnected) "已连接"
                            else "已断开",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isConnected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.disconnect(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "断开并返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearBuffer() }) {
                        Icon(Icons.Default.Clear, contentDescription = "清屏")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // 快捷键工具条
                QuickKeyBar(
                    onTab = { viewModel.send("\t") },
                    onCtrlC = { viewModel.sendCtrl('C') },
                    onCtrlD = { viewModel.sendCtrl('D') },
                    onCtrlZ = { viewModel.sendCtrl('Z') },
                    onCtrlL = { viewModel.sendCtrl('L') },
                    onArrowUp = { viewModel.send("\u001b[A") },
                    onArrowDown = { viewModel.send("\u001b[B") },
                    onEscape = { viewModel.send("\u001b") },
                    onPaste = {
                        clipboardManager.getText()?.text?.let { viewModel.send(it) }
                    }
                )
                // 输入行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            viewModel.sendLine(inputText)
                            inputText = ""
                        }),
                        decorationBox = { inner ->
                            Box(Modifier.padding(4.dp)) {
                                if (inputText.isEmpty()) {
                                    Text("输入命令…", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                }
                                inner()
                            }
                        }
                    )
                    IconButton(onClick = {
                        viewModel.sendLine(inputText)
                        inputText = ""
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TerminalOutputView(
                lines = state.lines,
                onCopy = { text -> clipboardManager.setText(AnnotatedString(text)) }
            )
            if (state.isConnecting) LoadingOverlay()
            state.error?.let { err ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter)) { Text(err) }
            }
        }
    }
}
```

### 8.5 TerminalOutputView.kt

```kotlin
/**
 * 终端输出滚动显示组件。
 * - 自动滚动到最新行
 * - 长按选择文本可复制
 * - 等宽字体渲染
 */
@Composable
fun TerminalOutputView(
    lines: List<String>,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var selectedText by remember { mutableStateOf("") }

    // 新内容时自动滚动到底部
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(lines.size) { index ->
                    Text(
                        text = lines[index],
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFFE0E0E0),
                        softWrap = true,
                        // 保留 ANSI 转义序列原文（终端模拟器完整支持超出本规格范围）
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 悬浮复制按钮（选中文本后显示）
        if (selectedText.isNotEmpty()) {
            FloatingActionButton(
                onClick = { onCopy(selectedText); selectedText = "" },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制")
            }
        }
    }
}
```

### 8.6 QuickKeyBar.kt

```kotlin
/**
 * 终端快捷键工具条，固定于软键盘上方。
 */
@Composable
fun QuickKeyBar(
    onTab: () -> Unit,
    onCtrlC: () -> Unit,
    onCtrlD: () -> Unit,
    onCtrlZ: () -> Unit,
    onCtrlL: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    onEscape: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        "TAB" to onTab,
        "↑" to onArrowUp,
        "↓" to onArrowDown,
        "ESC" to onEscape,
        "^C" to onCtrlC,
        "^D" to onCtrlD,
        "^Z" to onCtrlZ,
        "^L" to onCtrlL,
        "粘贴" to onPaste
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(keys) { (label, action) ->
                OutlinedButton(
                    onClick = action,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(label, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
```

### 8.7 HostKeyDialog.kt

```kotlin
@Composable
fun HostKeyDialog(
    host: String,
    port: Int,
    keyType: String,
    fingerprint: String,
    onAccept: () -> Unit,
    onAcceptOnce: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = { Icon(Icons.Default.Security, contentDescription = null) },
        title = { Text("未知主机") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("首次连接到：")
                Text("$host:$port", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("密钥类型：$keyType", style = MaterialTheme.typography.bodySmall)
                Text("指纹（SHA-256）：", style = MaterialTheme.typography.bodySmall)
                Text(
                    fingerprint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "请在带外方式（管理员、官方文档）验证指纹后再选择信任。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("永久信任") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onAcceptOnce) { Text("仅本次") }
                TextButton(onClick = onReject) { Text("拒绝") }
            }
        }
    )
}
```

### 8.8 通用组件

```kotlin
// PasswordField.kt
@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "隐藏密码" else "显示密码"
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

// LoadingOverlay.kt
@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
```

---

## 9. AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".SshClientApp"
        android:label="SSH 客户端"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/Theme.SshClient"
        android:allowBackup="false">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

---

## 10. build.gradle.kts (app 模块)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sshclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.sshclient"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    ksp("com.google.dagger:hilt-android-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // SSH
    implementation("com.hierynomus:sshj:0.38.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## 11. 关键实现约束与注意事项

### 11.1 Shell 交互（最重要）

- **禁止使用 `session.exec(command)`**。必须使用 `session.startShell()` + PTY 分配，保持 interactive shell 长连接。
- PTY 类型必须设置为 `xterm-256color`，尺寸初始化为 `220 x 50`（后续根据实际布局调用 `resize()`）。
- shell 输出流必须在独立协程持续读取，不得阻塞主线程。
- 换行符处理：服务器输出 `\r\n` 需在 `TerminalBuffer` 中正确处理。

### 11.2 凭据安全

- 密码和私钥 PEM 内容**绝对不能**存储在 Room 数据库或普通 SharedPreferences 中。
- 必须使用 `EncryptedSharedPreferences`（AES256_GCM）。
- 私钥文件从 Storage Access Framework（SAF）读取后，仅将内容存入 `CredentialStore`，不存磁盘明文。

### 11.3 Host Key 校验（TOFU 模型）

- 首次连接：弹出 `HostKeyDialog`，展示完整 SHA-256 指纹，提供"永久信任 / 仅本次 / 拒绝"三选项。
- 已知主机：对比 `KnownHostEntity.fingerprint`，不一致则**强制拒绝**并展示 MITM 风险警告，不提供"覆盖"选项。
- `HostKeyVerifier.verify()` 被 sshj 在 IO 线程同步调用，内部用 `runBlocking` + `CompletableDeferred` 挂起等待 UI 回调。

### 11.4 ViewModel 生命周期

- `TerminalViewModel` 在 `onCleared()` 中调用 `disconnect()`，确保离开 Terminal 屏幕时连接被正确关闭。
- `SshConnectionManager` 为 `@Singleton`，Activity 重建（旋转屏幕）时连接不中断，ViewModel 重新订阅 `outputFlow`。

### 11.5 线程模型

- 所有 sshj 操作在 `Dispatchers.IO` 执行。
- UI 状态更新通过 `StateFlow` 在主线程收集。
- `TerminalBuffer` 使用 `@Synchronized` 保证多线程安全。

### 11.6 错误处理

- 连接失败（网络、认证错误）：在 `TerminalState.error` 显示，提供重连按钮。
- Session 意外断开：`readOutput()` 捕获异常后向 `outputFlow` emit 断开提示，更新 `connected = false`。
- 认证失败（`UserAuthException`）：显示具体错误信息（密码错误 / 不支持的认证方式）。

### 11.7 复制粘贴

- 使用 Compose `SelectionContainer` 包裹终端输出，系统自动支持长按选择 + 复制。
- QuickKeyBar 的"粘贴"按钮读取系统剪贴板，直接 `send()` 至 shell stdin。

### 11.8 不在本版本范围内（预留扩展点）

- 完整 ANSI/VT100 转义序列解析（如需可集成 `terminfo` 或 `hterm` WebView 方案）
- 多 Tab 多会话 UI
- SFTP 文件传输
- SSH 隧道 / 端口转发
- 生物识别解锁凭据库

---

## 12. 文件读取私钥实现补充

在 `ServerEditScreen` 中，SAF launcher 回调需读取文件内容：

```kotlin
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri ?: return@rememberLauncherForActivityResult
    val content = context.contentResolver.openInputStream(uri)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: return@rememberLauncherForActivityResult
    if (content.contains("PRIVATE KEY")) {
        viewModel.update { copy(privateKeyPem = content) }
    } else {
        // 提示用户文件格式不正确
    }
}
// 调用时：launcher.launch("*/*")
```

---

## 13. SshClientApp.kt 和 MainActivity.kt

```kotlin
// SshClientApp.kt
@HiltAndroidApp
class SshClientApp : Application()

// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SshClientTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
```

---

## 14. 实现顺序建议

按以下顺序实现，每步可独立验证：

1. **项目骨架**：创建模块结构，配置 `build.gradle.kts`，验证编译通过。
2. **数据层**：实现 Entity、DAO、AppDatabase、DI 模块，验证 Room 编译。
3. **CredentialStore**：实现并单元测试加密存储读写。
4. **ServerList + ServerEdit UI**：完成服务器增删改查，不含 SSH 功能。
5. **SshSession 基础连接**：先实现密码认证 + 输出读取，硬编码跳过 host key 校验。
6. **TerminalScreen**：接入 SshSession，验证 interactive shell 可用。
7. **HostKeyVerifier**：实现 TOFU 校验，接入对话框流程。
8. **私钥认证**：接入 SAF 文件选择 + sshj `loadKeys()`。
9. **QuickKeyBar + 复制粘贴**：完善终端操作体验。
10. **错误处理与边界情况**：断线重连、认证失败提示、屏幕旋转不断连。

---

*文档版本：v1.0 | 适用项目：Android SSH 客户端中级版*
