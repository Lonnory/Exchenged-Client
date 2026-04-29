package com.exchenged.client.viewmodel

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.exchenged.client.BuildConfig
import com.exchenged.client.common.di.qualifier.ShortTime
import com.exchenged.client.common.model.PingType
import com.exchenged.client.common.repository.DEFAULT_DELAY_TEST_URL
import com.exchenged.client.common.repository.SettingsKeys
import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.common.repository.dataStore
import com.exchenged.client.core.XrayBaseService
import com.exchenged.client.core.XrayBaseServiceManager
import com.exchenged.client.core.XrayCoreManager
import com.exchenged.client.dto.Link
import com.exchenged.client.dto.Node
import com.exchenged.client.dto.Subscription
import com.exchenged.client.model.protocol.protocolsPrefix
import com.exchenged.client.parser.ParserFactory
import com.exchenged.client.parser.SubscriptionParser
import com.exchenged.client.repository.NodeRepository
import com.exchenged.client.repository.SubscriptionRepository
import com.exchenged.client.ui.navigation.NavigateDestination
import com.exchenged.client.utils.BarcodeUtils
import com.google.zxing.BarcodeFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import libv2ray.Libv2ray
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder
import java.util.concurrent.Executors
import javax.inject.Inject

class XrayViewmodel(
    private val repository: NodeRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val xrayBaseServiceManager: XrayBaseServiceManager,
    private val xrayCoreManager: XrayCoreManager,
    private val parserFactory: ParserFactory,
    private val okHttp: OkHttpClient,
    private val subscriptionParser: SubscriptionParser
): ViewModel() {

    companion object {
        const val TAG = "XrayViewmodel"
        const val EXTRA_LINK = "com.exchenged.client.EXTRA_LINK"
        const val EXTRA_PROTOCOL = "com.exchenged.client.EXTRA_PROTOCOL"
        const val DELETE_ALL = -2
        const val DELETE_NONE = -1
        const val SUB_ALL = 0
        const val SUB_MANUAL = -1
    }

    private val pingDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSubscriptionId = MutableStateFlow(SUB_ALL)
    val selectedSubscriptionId: StateFlow<Int> = _selectedSubscriptionId.asStateFlow()

    private val _pendingRoute = MutableStateFlow<NavigateDestination?>(null)
    val pendingRoute = _pendingRoute.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _nodeDelays = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val nodeDelays: StateFlow<Map<Int, Long>> = _nodeDelays.asStateFlow()

    private val _nodesTesting = MutableStateFlow<Set<Int>>(emptySet())
    val nodesTesting: StateFlow<Set<Int>> = _nodesTesting.asStateFlow()

    private val _upSpeed = MutableStateFlow(0.0)
    val upSpeed: StateFlow<Double> = _upSpeed.asStateFlow()

    private val _downSpeed = MutableStateFlow(0.0)
    val downSpeed: StateFlow<Double> = _downSpeed.asStateFlow()

    private val _delay = MutableStateFlow(-1L)
    val delay = _delay.asStateFlow()

    private val _testing = MutableStateFlow(false)
    val testing = _testing.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(XrayBaseService.statusFlow.value)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _qrcodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrcodeBitmap.asStateFlow()

    private val _deleteDialog = MutableStateFlow(false)
    val deleteDialog: StateFlow<Boolean> = _deleteDialog.asStateFlow()

    private val _showNavigationBar = MutableStateFlow(true)
    val showNavigationBar = _showNavigationBar.asStateFlow()

    private val _logList = MutableStateFlow<List<String>>(emptyList())
    val logList = _logList.asStateFlow()

    val settingsState = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.exchenged.client.common.repository.SettingsState()
    )

    val subscriptions: StateFlow<List<Subscription>> = subscriptionRepository.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val nodes: StateFlow<List<Node>> = combine(
        repository.allLinks,
        _searchQuery,
        _selectedSubscriptionId
    ) { allNodes, query, subId ->
        val filteredBySub = if (subId == SUB_ALL) allNodes else allNodes.filter { it.subscriptionId == subId }
        val reversed = filteredBySub.reversed()
        if (query.isBlank()) reversed else {
            reversed.filter { node ->
                node.remark?.contains(query, ignoreCase = true) == true ||
                        node.url.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queryNodes: StateFlow<List<Node>> = combine(
        repository.allLinks,
        _searchQuery,
        _selectedSubscriptionId
    ) { allNodes, query, subId ->
        if (query.isBlank()) emptyList() else {
            val filteredBySub = if (subId == SUB_ALL) allNodes else allNodes.filter { it.subscriptionId == subId }
            filteredBySub.reversed().filter { node ->
                node.remark?.contains(query, ignoreCase = true) == true ||
                        node.url.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var deleteLinkId = DELETE_NONE
    var shareUrl = ""
    private var measureJob: Job? = null

    init {
        xrayBaseServiceManager.viewmodelTrafficCallback = { pair ->
            _upSpeed.value = pair.first
            _downSpeed.value = pair.second
        }
        viewModelScope.launch {
            XrayBaseService.statusFlow.collect {
                _isServiceRunning.value = it
            }
        }

        // Add built-in subscriptions
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val builtInSubs = listOf(
                    "https://raw.githubusercontent.com/Lonnory/Sub/refs/heads/main/bypass.bin",
                    "https://raw.githubusercontent.com/Lonnory/Sub/refs/heads/main/vpn.bin"
                )
                val currentSubs = subscriptionRepository.allSubscriptions.first()
                for (url in builtInSubs) {
                    if (currentSubs.none { it.url == url }) {
                        val subId = subscriptionRepository.addSubscription(
                            Subscription(id = 0, mark = "Built-in", url = url, isLocked = true)
                        )
                        fetchSubscription(url, subId.toInt())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Built-in subscriptions init failed", e)
            }
        }
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
    }

    fun selectSubscription(id: Int) {
        _selectedSubscriptionId.value = id
    }

    fun getConfigFromClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        return if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).coerceToText(context).toString()
        } else ""
    }

    fun addXrayConfigFromClipboard(context: Context) {
        val clipboardText = getConfigFromClipboard(context).trim()
        if (clipboardText.isBlank()) return
        
        if (clipboardText.startsWith("#")) {
            viewModelScope.launch(Dispatchers.IO) {
                _isUpdating.value = true
                try {
                    val (subscription, urls) = subscriptionParser.parse(clipboardText, "manual_import_${System.currentTimeMillis()}")
                    val subId = subscriptionRepository.addSubscription(subscription)
                    val newNodes = mutableListOf<Node>()
                    for (url in urls) {
                        try {
                            val trimmedUrl = url.trim()
                            val protocol = trimmedUrl.substringBefore("://").lowercase()
                            if (protocolsPrefix.contains(protocol)) {
                                val link = Link(protocolPrefix = protocol, content = trimmedUrl, subscriptionId = subId.toInt())
                                newNodes.add(parserFactory.getParser(protocol).preParse(link))
                            }
                        } catch (e: Exception) { Log.e(TAG, "Parsing error", e) }
                    }
                    if (newNodes.isNotEmpty()) repository.addNode(*newNodes.toTypedArray())
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Imported ${newNodes.size} nodes from ${subscription.mark}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } finally { _isUpdating.value = false }
            }
            return
        }

        clipboardText.split(Regex("[,\\s]+")).filter { it.isNotBlank() }.forEach { addLink(it.trim(), context) }
    }

    fun addLink(link: String, context: Context? = null) {
        viewModelScope.launch {
            val lines = link.split(Regex("[,\\s]+")).filter { it.isNotBlank() }
            for (line in lines) {
                val trimmedLink = line.trim()
                val protocolPrefix = trimmedLink.substringBefore("://").lowercase()
                if (protocolsPrefix.contains(protocolPrefix)) {
                    try {
                        val link0 = Link(protocolPrefix = protocolPrefix, content = trimmedLink, subscriptionId = SUB_MANUAL)
                        repository.addNode(parserFactory.getParser(protocolPrefix).preParse(link0))
                    } catch (e: Exception) {
                        Log.e(TAG, "addLink error: ${e.message}")
                    }
                } else if (protocolPrefix == "http" || protocolPrefix == "https") {
                    _isUpdating.value = true
                    try {
                        val subId = subscriptionRepository.addSubscription(Subscription(id = 0, mark = "Fetching...", url = trimmedLink))
                        context?.let { android.widget.Toast.makeText(it, "Fetching subscription...", android.widget.Toast.LENGTH_SHORT).show() }
                        fetchSubscription(trimmedLink, subId.toInt(), context)
                    } catch (e: Exception) {
                        _isUpdating.value = false
                        context?.let { android.widget.Toast.makeText(it, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
    }

    fun updateSubscription(subscriptionId: Int, url: String, context: Context? = null) {
        fetchSubscription(url, subscriptionId, context)
    }

    fun importSubscriptionAsNodes(url: String, context: Context? = null) {
        val request = Request.Builder().get().url(url).header("User-Agent", "v2rayN/6.23").build()
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            try {
                val response = okHttp.newCall(request).execute()
                if (response.isSuccessful) {
                    val content = response.body?.string() ?: ""
                    if (content.isNotBlank()) {
                        val (_, urls) = subscriptionParser.parse(content, url)
                        val newNodes = mutableListOf<Node>()
                        for (nodeUrl in urls) {
                            try {
                                val trimmed = nodeUrl.trim()
                                val protocol = trimmed.substringBefore("://").lowercase()
                                if (protocolsPrefix.contains(protocol)) {
                                    newNodes.add(parserFactory.getParser(protocol).preParse(Link(protocolPrefix = protocol, content = trimmed, subscriptionId = SUB_MANUAL)))
                                }
                            } catch (e: Exception) { }
                        }
                        if (newNodes.isNotEmpty()) repository.addNode(*newNodes.toTypedArray())
                        context?.let { withContext(Dispatchers.Main) { android.widget.Toast.makeText(it, "Imported ${newNodes.size} nodes", android.widget.Toast.LENGTH_SHORT).show() } }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "importSubscriptionAsNodes error", e)
            } finally { _isUpdating.value = false }
        }
    }

    private fun fetchSubscription(url: String, subscriptionId: Int, context: Context? = null) {
        val request = Request.Builder().get().url(url).header("User-Agent", "v2rayN/6.23").build()
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdating.value = true
            try {
                val response = okHttp.newCall(request).execute()
                if (response.isSuccessful) {
                    val content = response.body?.string() ?: ""
                    if (content.isNotBlank()) {
                        val (parsedSubscription, urls) = subscriptionParser.parse(content, url, subscriptionId)
                        subscriptionRepository.updateSubscription(parsedSubscription)
                        repository.deleteLinkBySubscriptionId(subscriptionId)
                        val newNodes = mutableListOf<Node>()
                        for (nodeUrl in urls) {
                            try {
                                val trimmed = nodeUrl.trim()
                                val protocol = trimmed.substringBefore("://").lowercase()
                                if (protocolsPrefix.contains(protocol)) {
                                    newNodes.add(parserFactory.getParser(protocol).preParse(Link(protocolPrefix = protocol, content = trimmed, subscriptionId = subscriptionId)))
                                }
                            } catch (e: Exception) { }
                        }
                        if (newNodes.isNotEmpty()) repository.addNode(*newNodes.toTypedArray())
                        context?.let { withContext(Dispatchers.Main) { android.widget.Toast.makeText(it, "Updated: ${parsedSubscription.mark}", android.widget.Toast.LENGTH_SHORT).show() } }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchSubscription error", e)
            } finally { _isUpdating.value = false }
        }
    }

    fun startXrayService(context: Context) {
        viewModelScope.launch { xrayBaseServiceManager.startXrayBaseService() }
    }

    fun stopXrayService(context: Context) {
        xrayBaseServiceManager.stopXrayBaseService()
    }

    fun isServiceRunning(): Boolean = XrayBaseService.statusFlow.value

    fun getAllLinks(): Flow<List<Node>> = repository.allLinks

    fun updateLinkById(id: Int, selected: Boolean) {
        viewModelScope.launch { repository.updateLinkById(id, selected) }
    }

    fun getSelectedNode(): Flow<Node?> = repository.querySelectedNode()

    fun setSelectedNode(id: Int) {
        viewModelScope.launch {
            if (id == repository.querySelectedNode().first()?.id) return@launch
            repository.clearSelection()
            repository.updateLinkById(id, true)
            xrayBaseServiceManager.restartXrayBaseServiceIfNeed()
        }
    }

    fun deleteNode(id: Int) {
        when (id) {
            DELETE_ALL -> deleteAllNodes()
            SUB_MANUAL -> viewModelScope.launch { repository.deleteLinkBySubscriptionId(SUB_MANUAL) }
            else -> deleteNodeById(id)
        }
    }

    fun deleteNodeById(id: Int) {
        viewModelScope.launch { repository.deleteLinkById(id) }
    }

    fun deleteAllNodes() {
        viewModelScope.launch { repository.deleteAllNodes() }
    }

    fun generateQRCode(id: Int) {
        viewModelScope.launch {
            val node = repository.loadLinksById(id).first()
            shareUrl = node.url
            _qrcodeBitmap.value = BarcodeUtils.encodeBitmap(shareUrl, BarcodeFormat.QR_CODE, 400, 400)
        }
    }

    fun exportConfigToClipboard(context: Context) {
        if (shareUrl.isEmpty()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("label", shareUrl))
        shareUrl = ""
    }

    fun showDeleteDialog(id: Int = DELETE_ALL) {
        _deleteDialog.value = true
        deleteLinkId = id
    }

    fun hideDeleteDialog() {
        _deleteDialog.value = false
        deleteLinkId = DELETE_NONE
    }

    fun showNavigationBar() { _showNavigationBar.value = true }
    fun hideNavigationBar() { _showNavigationBar.value = false }

    fun deleteNodeFromDialog() {
        deleteNode(deleteLinkId)
        hideDeleteDialog()
    }

    fun dismissDialog() { _qrcodeBitmap.value = null }

    fun pingNodes(nodes: List<Node>) {
        viewModelScope.launch {
            val type = settingsState.value.pingType
            val testUrl = settingsState.value.delayTestUrl.ifBlank { DEFAULT_DELAY_TEST_URL }
            
            if (type == PingType.GET) {
                nodes.forEach { node ->
                    launch(pingDispatcher) {
                        _nodesTesting.value = _nodesTesting.value + node.id
                        val delay = tcpPing(node.address, node.port)
                        _nodeDelays.value = _nodeDelays.value + (node.id to delay)
                        _nodesTesting.value = _nodesTesting.value - node.id
                    }
                }
            } else {
                val semaphore = Semaphore(3)
                nodes.forEach { node ->
                    launch(Dispatchers.IO) {
                        semaphore.withPermit {
                            _nodesTesting.value = _nodesTesting.value + node.id
                            val delay = xrayGetPing(node, testUrl)
                            _nodeDelays.value = _nodeDelays.value + (node.id to delay)
                            _nodesTesting.value = _nodesTesting.value - node.id
                        }
                    }
                }
            }
        }
    }

    fun pingNode(node: Node) {
        viewModelScope.launch {
            _nodesTesting.value = _nodesTesting.value + node.id
            val type = settingsState.value.pingType
            val testUrl = settingsState.value.delayTestUrl.ifBlank { DEFAULT_DELAY_TEST_URL }
            val delay = when(type) {
                PingType.GET -> tcpPing(node.address, node.port)
                PingType.ICMP -> xrayGetPing(node, testUrl)
            }
            _nodeDelays.value = _nodeDelays.value + (node.id to delay)
            _nodesTesting.value = _nodesTesting.value - node.id
        }
    }

    private suspend fun xrayGetPing(node: Node, testUrl: String): Long = withContext(Dispatchers.IO) {
        try {
            val pingUrl = if (testUrl.contains("google.com")) "https://www.google.com/generate_204" else testUrl
            val config = parserFactory.getParser(node.protocolPrefix).parse(node.url, includeTun = false, forPing = true)
            val delay = Libv2ray.measureOutboundDelay(config, pingUrl)
            if (delay <= 0) -2L else delay
        } catch (e: Exception) {
            Log.e(TAG, "xrayGetPing error: ${e.message}")
            -2L
        }
    }

    private fun tcpPing(host: String, port: Int): Long {
        val start = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                System.currentTimeMillis() - start
            }
        } catch (e: Exception) {
            -2L
        }
    }

    fun measureDelay(context: Context) {
        if (!isServiceRunning()) return
        measureJob?.cancel()
        _testing.value = true
        _delay.value = -1L
        measureJob = viewModelScope.launch {
            val url = context.dataStore.data.first()[SettingsKeys.DELAY_TEST_URL] ?: DEFAULT_DELAY_TEST_URL
            val resultDeferred = CompletableDeferred<Long>()
            val testJob = launch(Dispatchers.IO) { resultDeferred.complete(try { xrayCoreManager.measureDelaySync(url) } catch (e: Exception) { -1L }) }
            val timeoutJob = launch { delay(5000L); resultDeferred.complete(-2L) }
            val finalResult = resultDeferred.await()
            timeoutJob.cancel(); testJob.cancel()
            _testing.value = false
            _delay.value = if (finalResult <= 0L) -2L else finalResult
        }
    }

    fun getLogcatContent(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lst = linkedListOf("logcat", "-d", "-v", "time", "-s", "GoLog,tun2socks,AndroidRuntime,System.err,Exception")
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.runningAppProcesses?.find { it.processName == context.packageName }?.pid?.let { lst.add(2, "--pid"); lst.add(3, it.toString()) }
                val log = Runtime.getRuntime().exec(lst.toTypedArray()).inputStream.bufferedReader().readText().lines()
                withContext(Dispatchers.Main) { _logList.value = log }
            } catch (e: Exception) { Log.i(TAG, "Logcat error", e) }
        }
    }
    
    private fun <T> linkedListOf(vararg elements: T): MutableList<T> = java.util.LinkedList(elements.toList())

    fun exportLogcatToClipboard(context: Context) {
        val log = _logList.value.joinToString("\n")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("log", log))
    }

    fun setPaddingRoute(navigation: NavigateDestination?) { _pendingRoute.value = navigation }

    fun setFirstLaunch(isFirstLaunch: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFirstLaunch(isFirstLaunch)
        }
    }

    fun bugReport(context: Context) {
        val issueBody = "App Version: ${BuildConfig.VERSION_NAME}\nAndroid: ${Build.VERSION.RELEASE}\nModel: ${Build.MODEL}"
        try {
            val url = "https://github.com/Q7DF1/XrayFA/issues/new?title=[Bug]&body=${URLEncoder.encode(issueBody, "UTF-8")}&labels=bug"
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { e.printStackTrace() }
    }
}

class XrayViewmodelFactory @Inject constructor(
    private val repository: NodeRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val xrayBaseServiceManager: XrayBaseServiceManager,
    private val xrayCoreManager: XrayCoreManager,
    private val parserFactory: ParserFactory,
    @ShortTime private val okHttp: OkHttpClient,
    private val subscriptionParser: SubscriptionParser
): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return XrayViewmodel(repository, subscriptionRepository, settingsRepository, xrayBaseServiceManager, xrayCoreManager, parserFactory, okHttp, subscriptionParser) as T
    }
}
