package com.example.voxmaster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "site_blocker"
    private val VPN_REQUEST_CODE = 100

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "startVpn" -> {
                        val intent = VpnService.prepare(this)
                        if (intent != null) {
                            startActivityForResult(intent, VPN_REQUEST_CODE)
                        } else {
                            startForegroundService(
                                Intent(this, BlockVpnService::class.java)
                            )
                        }
                        result.success(true)
                    }
                    "stopVpn" -> {
                        val stopIntent = Intent(this, BlockVpnService::class.java)
                        stopIntent.action = "STOP"
                        startService(stopIntent)
                        result.success(true)
                    }
                    "blockSite" -> {
                        val url = call.argument<String>("url")
                        if (url != null) {
                            BlockVpnService.blockedSites.add(url)
                        }
                        result.success(true)
                    }
                    "unblockSite" -> {
                        val url = call.argument<String>("url")
                        BlockVpnService.blockedSites.remove(url)
                        result.success(true)
                    }
                    "isVpnRunning" -> {
                        result.success(BlockVpnService.isRunning)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            startForegroundService(Intent(this, BlockVpnService::class.java))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}