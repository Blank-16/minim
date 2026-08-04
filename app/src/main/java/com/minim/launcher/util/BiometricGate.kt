package com.minim.launcher.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine

private const val AUTH_FLAGS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Gates the hidden-apps screen behind the device's existing lock method
 * (biometric or PIN/pattern/password) rather than a launcher-specific PIN —
 * one less secret for the person to manage, and it's exactly as secure as
 * unlocking their phone already is.
 */
object BiometricGate {

    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTH_FLAGS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Returns true only on a real successful authentication; false on any error, cancel, or lockout. */
    suspend fun authenticate(activity: FragmentActivity, title: String = "Unlock hidden apps"): Boolean =
        suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(true))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) continuation.resumeWith(Result.success(false))
                    }

                    // Deliberately no-op: a single failed attempt (wrong finger,
                    // misread) should let the person retry, not immediately fail.
                    override fun onAuthenticationFailed() = Unit
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(AUTH_FLAGS)
                .build()

            prompt.authenticate(promptInfo)
        }
}
