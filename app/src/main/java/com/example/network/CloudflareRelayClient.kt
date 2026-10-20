package com.example.network

import android.content.Context
import android.util.Base64
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
import java.io.File
import java.util.concurrent.TimeUnit

class CloudflareRelayClient(private val context: Context) {
    private val prefs = context.getSharedPreferences("classvoice_settings", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "CloudflareRelayClient"
        const val PREF_RELAY_URL = "pref_relay_url"
        const val PREF_RELAY_TOKEN = "pref_relay_token"
        const val PREF_USE_RELAY = "pref_use_relay"
        const val PREF_SELECTED_MODEL = "pref_selected_model"

        const val PREF_CUSTOM_API_KEY = "pref_custom_api_key"

        // Default Cloudflare Worker Relay endpoint
        const val DEFAULT_WORKER_URL = "https://gemini-ai-relay.workers.dev"
        const val DEFAULT_MODEL = "gemini-3.5-transcribe"
        const val FALLBACK_MODEL = "gemini-3.8-flash"
        const val SECOND_FALLBACK_MODEL = "gemini-3.5-flash"

        // Ready-to-deploy Cloudflare Worker Relay Script
        const val CLOUDFLARE_WORKER_CODE = """/**
 * Cloudflare Worker Relay for Gemini Audio Transcription
 * App: ClassVoice (کلاس‌یار)
 * 
 * Instructions:
 * 1. Go to https://dash.cloudflare.com -> Workers & Pages -> Create Application -> Create Worker
 * 2. Paste this entire code into the Worker editor.
 * 3. (Optional) In Settings -> Variables, add Secret: GEMINI_API_KEY
 * 4. Deploy and copy your worker URL (e.g., https://my-worker.subdomain.workers.dev)
 * 5. Paste the URL into the ClassVoice app Settings.
 */
export default {
  async fetch(request, env, ctx) {
    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, X-Goog-Api-Key, Authorization",
        },
      });
    }

    const url = new URL(request.url);

    // Health check endpoint for testing connection
    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/health")) {
      return new Response(JSON.stringify({ 
        status: "active", 
        service: "ClassVoice Cloudflare Relay",
        version: "1.0.0" 
      }), {
        headers: { 
          "Content-Type": "application/json", 
          "Access-Control-Allow-Origin": "*" 
        }
      });
    }

    if (request.method !== "POST") {
      return new Response("Method Not Allowed", { status: 405 });
    }

    try {
      // 1. Retrieve API Key from header, query param, or Worker env secret
      const apiKey = request.headers.get("X-Goog-Api-Key") || 
                     url.searchParams.get("key") || 
                     env.GEMINI_API_KEY;

      if (!apiKey) {
        return new Response(JSON.stringify({ 
          error: "API Key missing. Provide via X-Goog-Api-Key header or set GEMINI_API_KEY in Worker variables." 
        }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
        });
      }

      // 2. Extract model name from URL or fallback to gemini-3.5-transcribe
      let model = "gemini-3.5-transcribe";
      const modelMatch = url.pathname.match(/models\/([^:]+)/);
      if (modelMatch && modelMatch[1]) {
        model = modelMatch[1];
      }

      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${'$'}{model}:generateContent?key=${'$'}{apiKey}`;

      const bodyText = await request.text();

      // 3. Relay request to Google Gemini API
      const geminiResponse = await fetch(geminiUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: bodyText,
      });

      const responseBody = await geminiResponse.text();

      return new Response(responseBody, {
        status: geminiResponse.status,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      });
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { 
          "Content-Type": "application/json", 
          "Access-Control-Allow-Origin": "*" 
        },
      });
    }
  },
};
"""
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun getWorkerRelayUrl(): String {
        return prefs.getString(PREF_RELAY_URL, DEFAULT_WORKER_URL) ?: DEFAULT_WORKER_URL
    }

    fun setWorkerRelayUrl(url: String) {
        prefs.edit().putString(PREF_RELAY_URL, url.trim()).apply()
    }

    fun getWorkerRelayToken(): String {
        return prefs.getString(PREF_RELAY_TOKEN, "") ?: ""
    }

    fun setWorkerRelayToken(token: String) {
        prefs.edit().putString(PREF_RELAY_TOKEN, token.trim()).apply()
    }

    fun getCustomApiKey(): String {
        return prefs.getString(PREF_CUSTOM_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(PREF_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getEffectiveApiKey(): String {
        val custom = getCustomApiKey()
        if (custom.isNotBlank()) return custom
        return try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
    }

    fun isRelayEnabled(): Boolean {
        return prefs.getBoolean(PREF_USE_RELAY, false)
    }

    fun setRelayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_USE_RELAY, enabled).apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(PREF_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(PREF_SELECTED_MODEL, model).apply()
    }

    fun detectMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg", "oga" -> "audio/ogg"
            "flac" -> "audio/flac"
            "m4a", "mp4" -> "audio/mp4"
            "amr" -> "audio/amr"
            "3gp", "3gpp" -> "audio/3gpp"
            else -> "audio/mp4"
        }
    }

    /**
     * Transcribes an audio file into clean text using Direct Gemini API (or Relay if enabled).
     * Supports all audio formats: MP3, M4A, WAV, AAC, OGG, FLAC, AMR, etc.
     */
    suspend fun transcribeAudio(
        audioFile: File,
        courseName: String = "",
        sessionNumber: Int = 1
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(Exception("فایل صوتی یافت نشد یا خالی است."))
        }

        if (audioFile.length() > 25 * 1024 * 1024L) {
            return@withContext Result.failure(Exception("حجم فایل صوتی بیش از ۲۵ مگابایت است. لطفاً فایل کوتاه‌تری انتخاب کنید."))
        }

        try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val mimeType = detectMimeType(audioFile)

            val prompt = """
                شما یک دستیار حرفه‌ای پیاده‌سازی صوت کلاس‌های دانشگاهی و درسی به متن هستید.
                نام درس: $courseName
                شماره جلسه: $sessionNumber
                
                لطفاً صدای ضبط‌شده را با دقت بسیار بالا، واژه به واژه و با املای صحیح فارسی، پاراگراف‌بندی مناسب و رعایت علائم نگارشی به متن تبدیل کنید.
                اصطلاحات تخصصی، علمی و نام‌ها را دقیق بنویسید.
                فقط و فقط متن پیاده‌شده سخنان استاد و کلاس را بدون هیچ‌گونه مقدمه، نتیجه‌گیری یا توضیحات اضافی ارائه دهید.
            """.trimIndent()

            val requestJson = buildGeminiPayload(prompt, base64Audio, mimeType)

            val relayEnabled = isRelayEnabled()
            val workerUrl = getWorkerRelayUrl()
            val workerToken = getWorkerRelayToken()
            val modelName = getSelectedModel()
            val apiKey = getEffectiveApiKey()

            var lastError: Exception? = null

            // 1. If user explicitly enabled worker relay, use worker
            if (relayEnabled && workerUrl.isNotBlank()) {
                try {
                    val result = callWorkerRelay(workerUrl, workerToken, modelName, apiKey, requestJson)
                    if (result.isNotBlank()) {
                        return@withContext Result.success(result)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cloudflare Worker relay call failed: ${e.message}, falling back to direct API", e)
                    lastError = e
                }
            }

            // 2. Direct Gemini API (Default mode)
            try {
                val directResult = callDirectGeminiApi(modelName, apiKey, requestJson)
                if (directResult.isNotBlank()) {
                    return@withContext Result.success(directResult)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct Gemini call failed for $modelName: ${e.message}, trying fallback model", e)
                lastError = e
            }

            // Try with fallback model gemini-3.8-flash if primary failed or was transcribe
            if (modelName != FALLBACK_MODEL) {
                try {
                    val fallbackResult = callDirectGeminiApi(FALLBACK_MODEL, apiKey, requestJson)
                    if (fallbackResult.isNotBlank()) {
                        return@withContext Result.success(fallbackResult)
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }

            // Second fallback to gemini-3.5-flash
            if (modelName != SECOND_FALLBACK_MODEL) {
                try {
                    val secondFallbackResult = callDirectGeminiApi(SECOND_FALLBACK_MODEL, apiKey, requestJson)
                    if (secondFallbackResult.isNotBlank()) {
                        return@withContext Result.success(secondFallbackResult)
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }

            Result.failure(lastError ?: Exception("خطا در برقراری ارتباط با سرویس تبدیل صوت به متن."))
        } catch (t: Throwable) {
            Log.e(TAG, "Error in transcribeAudio", t)
            Result.failure(Exception(t.localizedMessage ?: "خطای حافظه یا پردازش در تبدیل صوت."))
        }
    }

    private fun buildGeminiPayload(prompt: String, base64Audio: String, mimeType: String): String {
        val root = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        // Text Part
        val textPart = JSONObject()
        textPart.put("text", prompt)
        partsArray.put(textPart)

        // Audio InlineData Part with dynamic MIME type
        val audioPart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", mimeType)
        inlineData.put("data", base64Audio)
        audioPart.put("inlineData", inlineData)
        partsArray.put(audioPart)

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        return root.toString()
    }

    private fun callWorkerRelay(
        workerUrl: String,
        workerToken: String,
        modelName: String,
        apiKey: String,
        requestBodyJson: String
    ): String {
        val cleanUrl = workerUrl.trimEnd('/')
        val targetUrl = if (cleanUrl.contains("generateContent")) {
            if (!cleanUrl.contains("key=") && apiKey.isNotBlank()) {
                "$cleanUrl?key=$apiKey"
            } else cleanUrl
        } else {
            val sep = if (cleanUrl.contains("?")) "&" else "?"
            val keyParam = if (apiKey.isNotBlank()) "${sep}key=$apiKey" else ""
            "$cleanUrl/v1beta/models/$modelName:generateContent$keyParam"
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        val reqBuilder = Request.Builder()
            .url(targetUrl)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("X-Goog-Api-Key", apiKey)
        }
        if (workerToken.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $workerToken")
        }

        val response = okHttpClient.newCall(reqBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("رله کلودفلر با خطا مواجه شد (${response.code}): $responseBody")
        }

        return parseGeminiResponse(responseBody)
    }

    private fun callDirectGeminiApi(
        modelName: String,
        apiKey: String,
        requestBodyJson: String
    ): String {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("کلید API جمینای تنظیم نشده است.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw Exception("پاسخ سرور جمینای (${response.code}): $responseBody")
        }

        return parseGeminiResponse(responseBody)
    }

    private fun parseGeminiResponse(jsonString: String): String {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val text = part.optString("text", "")
            if (text.isNotBlank()) {
                sb.append(text).append("\n")
            }
        }

        return sb.toString().trim()
    }
}
