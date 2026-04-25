package com.example.gymworkout.data

import org.json.JSONArray
import org.json.JSONObject

data class DescriptionChecklistItem(val text: String, val checked: Boolean)

sealed class HabitDescription {
    data class Text(val value: String) : HabitDescription()
    data class Checklist(val items: List<DescriptionChecklistItem>) : HabitDescription()

    val mode: String get() = when (this) {
        is Text -> "text"
        is Checklist -> "checklist"
    }

    fun toJson(): String = when (this) {
        is Text -> JSONObject().put("value", value).toString()
        is Checklist -> JSONObject().put(
            "items",
            JSONArray().apply {
                items.forEach { put(JSONObject().put("text", it.text).put("checked", it.checked)) }
            }
        ).toString()
    }

    companion object {
        fun parse(mode: String, raw: String): HabitDescription {
            if (raw.isBlank()) {
                return if (mode == "checklist") Checklist(emptyList()) else Text("")
            }
            return try {
                if (mode == "checklist") {
                    val arr = JSONObject(raw).optJSONArray("items") ?: JSONArray()
                    val items = (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        DescriptionChecklistItem(
                            text = obj.optString("text", ""),
                            checked = obj.optBoolean("checked", false)
                        )
                    }
                    Checklist(items)
                } else {
                    Text(JSONObject(raw).optString("value", ""))
                }
            } catch (_: Exception) {
                if (mode == "checklist") Checklist(emptyList()) else Text(raw)
            }
        }
    }
}
