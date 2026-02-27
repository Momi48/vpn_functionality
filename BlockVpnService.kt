package com.example.voxmaster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream

class BlockVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val CHANNEL_ID = "vpn_channel"
    private val NOTIFICATION_ID = 1

    companion object {
        var isRunning = false
        val blockedSites = mutableListOf(
            "youtube.com",
            "facebook.com",
            "instagram.com",
        )
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = false
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            isRunning = false
            vpnInterface?.close()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        startVpn()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Site Blocker Active")
            .setContentText("Blocking ${blockedSites.size} sites")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startVpn() {
        val builder = Builder()
            .addAddress("10.0.0.2", 32)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
            .setSession("SiteBlocker")
            .addDisallowedApplication(packageName)

        vpnInterface = builder.establish()
        isRunning = true

        Thread {
            val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
            val packet = ByteArray(32767)

            while (isRunning) {
                val length = inputStream.read(packet)
                if (length > 0) {
                    val domain = extractDomain(packet, length)
                    if (domain != null && isBlocked(domain)) {
                        Log.d("VPN", "Blocked: $domain")
                    } else {
                        outputStream.write(packet, 0, length)
                    }
                }
            }
        }.start()
    }

    private fun extractDomain(packet: ByteArray, length: Int): String? {
        return try {
            if (length < 28) return null

            val protocol = packet[9].toInt() and 0xFF
            if (protocol != 17) return null

            val destPort = ((packet[22].toInt() and 0xFF) shl 8) or
                    (packet[23].toInt() and 0xFF)
            if (destPort != 53) return null

            var i = 28
            val domainBuilder = StringBuilder()

            while (i < length) {
                val labelLength = packet[i].toInt() and 0xFF
                if (labelLength == 0) break
                if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
                i++
                for (j in 0 until labelLength) {
                    if (i + j < length) {
                        domainBuilder.append(packet[i + j].toChar())
                    }
                }
                i += labelLength
            }

            domainBuilder.toString().lowercase()
        } catch (e: Exception) {
            null
        }
    }

    private fun isBlocked(domain: String): Boolean {
        return blockedSites.any { domain.contains(it) }
    }

    override fun onDestroy() {
        isRunning = false
        vpnInterface?.close()
        super.onDestroy()
    }
}