package com.ehansih.whatsapprules

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Multi-provider AI reply engine.
 *
 * Supported providers (free first):
 *  - GROQ      — FREE 14,400 req/day  | Llama 3.3 70B  | groq.com
 *  - GEMINI    — FREE 1,500 req/day   | Gemini 1.5 Flash | aistudio.google.com
 *  - DEEPSEEK  — FREE credits on signup, then ~$0.014/1M tokens | platform.deepseek.com
 *  - OPENAI    — Paid ($0.15/1M)      | GPT-4o Mini    | platform.openai.com
 *  - CLAUDE    — Paid ($0.80/1M)      | Haiku 4.5      | console.anthropic.com
 */
enum class AiProvider(
    val displayName: String,
    val isFree: Boolean,
    val prefKey: String
) {
    GROQ("Groq — Llama 3.3 (FREE)", true, "key_groq"),
    GEMINI("Gemini 1.5 Flash (FREE)", true, "key_gemini"),
    DEEPSEEK("DeepSeek V3 (near-free)", true, "key_deepseek"),
    OPENAI("OpenAI GPT-4o Mini (paid)", false, "key_openai"),
    CLAUDE("Claude Haiku (paid)", false, "key_claude"),
}

object AiReplyEngine {

    private const val TAG = "WARules.AI"
    private const val PREFS = "wa_rules_prefs"
    private const val KEY_PROVIDER = "ai_provider"

    // ── API endpoints + models ────────────────────────────────────────────────
    private const val GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions"
    private const val GEMINI_URL   = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
    private const val DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
    private const val OPENAI_URL   = "https://api.openai.com/v1/chat/completions"
    private const val CLAUDE_URL   = "https://api.anthropic.com/v1/messages"

    private const val GROQ_MODEL     = "llama-3.3-70b-versatile"
    private const val DEEPSEEK_MODEL = "deepseek-chat"
    private const val OPENAI_MODEL   = "gpt-4o-mini"
    private const val CLAUDE_MODEL   = "claude-haiku-4-5-20251001"

    private val SYSTEM_PROMPT = """
You are a smart auto-reply assistant for Harsh Vardhan Singh Chauhan (cybersecurity & AI engineer).
He is currently unavailable. Read the WhatsApp message and write a SHORT, warm, natural auto-reply.

Rules:
- Address the sender by first name
- Be friendly, human, empathetic — never robotic
- If distress/crisis/emergency detected: include "If urgent: iCall 9152987821"
- Business/work query: say Harsh will follow up soon
- Casual message: keep it light and friendly
- Max 2-3 sentences, no formal sign-off
    """.trimIndent()

    // ── Prefs helpers (AES-256 encrypted storage for API keys) ───────────────
    private fun prefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to plain prefs if encryption unavailable (shouldn't happen on API 26+)
        Log.w(TAG, "EncryptedSharedPreferences unavailable, using plain: ${e.message}")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun saveApiKey(context: Context, provider: AiProvider, key: String) =
        prefs(context).edit().putString(provider.prefKey, key).apply()

    fun getApiKey(context: Context, provider: AiProvider): String =
        prefs(context).getString(provider.prefKey, "") ?: ""

    fun hasApiKey(context: Context, provider: AiProvider) =
        getApiKey(context, provider).isNotBlank()

    fun saveProvider(context: Context, provider: AiProvider) =
        prefs(context).edit().putString(KEY_PROVIDER, provider.name).apply()

    fun getProvider(context: Context): AiProvider {
        val name = prefs(context).getString(KEY_PROVIDER, AiProvider.GROQ.name)
        return AiProvider.values().firstOrNull { it.name == name } ?: AiProvider.GROQ
    }

    // ── Main entry point ──────────────────────────────────────────────────────
    suspend fun generateReply(
        context: Context,
        senderName: String,
        incomingMessage: String,
        fallbackReply: String,
        provider: AiProvider = getProvider(context)
    ): String {
        val apiKey = getApiKey(context, provider)
        if (apiKey.isBlank()) {
            Log.w(TAG, "No API key for $provider — using fallback")
            return applyPlaceholders(fallbackReply, senderName, incomingMessage)
        }

        val userPrompt = "Sender: $senderName\nMessage: \"$incomingMessage\"\n\nWrite the auto-reply:"

        return try {
            when (provider) {
                AiProvider.CLAUDE  -> callClaude(apiKey, userPrompt)
                AiProvider.GEMINI  -> callGemini(apiKey, userPrompt)
                else               -> callOpenAiCompat(provider, apiKey, userPrompt)
            } ?: applyPlaceholders(fallbackReply, senderName, incomingMessage)
        } catch (e: Exception) {
            Log.e(TAG, "${provider.name} failed: ${e.message} — using fallback")
            applyPlaceholders(fallbackReply, senderName, incomingMessage)
        }
    }

    // ── Provider implementations ──────────────────────────────────────────────

    /** Groq, DeepSeek, OpenAI — all use OpenAI-compatible chat/completions format */
    private fun callOpenAiCompat(provider: AiProvider, apiKey: String, userPrompt: String): String? {
        val url = when (provider) {
            AiProvider.GROQ     -> GROQ_URL
            AiProvider.DEEPSEEK -> DEEPSEEK_URL
            AiProvider.OPENAI   -> OPENAI_URL
            else                -> return null
        }
        val model = when (provider) {
            AiProvider.GROQ     -> GROQ_MODEL
            AiProvider.DEEPSEEK -> DEEPSEEK_MODEL
            AiProvider.OPENAI   -> OPENAI_MODEL
            else                -> GROQ_MODEL
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 150)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val response = post(url, body, mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type"  to "application/json"
        )) ?: return null

        return JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .also { Log.d(TAG, "${provider.name} reply: \"$it\"") }
    }

    private fun callClaude(apiKey: String, userPrompt: String): String? {
        val body = JSONObject().apply {
            put("model", CLAUDE_MODEL)
            put("max_tokens", 150)
            put("system", SYSTEM_PROMPT)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val response = post(CLAUDE_URL, body, mapOf(
            "x-api-key"         to apiKey,
            "anthropic-version" to "2023-06-01",
            "Content-Type"      to "application/json"
        )) ?: return null

        return JSONObject(response)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .also { Log.d(TAG, "Claude reply: \"$it\"") }
    }

    private fun callGemini(apiKey: String, userPrompt: String): String? {
        val urlWithKey = "$GEMINI_URL?key=$apiKey"

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$SYSTEM_PROMPT\n\n$userPrompt")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 150)
                put("temperature", 0.7)
            })
        }

        val response = post(urlWithKey, body, mapOf("Content-Type" to "application/json")) ?: return null

        return JSONObject(response)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .also { Log.d(TAG, "Gemini reply: \"$it\"") }
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────
    private fun post(urlStr: String, body: JSONObject, headers: Map<String, String>): String? {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val code = conn.responseCode
        return if (code == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "no error body"
            Log.e(TAG, "HTTP $code from $urlStr: $err")
            null
        }
    }

    // ── Placeholder replacement ───────────────────────────────────────────────
    fun applyPlaceholders(template: String, senderName: String, incomingMessage: String): String {
        val firstName = senderName.split(" ").firstOrNull()?.trim() ?: senderName
        return template
            .replace("{name}", firstName, ignoreCase = true)
            .replace("{fullname}", senderName, ignoreCase = true)
            .replace("{message}", incomingMessage, ignoreCase = true)
    }
}
