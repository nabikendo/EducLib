package com.example.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                     BiometricManager.Authenticators.DEVICE_CREDENTIAL
                biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                // On older APIs, check BIOMETRIC_STRONG alone to avoid IllegalArgumentException
                val status = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                status == BiometricManager.BIOMETRIC_SUCCESS
            }
        } catch (e: Throwable) {
            android.util.Log.e("BiometricHelper", "Error checking biometric availability: ${e.message}", e)
            false
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Acesso Administrativo",
        subtitle: String = "Por favor, autentique-se para continuar",
        negativeButtonText: String = "Cancelar",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autenticação biométrica não reconhecida.")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Throwable) {
            onError(e.message ?: "Falha ao iniciar autenticação biométrica.")
        }
    }
}
