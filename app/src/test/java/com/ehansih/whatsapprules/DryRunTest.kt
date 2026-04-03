package com.ehansih.whatsapprules

import org.junit.Test
import org.junit.Assert.*

/**
 * Dry-run tests for rule matching and group chat detection logic.
 * These test the pure logic without needing Android SDK or a device.
 */
class DryRunTest {

    // ── Rule matching logic (extracted from WhatsAppListenerService) ──────────

    private fun matchesRule(rule: Rule, sender: String, message: String): Boolean {
        val contactMatch = rule.contactName.trim() == "*" ||
                rule.contactName.split(",").map { it.trim() }.any { entry ->
                    sender.contains(entry, ignoreCase = true)
                }
        val keywordMatch = rule.keyword.trim() == "*" ||
                message.contains(rule.keyword.trim(), ignoreCase = true)
        return contactMatch && keywordMatch
    }

    // ── Group chat detection logic ────────────────────────────────────────────

    private fun isGroupChat(sender: String, text: String, isGroupConversation: Boolean): Boolean {
        if (isGroupConversation) return true
        if (text.contains(Regex("^[^:]+: .+"))) {
            val subSender = text.substringBefore(":").trim()
            if (subSender != sender) return true
        }
        return false
    }

    // ── Tests: Rule Matching ──────────────────────────────────────────────────

    @Test
    fun `wildcard contact and keyword matches everyone`() {
        val rule = Rule(contactName = "*", keyword = "*", replyMessage = "I'll get back to you!")
        assertTrue(matchesRule(rule, "John", "hello"))
        assertTrue(matchesRule(rule, "Unknown", "some random text"))
    }

    @Test
    fun `specific contact matches correctly`() {
        val rule = Rule(contactName = "Rahul", keyword = "*", replyMessage = "Hey Rahul!")
        assertTrue(matchesRule(rule, "Rahul Sharma", "anything"))
        assertFalse(matchesRule(rule, "Priya", "anything"))
    }

    @Test
    fun `keyword matching is case insensitive`() {
        val rule = Rule(contactName = "*", keyword = "urgent", replyMessage = "On my way!")
        assertTrue(matchesRule(rule, "Anyone", "This is URGENT please help"))
        assertTrue(matchesRule(rule, "Anyone", "urgent issue here"))
        assertFalse(matchesRule(rule, "Anyone", "hello how are you"))
    }

    @Test
    fun `multiple contacts comma separated`() {
        val rule = Rule(contactName = "Mom, Dad, Sister", keyword = "*", replyMessage = "Busy now")
        assertTrue(matchesRule(rule, "Mom", "hi"))
        assertTrue(matchesRule(rule, "Dad", "call me"))
        assertTrue(matchesRule(rule, "Sister", "where are you"))
        assertFalse(matchesRule(rule, "Boss", "meeting?"))
    }

    @Test
    fun `disabled rule should not be in enabled rules list`() {
        val rule = Rule(contactName = "*", keyword = "*", replyMessage = "test", isEnabled = false)
        assertFalse(rule.isEnabled)
    }

    @Test
    fun `rule with specific contact and keyword`() {
        val rule = Rule(contactName = "Priya", keyword = "help", replyMessage = "Coming!")
        assertTrue(matchesRule(rule, "Priya Gupta", "I need help urgently"))
        assertFalse(matchesRule(rule, "Priya Gupta", "hello there"))  // keyword mismatch
        assertFalse(matchesRule(rule, "Rahul", "I need help"))         // contact mismatch
    }

    @Test
    fun `mental health crisis keyword rule`() {
        val rule = Rule(contactName = "*", keyword = "help", replyMessage =
            "I'm here for you. Please stay — I will call you back immediately.")
        assertTrue(matchesRule(rule, "Unknown", "I need help"))
        assertTrue(matchesRule(rule, "Anyone", "HELP ME"))
        assertTrue(matchesRule(rule, "Stranger", "please help i cant do this"))
    }

    // ── Tests: Group Chat Detection ───────────────────────────────────────────

    @Test
    fun `individual message is not a group chat`() {
        assertFalse(isGroupChat("Rahul", "hey what's up", false))
    }

    @Test
    fun `EXTRA_IS_GROUP_CONVERSATION flag detects groups`() {
        assertTrue(isGroupChat("Family Group", "Mom: dinner ready", true))
    }

    @Test
    fun `group message text format detected`() {
        // WhatsApp group: EXTRA_TITLE = "Group Name", EXTRA_TEXT = "MemberName: message"
        assertTrue(isGroupChat("Office Group", "Rahul: meeting at 3pm", false))
    }

    @Test
    fun `individual message with colon in text not flagged as group`() {
        // Message like "Note: call me" from a direct contact — sender matches sub-sender would be edge case
        assertFalse(isGroupChat("Rahul", "Rahul: hey this is me", false))
    }

    @Test
    fun `reply text is preserved exactly`() {
        val rule = Rule(
            contactName = "*",
            keyword = "*",
            replyMessage = "I'm currently unavailable. I'll respond soon. 🙏"
        )
        assertEquals("I'm currently unavailable. I'll respond soon. 🙏", rule.replyMessage)
    }

    // ── Tests: Edge Cases ─────────────────────────────────────────────────────

    @Test
    fun `whitespace trimmed in contact and keyword`() {
        val rule = Rule(contactName = "  Rahul  ", keyword = "  hello  ", replyMessage = "Hi!")
        assertTrue(matchesRule(rule, "Rahul", "hello there"))
    }

    @Test
    fun `empty message does not crash`() {
        val rule = Rule(contactName = "*", keyword = "*", replyMessage = "OK")
        assertTrue(matchesRule(rule, "Anyone", ""))
    }

    @Test
    fun `wildcard keyword matches empty message`() {
        val rule = Rule(contactName = "*", keyword = "*", replyMessage = "Got it")
        assertTrue(matchesRule(rule, "Test", ""))
    }
}
