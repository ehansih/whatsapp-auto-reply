package com.ehansih.whatsapprules

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Generates intelligent auto-replies using Claude API.
 * Falls back to the rule's replyMessage if AI fails.
 */
object AiReplyEngine {

    private const val TAG = "WARules.AI"
    private const val PREFS = "wa_rules_prefs"
    private const val KEY_API_KEY = "claude_api_key"
    private const val CLAUDE_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5-20251001" // fast + cheap for auto-replies

    fun saveApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, "") ?: ""
    }

    fun hasApiKey(context: Context) = getApiKey(context).isNotBlank()

    /**
     * Generates a contextual reply using Claude.
     * Returns null if API key missing or request fails (caller uses fallback).
     */
    suspend fun generateReply(
        context: Context,
        senderName: String,
        incomingMessage: String,
        fallbackReply: String
    ): String {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            Log.w(TAG, "No API key set — using fallback reply")
            return applyPlaceholders(fallbackReply, senderName, incomingMessage)
        }

        return try {
            val systemPrompt = """
You are a smart auto-reply assistant for Harsh Vardhan Singh Chauhan (a cybersecurity & AI engineer).
He is currently unavailable. Your job: read the incoming WhatsApp message and write a SHORT,
warm, natural auto-reply (2-3 sentences max).

Rules:
- Always address the sender by their first name if provided
- Be friendly, human, and empathetic — never robotic
- If the message sounds like distress, crisis, or emergency: add "If urgent, call: +91-iCall: 9152987821"
- If it's a business/work query: say Harsh will follow up soon
- If it's casual: keep it light and friendly
- Never make promises you can't keep (e.g. "I'll call in 5 mins")
- Sign off naturally — no formal signatures needed
- Keep it under 3 sentences
            """.trimIndent()

            val userPrompt = "Sender: $senderName\nMessage: \"$incomingMessage\"\n\nWrite the auto-reply:"

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 150)
                put("system", systemPrompt)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            }

            val url = URL(CLAUDE_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 10000

            OutputStreamWriter(conn.outputStream).use { it.write(requestBody.toString()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val content = JSONObject(response)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                Log.d(TAG, "AI reply generated: \"$content\"")
                content
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "unknown"
                Log.e(TAG, "Claude API error $responseCode: $error")
                applyPlaceholders(fallbackReply, senderName, incomingMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI reply failed: ${e.message} — using fallback")
            applyPlaceholders(fallbackReply, senderName, incomingMessage)
        }
    }

    /**
     * Replaces {name} and {message} placeholders in the reply template.
     * Works for both AI-off and AI-on (as fallback).
     */
    fun applyPlaceholders(template: String, senderName: String, incomingMessage: String): String {
        val firstName = senderName.split(" ").firstOrNull()?.trim() ?: senderName
        return template
            .replace("{name}", firstName, ignoreCase = true)
            .replace("{fullname}", senderName, ignoreCase = true)
            .replace("{message}", incomingMessage, ignoreCase = true)
    }
}
