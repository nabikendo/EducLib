package com.example.ui

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun generateResponse(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Para usar a IA, adicione a sua chave no painel de Secrets ou no arquivo .env."
        }

        try {
            val requestJson = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System instruction if any
            if (systemInstruction != null) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstruction)
                sysParts.put(sysPart)
                sysContent.put("parts", sysParts)
                requestJson.put("systemInstruction", sysContent)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 400 || code == 403) {
                        return@withContext "Erro na API Gemini (Verifique se sua chave está configurada CORRETAMENTE no painel de Secrets). Código: $code"
                    }
                    return@withContext "Erro ao conectar com servidor do Gemini de IA. Código: $code"
                }
                val responseBody = response.body?.string() ?: return@withContext "Sem resposta da IA."
                
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Nenhum texto retornado.")
                        }
                    }
                }
                return@withContext "Resposta da IA em formato inválido."
            }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Erro ao chamar o Gemini: ", e)
            val msg = e.message ?: "Conexão timeout"
            return@withContext "Falha na conexão de internet ou API de IA. Erro: $msg"
        }
    }
}
