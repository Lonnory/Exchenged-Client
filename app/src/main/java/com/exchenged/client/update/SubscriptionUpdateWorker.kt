package com.exchenged.client.update

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.exchenged.client.dto.Link
import com.exchenged.client.model.protocol.protocolsPrefix
import com.exchenged.client.parser.ParserFactory
import com.exchenged.client.parser.SubscriptionParser
import com.exchenged.client.repository.NodeRepository
import com.exchenged.client.repository.SubscriptionRepository
import com.exchenged.client.XrayAppCompatFactory
import com.exchenged.client.common.di.qualifier.ShortTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class SubscriptionUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @Inject
    lateinit var subRepo: SubscriptionRepository
    @Inject
    lateinit var nodeRepo: NodeRepository
    @Inject
    @ShortTime
    lateinit var okHttp: OkHttpClient
    @Inject
    lateinit var subParser: SubscriptionParser
    @Inject
    lateinit var parserFactory: ParserFactory

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("SubUpdateWorker", "Starting auto-update of all subscriptions...")
        
        XrayAppCompatFactory.rootComponent?.inject(this@SubscriptionUpdateWorker)
        
        // If injection failed (shouldn't happen if app is running), we can't proceed
        if (!::subRepo.isInitialized) {
            Log.e("SubUpdateWorker", "Dependency injection failed")
            return@withContext Result.failure()
        }

        try {
            val subscriptions = subRepo.allSubscriptions.first()
            for (sub in subscriptions) {
                Log.d("SubUpdateWorker", "Updating: ${sub.mark} (${sub.url})")
                val request = Request.Builder()
                    .get()
                    .url(sub.url)
                    .header("User-Agent", "v2rayN/6.23")
                    .header("Cache-Control", "no-cache")
                    .build()

                try {
                    val response = okHttp.newCall(request).execute()
                    if (response.isSuccessful) {
                        val content = response.body?.string() ?: ""
                        if (content.isNotBlank()) {
                            val (parsedSub, urls) = subParser.parse(content, sub.url, sub.id)
                            subRepo.updateSubscription(parsedSub)
                            nodeRepo.deleteLinkBySubscriptionId(sub.id)
                            
                            val newNodes = mutableListOf<com.exchenged.client.dto.Node>()
                            for (nodeUrl in urls) {
                                try {
                                    val trimmed = nodeUrl.trim()
                                    val protocol = trimmed.substringBefore("://").lowercase()
                                    if (protocolsPrefix.contains(protocol)) {
                                        newNodes.add(parserFactory.getParser(protocol).preParse(Link(protocolPrefix = protocol, content = trimmed, subscriptionId = sub.id)))
                                    }
                                } catch (e: Exception) { }
                            }
                            if (newNodes.isNotEmpty()) nodeRepo.addNode(*newNodes.toTypedArray())
                            Log.d("SubUpdateWorker", "Successfully updated ${sub.mark} with ${newNodes.size} nodes")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SubUpdateWorker", "Failed to update ${sub.mark}: ${e.message}")
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SubUpdateWorker", "Fatal error in worker: ${e.message}")
            Result.retry()
        }
    }
}
