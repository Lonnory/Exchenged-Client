package com.exchenged.client.tun2socks

import android.content.Context
import android.util.Log
import com.exchenged.client.common.di.qualifier.Application
import com.exchenged.client.tun2socks.utils.Tun2SocksConfigUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
open class TProxyService @Inject constructor(
    @Application private val context: Context,
    private val util: Tun2SocksConfigUtil
) : Tun2SocksService {

    var running: Boolean = false
    companion object {
        init {
            System.loadLibrary("hev-socks5-tunnel")
        }

        @JvmStatic
        @Suppress("FunctionName")
        external fun TProxyStartService(configPath: String, fd: Int)

        @JvmStatic
        @Suppress("FunctionName")
        external fun TProxyStopService()

        @JvmStatic
        @Suppress("FunctionName")
        external fun TProxyGetStats(): LongArray
    }


    override suspend fun startTun2Socks(fd: Int) {
        val path = util.configure(context)
        withContext(Dispatchers.IO) {
            try {
                TProxyStartService(path, fd)
                running = true
            } catch (e: Exception) {
                Log.e("TProxyService", "startTun2Socks: ${e.message}")
            }
        }
    }

    override suspend fun stopTun2Socks() {
        if (running) {
            TProxyStopService()
            running = false
        }

    }

    override fun isRunning(): Boolean {
        return running
    }


}
