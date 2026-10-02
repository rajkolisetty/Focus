package com.focusblock.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.IOException

/**
 * A local, on-device VPN that never actually connects anywhere. Every app except the ones on
 * the allowed list has its network traffic routed into this VPN's tunnel; the tunnel's only job
 * is to read those packets and throw them away, so those apps simply have no working internet.
 * Allowed apps are explicitly excluded from the tunnel, so their own connection is untouched.
 * This is the same technique open-source firewall apps such as NetGuard use.
 */
class LocalVpnService : VpnService() {

    private var fd: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        restart()
        return START_STICKY
    }

    /** Rebuilds the tunnel so a just-changed allow-list takes effect immediately. */
    private fun restart() {
        stopTunnel()
        val builder = Builder()
            .setSession("Focus Block")
            .addAddress("10.231.0.1", 32)
            .addRoute("0.0.0.0", 0)
            .setBlocking(true)

        for (pkg in Prefs.getAllowed(applicationContext)) {
            try { builder.addDisallowedApplication(pkg) } catch (e: Exception) { /* app may be uninstalled */ }
        }
        try { builder.addDisallowedApplication(applicationContext.packageName) } catch (e: Exception) {}

        fd = try { builder.establish() } catch (e: Exception) { null }
        val descriptor = fd ?: return
        running = true
        worker = Thread { sink(descriptor) }.apply { isDaemon = true; start() }
    }

    private fun sink(descriptor: ParcelFileDescriptor) {
        val input = FileInputStream(descriptor.fileDescriptor)
        val buffer = ByteArray(32767)
        while (running) {
            try {
                if (input.read(buffer) < 0) break
            } catch (e: IOException) {
                break
            }
        }
    }

    private fun stopTunnel() {
        running = false
        worker?.interrupt()
        worker = null
        try { fd?.close() } catch (e: IOException) {}
        fd = null
    }

    private fun startForegroundCompat() {
        val channelId = "focus_block_vpn"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(channelId, "Focus Block", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Block is active")
            .setContentText("Blocking internet access for apps that are not allowed")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(1, notif)
    }

    override fun onDestroy() { stopTunnel(); super.onDestroy() }
    override fun onRevoke() { stopTunnel(); super.onRevoke() }

    companion object {
        fun start(ctx: Context) { ctx.startService(Intent(ctx, LocalVpnService::class.java)) }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, LocalVpnService::class.java)) }
    }
}
