package com.minim.launcher.util

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.NotificationManager

/**
 * Torch is the one radio an app can flip directly and reliably via
 * CameraManager — no permission required. WiFi, Bluetooth, and Do Not
 * Disturb cannot be toggled directly by a third-party app on modern Android
 * (Google locked this down starting with Android 10 specifically to stop
 * exactly this kind of launcher/widget shortcut from silently flipping
 * radios). Rather than fake a toggle that doesn't work, this opens the real
 * system panel so the action still completes in one extra tap.
 */
class SystemToggleController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var torchCameraId: String? = null
    var isTorchOn: Boolean = false
        private set

    init {
        torchCameraId = runCatching {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun toggleTorch() {
        val id = torchCameraId ?: return
        runCatching {
            cameraManager?.setTorchMode(id, !isTorchOn)
            isTorchOn = !isTorchOn
        }
    }

    fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openBluetoothPanel() {
        // Bluetooth doesn't have a dedicated Settings.Panel action across all
        // OEM builds; ACTION_BLUETOOTH_SETTINGS is the reliable cross-OEM path.
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openDndSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Without Notification Policy Access granted, Android reports
     * `INTERRUPTION_FILTER_UNKNOWN` (0) rather than the real filter — that's
     * "we can't tell", not "DND is on". Treating it as ALL (i.e. not active)
     * used to be missing here, so the toggle showed DND as permanently
     * "active" for anyone who hadn't granted that permission yet, even with
     * DND genuinely off.
     */
    fun isDndActive(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val filter = nm?.currentInterruptionFilter ?: return false
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }
}
