package com.example.gymworkout.data

import org.json.JSONArray
import org.json.JSONObject

data class DescriptionChecklistItem(val text: String, val checked: Boolean)

/**
 * Holds both a free-text description and a checklist for a habit.
 * Both sides persist together; [mode] only controls which view is active in the UI.
 */
data class HabitDescription(
    val text: String = "",
    val items: List<DescriptionChecklistItem> = emptyList(),
    val mode: String = "text"
) {
    fun withText(newText: String) = copy(text = newText)
    fun withItems(newItems: List<DescriptionChecklistItem>) = copy(items = newItems)
    fun withMode(newMode: String) = copy(mode = newMode)

    fun toJson(): String = JSONObject().apply {
        put("value", text)
        put("items", JSONArray().apply {
            items.forEach { put(JSONObject().put("text", it.text).put("checked", it.checked)) }
        })
    }.toString()

    companion object {
        fun parse(mode: String, raw: String): HabitDescription {
            val resolvedMode = if (mode == "checklist") "checklist" else "text"
            if (raw.isBlank()) return HabitDescription(mode = resolvedMode)
            return try {
                val obj = JSONObject(raw)
                val text = obj.optString("value", "")
                val arr = obj.optJSONArray("items") ?: JSONArray()
                val items = (0 until arr.length()).map { i ->
                    val item = arr.getJSONObject(i)
                    DescriptionChecklistItem(
                        text = item.optString("text", ""),
                        checked = item.optBoolean("checked", false)
                    )
                }
                HabitDescription(text = text, items = items, mode = resolvedMode)
            } catch (_: Exception) {
                // Legacy/non-JSON content: treat as plain text.
                HabitDescription(text = raw, mode = resolvedMode)
            }
        }
    }
}
