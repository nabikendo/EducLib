package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val UPDATE_PREFERENCES = "app_update_preferences"
private const val KEY_LAST_CHECKED_AT = "last_checked_at"
private const val KEY_DISMISSED_TAG = "dismissed_release_tag"
private const val KEY_CACHED_TAG = "cached_release_tag"
private const val KEY_CACHED_VERSION = "cached_release_version"
private const val KEY_CACHED_APK_URL = "cached_release_apk_url"
private const val KEY_CACHED_PAGE_URL = "cached_release_page_url"
private val RELEASE_CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(12)
private const val LATEST_RELEASE_URL = "https://api.github.com/repos/nabikendo/EducLib/releases/latest"

private data class AvailableRelease(
    val tagName: String,
    val versionName: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String
)

private object AppUpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun findUpdate(context: Context, currentVersion: String): AvailableRelease? = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val cachedUpdate = readCachedUpdate(preferences, currentVersion)
        val lastCheckedAt = preferences.getLong(KEY_LAST_CHECKED_AT, 0L)

        if (now - lastCheckedAt < RELEASE_CHECK_INTERVAL_MS) {
            return@withContext cachedUpdate
        }

        // Record the attempt before networking so an outage cannot cause a request on every app launch.
        preferences.edit().putLong(KEY_LAST_CHECKED_AT, now).apply()

        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "EducLib-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    clearCachedRelease(preferences)
                    return@withContext null
                }
                if (!response.isSuccessful) {
                    return@withContext cachedUpdate
                }

                val body = response.body?.string() ?: return@withContext cachedUpdate
                val release = JSONObject(body)
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                    clearCachedRelease(preferences)
                    return@withContext null
                }

                val tagName = release.optString("tag_name").trim()
                val versionName = tagName.removePrefix("v").removePrefix("V")
                if (tagName.isBlank() || !isVersionNewer(versionName, currentVersion)) {
                    clearCachedRelease(preferences)
                    return@withContext null
                }

                val assets = release.optJSONArray("assets")
                if (assets == null) {
                    clearCachedRelease(preferences)
                    return@withContext null
                }

                var apkDownloadUrl: String? = null
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        if (apkDownloadUrl != null) break
                    }
                }

                if (apkDownloadUrl == null) {
                    clearCachedRelease(preferences)
                    return@withContext null
                }

                val availableRelease = AvailableRelease(
                    tagName = tagName,
                    versionName = versionName,
                    apkDownloadUrl = apkDownloadUrl,
                    releasePageUrl = release.optString("html_url")
                )
                preferences.edit()
                    .putString(KEY_CACHED_TAG, availableRelease.tagName)
                    .putString(KEY_CACHED_VERSION, availableRelease.versionName)
                    .putString(KEY_CACHED_APK_URL, availableRelease.apkDownloadUrl)
                    .putString(KEY_CACHED_PAGE_URL, availableRelease.releasePageUrl)
                    .apply()
                availableRelease
            }
        } catch (exception: Exception) {
            Log.w("AppUpdateChecker", "Release check failed; continuing without blocking the app.", exception)
            cachedUpdate
        }
    }

    private fun clearCachedRelease(preferences: SharedPreferences) {
        preferences.edit()
            .remove(KEY_CACHED_TAG)
            .remove(KEY_CACHED_VERSION)
            .remove(KEY_CACHED_APK_URL)
            .remove(KEY_CACHED_PAGE_URL)
            .apply()
    }

    private fun readCachedUpdate(preferences: SharedPreferences, currentVersion: String): AvailableRelease? {
        val tagName = preferences.getString(KEY_CACHED_TAG, null) ?: return null
        val versionName = preferences.getString(KEY_CACHED_VERSION, null) ?: return null
        val apkUrl = preferences.getString(KEY_CACHED_APK_URL, null) ?: return null
        val pageUrl = preferences.getString(KEY_CACHED_PAGE_URL, null).orEmpty()
        if (!isVersionNewer(versionName, currentVersion)) return null
        return AvailableRelease(tagName, versionName, apkUrl, pageUrl)
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = numericVersionParts(latest)
        val currentParts = numericVersionParts(current)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false

        for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
            val comparison = (latestParts.getOrNull(index) ?: 0).compareTo(currentParts.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    private fun numericVersionParts(value: String): List<Int> {
        val normalized = value.trim().removePrefix("v").removePrefix("V").substringBefore('-')
        val pieces = normalized.split('.')
        if (pieces.isEmpty() || pieces.any { it.toIntOrNull() == null }) return emptyList()
        return pieces.map { it.toInt() }
    }
}

@Composable
fun AppUpdatePrompt() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE)
    }
    var availableRelease by remember { mutableStateOf<AvailableRelease?>(null) }

    LaunchedEffect(Unit) {
        val update = AppUpdateChecker.findUpdate(context, BuildConfig.VERSION_NAME)
        if (update != null && preferences.getString(KEY_DISMISSED_TAG, null) != update.tagName) {
            availableRelease = update
        }
    }

    val update = availableRelease ?: return
    val dismissForLater = {
        preferences.edit().putString(KEY_DISMISSED_TAG, update.tagName).apply()
        availableRelease = null
    }

    AlertDialog(
        onDismissRequest = dismissForLater,
        title = { Text("Atualização disponível") },
        text = { Text("A versão ${update.versionName} do EducLib já pode ser baixada.") },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetUrl = update.apkDownloadUrl.ifBlank { update.releasePageUrl }
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                    }.onFailure {
                        Log.w("AppUpdatePrompt", "Could not open the APK download link.", it)
                    }
                    availableRelease = null
                }
            ) {
                Text("Baixar atualização")
            }
        },
        dismissButton = {
            TextButton(onClick = dismissForLater) {
                Text("Mais tarde")
            }
        }
    )
}
