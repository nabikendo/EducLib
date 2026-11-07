package com.example.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
private const val KEY_DISMISSED_AT = "dismissed_release_at"
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


    suspend fun downloadApk(context: Context, release: AvailableRelease): Uri = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(release.apkDownloadUrl)
        require(parsed.scheme == "https" && parsed.host == "github.com") {
            "A atualização deve vir do GitHub por HTTPS."
        }
        val request = Request.Builder().url(release.apkDownloadUrl).get().build()
        val destination = File(context.cacheDir, "educLib-update.apk")
        val staging = File(context.cacheDir, "educLib-update.tmp")
        try {
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Falha no download: HTTP ${response.code}" }
                val length = response.body?.contentLength() ?: -1L
                check(length <= 200L * 1024 * 1024) { "O arquivo excede o limite permitido." }
                val body = response.body ?: error("O download não contém dados.")
                body.byteStream().use { input ->
                    staging.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var count = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            count += read
                            check(count <= 200L * 1024 * 1024) { "Arquivo APK muito grande." }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            check(staging.length() > 0L) { "Arquivo APK vazio." }
            val archive = context.packageManager.getPackageArchiveInfo(staging.absolutePath, 0)
            check(archive?.packageName == context.packageName) {
                "O APK não pertence ao EducLib."
            }
            check(staging.renameTo(destination)) { "Não foi possível finalizar o download." }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination)
        } finally {
            staging.delete()
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
    val scope = rememberCoroutineScope()
    val preferences = remember(context) {
        context.getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE)
    }
    var availableRelease by remember { mutableStateOf<AvailableRelease?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val update = AppUpdateChecker.findUpdate(context, BuildConfig.VERSION_NAME)
        val postponed = update != null && preferences.getString(KEY_DISMISSED_TAG, null) == update.tagName &&
            System.currentTimeMillis() - preferences.getLong(KEY_DISMISSED_AT, 0L) < TimeUnit.DAYS.toMillis(1)
        if (update != null && !postponed) {
            availableRelease = update
        }
    }

    val update = availableRelease ?: return
    val dismissForLater = {
        if (!downloading) {
            preferences.edit().putString(KEY_DISMISSED_TAG, update.tagName)
                .putLong(KEY_DISMISSED_AT, System.currentTimeMillis()).apply()
            availableRelease = null
        }
    }

    AlertDialog(
        onDismissRequest = dismissForLater,
        title = { Text("Atualização disponível") },
        text = {
            Text(
                when {
                    downloading -> "Baixando a versão ${update.versionName} dentro do EducLib…"
                    error != null -> "Não foi possível preparar a atualização: $error"
                    else -> "Versão ${update.versionName}. O EducLib baixa o APK e solicita a confirmação de instalação do Android."
                }
            )
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    downloading = true
                    error = null
                    scope.launch {
                        try {
                            val uri = AppUpdateChecker.downloadApk(context, update)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                !context.packageManager.canRequestPackageInstalls()
                            ) {
                                val settingsIntent = Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(settingsIntent)
                                error = "Autorize a instalação para o EducLib nas configurações e tente novamente."
                            } else {
                                context.startActivity(intent)
                                availableRelease = null
                            }
                        } catch (exception: Exception) {
                            Log.w("AppUpdatePrompt", "In-app update failed.", exception)
                            error = exception.message ?: "Erro desconhecido."
                        } finally {
                            downloading = false
                        }
                    }
                }
            ) {
                Text(if (downloading) "Baixando…" else "Atualizar agora")
            }
        },
        dismissButton = {
            TextButton(enabled = !downloading, onClick = dismissForLater) {
                Text("Mais tarde")
            }
        }
    )
}
