package dev.sora.protohax

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareConfig
import com.github.megatronking.netbare.NetBareListener
import com.github.megatronking.netbare.ip.IpAddress
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.sora.protohax.CacheManager.readStringOrDefault
import dev.sora.protohax.CacheManager.writeString


class MainActivity : Activity(), NetBareListener {

    private val configBuilder: NetBareConfig.Builder
        get() = NetBareConfig.Builder()
        .setMtu(4096)
        .setAddress(IpAddress("10.1.10.1", 32))
        .setSession("ProtoHax")
        .addRoute(IpAddress("0.0.0.0", 0))

    private fun requestOverlayPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(myIntent)
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<FloatingActionButton>(R.id.floatingBtn)
        val input = findViewById<TextView>(R.id.name_edit_text)
        button.setOnClickListener {
            try {
                if (requestOverlayPermission()) {
                    Toast.makeText(this, getString(R.string.request_overlay), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (!NetBare.get().isActive) {
                    val intent = NetBare.get().prepare()
                    if (intent == null) {
                        val targetPkgName = input.text.toString()
                        writeString(TARGET_PACKAGE_CACHE_KEY, targetPkgName)
                        NetBare.get().start(configBuilder
                            .addAllowedApplication(targetPkgName)
                            .build())
                        Toast.makeText(this, getString(R.string.start_proxy_toast, targetPkgName), Toast.LENGTH_LONG).show()
                    } else {
                        this.startActivity(intent)
                    }
                } else {
                    NetBare.get().stop()
                    Toast.makeText(this, getString(R.string.stop_proxy_toast), Toast.LENGTH_LONG).show()
                }
            } catch (e: Throwable) {
                Log.e("ProtoHax", "mitm", e)
            }
        }
        input.text = readStringOrDefault(TARGET_PACKAGE_CACHE_KEY, "com.mojang.minecraftpe")

        NetBare.get().registerNetBareListener(this)
        updateConnStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        NetBare.get().unregisterNetBareListener(this)
    }

    private fun updateConnStatus(status: Boolean = NetBare.get().isActive) {
        val button = findViewById<FloatingActionButton>(R.id.floatingBtn)
        button.backgroundTintList = ColorStateList.valueOf(getColor(if (status) R.color.actionbtn_active else R.color.actionbtn_inactive))

        val text1 = findViewById<TextView>(R.id.bottomAppBarText)
        text1.setText(if (status) R.string.connected else R.string.not_connected)
    }

    override fun onServiceStarted() {
        runOnUiThread {
            updateConnStatus(true)
        }
    }

    override fun onServiceStopped() {
        runOnUiThread {
            updateConnStatus(false)
        }
    }

    companion object {
        private const val TARGET_PACKAGE_CACHE_KEY = "TARGET_PACKAGE"
    }
}