package com.example.gymworkout.ai

import android.content.Context
import android.os.Build
import com.example.gymworkout.data.FoodDatabase
import com.example.gymworkout.data.FoodItem
import com.example.gymworkout.data.ServingUnit
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime

/**
 * On-device AI planner that generates creative suggestions to help users
 * reach their daily nutrition and habit targets.
 *
 * Uses Gemini Nano (via AICore) on supported devices for creative, varied suggestions.
 * Falls back to a smart rule-based engine on unsupported devices.
 */
object AiPlannerEngine {

    data class TargetProgress(
        val label: String,
        val unit: String,
        val current: Float,
        val target: Float,
        val isNutrition: Boolean
    ) {
        val remaining get() = (target - current).coerceAtLeast(0f)
        val percentage get() = if (target > 0) (current / target * 100).coerceIn(0f, 100f) else 0f
        val isComplete get() = current >= target
    }

    data class PlannerResult(
        val suggestions: List<String>,
        val isFromLlm: Boolean,
        val summary: String
    )

    private var generativeModel: GenerativeModel? = null
    private var modelAvailable: Boolean? = null

    /**
     * Check if Gemini Nano is available on this device.
     */
    suspend fun isNanoAvailable(context: Context): Boolean {
        if (modelAvailable != null) return modelAvailable!!
        if (Build.VERSION.SDK_INT < 31) {
            modelAvailable = false
            return false
        }
        return try {
            withContext(Dispatchers.IO) {
                val model = GenerativeModel(
                    generationConfig = generationConfig {
                        temperature = 0.9f
                        topK = 16
                        maxOutputTokens = 1024
                    }
                )
                generativeModel = model
                modelAvailable = true
                true
            }
        } catch (_: Exception) {
            modelAvailable = false
            false
        }
    }

    /**
     * Generate suggestions using Gemini Nano or fallback.
     */
    suspend fun generateSuggestions(
        context: Context,
        progressList: List<TargetProgress>
    ): PlannerResult {
        val incomplete = progressList.filter { !it.isComplete }
        val completed = progressList.filter { it.isComplete }

        if (incomplete.isEmpty()) {
            return PlannerResult(
                suggestions = listOf(
                    "All targets complete! You've crushed every goal today.",
                    "Consider setting stretch goals for tomorrow to keep pushing.",
                    "Great discipline! Consistency like this builds lasting results."
                ),
                isFromLlm = false,
                summary = "All ${progressList.size} targets hit!"
            )
        }

        val hour = LocalTime.now().hour
        val timeOfDay = when {
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }

        // Try Gemini Nano first
        if (isNanoAvailable(context)) {
            try {
                val result = generateWithNano(incomplete, completed, timeOfDay)
                if (result != null) return result
            } catch (_: Exception) {
                // Fall through to rule-based
            }
        }

        // Fallback: rule-based suggestions
        return generateRuleBased(incomplete, completed, timeOfDay)
    }

    private suspend fun generateWithNano(
        incomplete: List<TargetProgress>,
        completed: List<TargetProgress>,
        timeOfDay: String
    ): PlannerResult? {
        val model = generativeModel ?: return null

        val prompt = buildPrompt(incomplete, completed, timeOfDay)

        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text?.trim() ?: return@withContext null

                val suggestions = text.lines()
                    .map { it.trim().removePrefix("-").removePrefix("*").removePrefix("1.").removePrefix("2.").removePrefix("3.").removePrefix("4.").removePrefix("5.").removePrefix("6.").removePrefix("7.").removePrefix("8.").trim() }
                    .filter { it.isNotBlank() && it.length > 10 }

                if (suggestions.isEmpty()) return@withContext null

                PlannerResult(
                    suggestions = suggestions.take(8),
                    isFromLlm = true,
                    summary = "${incomplete.size} targets remaining this $timeOfDay"
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun buildPrompt(
        incomplete: List<TargetProgress>,
        completed: List<TargetProgress>,
        timeOfDay: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("You are a concise fitness and nutrition coach. It's $timeOfDay.")
        sb.appendLine("The user has these REMAINING targets for today:")
        sb.appendLine()

        for (t in incomplete) {
            val pct = t.percentage.toInt()
            sb.appendLine("- ${t.label}: ${formatValue(t.current)}/${formatValue(t.target)} ${t.unit} ($pct% done, need ${formatValue(t.remaining)} ${t.unit} more)")

            // Add top food sources from our database for nutrition targets
            val topFoods = getTopFoodsForTarget(t, 3)
            if (topFoods.isNotEmpty()) {
                val foodList = topFoods.joinToString(", ") { (food, value, amountNeeded) ->
                    val serving = if (food.servingUnit == ServingUnit.PIECE) "pc" else "g"
                    "${food.name} (${formatValue(value)} ${t.unit}/100$serving, eat ~${amountNeeded.toInt()}$serving to fill gap)"
                }
                sb.appendLine("  Top foods: $foodList")
            }
        }

        if (completed.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Already completed: ${completed.joinToString(", ") { it.label }}")
        }

        sb.appendLine()
        sb.appendLine("Give 5-7 creative, specific, actionable suggestions to help reach the remaining targets.")
        sb.appendLine("Use the food data above to suggest specific meals, combos, and snacks with exact amounts.")
        sb.appendLine("Combine multiple foods into meal ideas (e.g. 'spinach + carrot salad with eggs').")
        sb.appendLine("Include practical tips and time-based advice for habit targets.")
        sb.appendLine("Keep each suggestion to 1-2 sentences. Be encouraging but concise.")
        sb.appendLine("Do NOT use numbering or bullet markers. One suggestion per line.")

        return sb.toString()
    }

    private fun generateRuleBased(
        incomplete: List<TargetProgress>,
        completed: List<TargetProgress>,
        timeOfDay: String
    ): PlannerResult {
        val suggestions = mutableListOf<String>()

        for (target in incomplete) {
            suggestions.addAll(getSuggestionsForTarget(target, timeOfDay))
        }

        // Add motivational suggestion based on completed count
        if (completed.isNotEmpty()) {
            suggestions.add(
                "You've already hit ${completed.size} target${if (completed.size > 1) "s" else ""} (${completed.joinToString(", ") { it.label }}) — keep that momentum going for the rest!"
            )
        }

        // Time-based general tips
        when (timeOfDay) {
            "morning" -> suggestions.add("You have the whole day ahead. Start strong with a nutrient-dense breakfast to front-load your targets.")
            "afternoon" -> suggestions.add("Afternoon is a great time to check in. A solid lunch or snack can close the gap on multiple targets at once.")
            "evening" -> suggestions.add("Evening push! Focus on the targets closest to completion first for quick wins before bed.")
        }

        return PlannerResult(
            suggestions = suggestions.shuffled().take(8),
            isFromLlm = false,
            summary = "${incomplete.size} targets remaining this $timeOfDay"
        )
    }

    private fun getSuggestionsForTarget(target: TargetProgress, timeOfDay: String): List<String> {
        val remaining = target.remaining
        val label = target.label.lowercase()
        val pct = target.percentage.toInt()

        return when {
            // WATER
            label == "water" -> listOf(
                when {
                    remaining >= 2f -> "You need ${formatValue(remaining)}L of water. Fill a 1L bottle and aim to finish it in the next 2 hours, then refill."
                    remaining >= 1f -> "About ${formatValue(remaining)}L to go. Drink a full glass now, then one every 30 minutes."
                    else -> "Almost there! Just ${formatValue(remaining)}L left. A couple of glasses and you're done."
                },
                when (timeOfDay) {
                    "morning" -> "Keep a water bottle at your desk and sip between tasks. Adding lemon or cucumber can make it more enjoyable."
                    "afternoon" -> "Pair every snack or meal with a full glass of water to passively hit your target."
                    else -> "Have a glass of water with dinner and keep one on your nightstand."
                }
            )

            // CALORIES
            label == "calories" -> {
                val topCal = getTopFoodsForTarget(target, 5)
                val suggestions = mutableListOf<String>()
                if (topCal.isNotEmpty()) {
                    val best = topCal[0]
                    suggestions.add(
                        "You need ${remaining.toInt()} more cal. Top calorie source: ${best.first.name} " +
                        "(${formatValue(best.second)} cal/100g) — eat ~${best.third.toInt()}g to fill the gap."
                    )
                    if (topCal.size >= 2) {
                        val meal = topCal.take(2)
                        suggestions.add(
                            "Meal idea: ${meal[0].third.toInt().coerceAtMost(150)}g ${meal[0].first.name} + " +
                            "${meal[1].third.toInt().coerceAtMost(150)}g ${meal[1].first.name} for a calorie-dense combo."
                        )
                    }
                } else {
                    suggestions.add("You need ${remaining.toInt()} more cal. Consider calorie-dense foods like nuts, rice, or a banana shake.")
                }
                suggestions
            }

            // PROTEIN
            label == "protein" -> {
                val topPro = getTopFoodsForTarget(target, 5)
                val suggestions = mutableListOf<String>()
                if (topPro.isNotEmpty()) {
                    val best = topPro[0]
                    val bestUnit = if (best.first.servingUnit == ServingUnit.PIECE) "piece(s)" else "g"
                    suggestions.add(
                        "You need ${remaining.toInt()}g more protein. Best source: ${best.first.name} " +
                        "(${formatValue(best.second)}g per ${if (best.first.servingUnit == ServingUnit.PIECE) "piece" else "100g"}) — " +
                        "eat ~${best.third.toInt()}$bestUnit to hit your target."
                    )
                    if (topPro.size >= 3) {
                        val snackOptions = topPro.drop(1).take(3).joinToString(", ") { (f, v, _) ->
                            "${f.name} (${formatValue(v)}g)"
                        }
                        suggestions.add("Other high-protein options: $snackOptions. Mix these into your meals.")
                    }
                } else {
                    suggestions.add("You need ${remaining.toInt()}g more protein. Try chicken, eggs, paneer, or a protein shake.")
                }
                suggestions
            }

            // CARBS
            label == "carbs" -> {
                val topCarb = getTopFoodsForTarget(target, 5)
                val suggestions = mutableListOf<String>()
                if (topCarb.isNotEmpty()) {
                    val best = topCarb[0]
                    suggestions.add(
                        "${remaining.toInt()}g of carbs remaining. Top source: ${best.first.name} " +
                        "(${formatValue(best.second)}g per 100g) — ${best.third.toInt()}g will cover it."
                    )
                    if (topCarb.size >= 2) {
                        val quick = topCarb[1]
                        suggestions.add(
                            "Quick option: ${quick.third.toInt().coerceAtMost(200)}g ${quick.first.name} as a side dish or snack."
                        )
                    }
                } else {
                    suggestions.add("${remaining.toInt()}g of carbs left. Try rice, oats, bread, or a banana.")
                }
                suggestions
            }

            // SLEEP
            label == "sleep" -> listOf(
                when {
                    remaining >= 6 -> "You need ${formatValue(remaining)} hrs of sleep tonight. Set a firm bedtime alarm and start winding down 30 minutes before."
                    remaining >= 2 -> "Aim for ${formatValue(remaining)} more hours. Avoid screens 1 hour before bed, keep the room cool, and try a short breathing exercise."
                    else -> "Just ${formatValue(remaining)} hrs more. Even a power nap can count — or go to bed a bit earlier tonight."
                },
                "Create a sleep-friendly environment: dim lights, cool temperature, no caffeine after 2 PM."
            )

            // STEPS
            label == "steps" -> listOf(
                when {
                    remaining >= 5000 -> "${remaining.toInt()} steps to go. Take a 25-minute brisk walk — that's roughly 2,500 steps. Do it twice or walk during a phone call."
                    remaining >= 2000 -> "${remaining.toInt()} steps left. A quick 10-minute walk around your building or neighborhood will knock this out."
                    else -> "Almost done! Just ${remaining.toInt()} steps. Walk around your house or take the stairs a few times."
                }
            )

            // MEDITATION
            label == "meditation" -> listOf(
                "You need ${remaining.toInt()} minutes of meditation. Try a body scan or box breathing exercise — no apps needed, just close your eyes and focus on your breath.",
                when (timeOfDay) {
                    "morning" -> "Morning meditation sets the tone for the whole day. Even 5 minutes of stillness can make a big difference."
                    "afternoon" -> "A mid-day mindfulness break can reset your focus and reduce afternoon fatigue."
                    else -> "Evening meditation helps you process the day and wind down for better sleep."
                }
            )

            // READING
            label == "reading" -> listOf(
                "You have ${remaining.toInt()} minutes of reading left. Pick up where you left off — even 10 minutes before bed builds a strong habit.",
                "Try replacing 15 minutes of phone scrolling with reading. Keep a book on your nightstand or phone screen as a reminder."
            )

            // WORKOUT / CARDIO / YOGA / STRETCHING
            label in listOf("workout", "cardio", "yoga", "stretching") -> listOf(
                when {
                    remaining >= 30 -> "${remaining.toInt()} min of ${target.label.lowercase()} remaining. Break it into two sessions if needed — consistency beats intensity."
                    remaining >= 15 -> "Just ${remaining.toInt()} min to go. A quick bodyweight circuit or brisk walk can finish this off."
                    else -> "Only ${remaining.toInt()} min left! A quick stretch routine or walk will complete your ${target.label.lowercase()} goal."
                },
                when (timeOfDay) {
                    "morning" -> "Morning workouts boost energy and metabolism for the rest of the day."
                    "afternoon" -> "Afternoon movement fights the post-lunch slump. Even a short walk counts."
                    else -> "If it's late, go easy — a yoga flow or light stretching still counts and won't affect sleep."
                }
            )

            // COLD SHOWER
            label == "cold shower" -> listOf(
                "You need ${remaining.toInt()} min of cold exposure. Start with 30 seconds of cold at the end of your regular shower and build up.",
                "Cold exposure boosts alertness, mood, and recovery. Take deep breaths through it — it gets easier after the first 10 seconds."
            )

            // JOURNALING
            label == "journaling" -> listOf(
                "Spend ${remaining.toInt()} min journaling. Write 3 things you're grateful for, 1 win from today, and 1 thing you'd improve.",
                "Don't overthink it. Set a timer and just write whatever comes to mind. Stream of consciousness works great."
            )

            // CREATINE
            label == "creatine" -> listOf(
                "Take your ${formatValue(remaining)}g of creatine. Mix it with water or your protein shake — timing doesn't matter much, consistency does."
            )

            // BREATHWORK
            label == "breathwork" -> listOf(
                "You need ${remaining.toInt()} min of breathwork. Try the 4-7-8 technique: inhale 4 seconds, hold 7 seconds, exhale 8 seconds. Repeat 4 cycles.",
                "Box breathing works great: 4 seconds in, 4 hold, 4 out, 4 hold. It calms the nervous system and improves focus."
            )

            // SUNLIGHT
            label == "sunlight" -> listOf(
                when (timeOfDay) {
                    "morning" -> "Get ${remaining.toInt()} min of sunlight now! Morning sun is the best for circadian rhythm. Step outside for a walk or sit by a window."
                    "afternoon" -> "Step outside for ${remaining.toInt()} min. Even indirect sunlight through clouds counts for vitamin D and mood."
                    else -> "You missed your sunlight window today. Try to get morning sun first thing tomorrow — it improves sleep and energy."
                }
            )

            // NO SUGAR / NO ALCOHOL
            label in listOf("no sugar", "no alcohol") -> listOf(
                "Keep going with ${target.label} today! If cravings hit, try sparkling water with lemon, herbal tea, or dark chocolate (85%+).",
                "You're ${pct}% through the day without ${label.removePrefix("no ")}. Stay strong — the craving will pass in about 20 minutes."
            )

            // SCREEN LIMIT
            label == "screen limit" -> listOf(
                "You have ${formatValue(remaining)} hrs of screen time left. Be intentional — batch your phone use instead of checking constantly.",
                "Try putting your phone in another room for 30 minutes. You'll be surprised how much focus you gain."
            )

            // NUTRITION WITH FOOD DATABASE (vitamins, minerals, fat, fiber, etc.)
            target.isNutrition -> {
                val topFoods = getTopFoodsForTarget(target, 5)
                if (topFoods.isNotEmpty()) {
                    val suggestions = mutableListOf<String>()

                    // Primary suggestion: best single food
                    val (bestFood, bestValue, bestAmount) = topFoods[0]
                    val bestServing = if (bestFood.servingUnit == ServingUnit.PIECE) "piece(s)" else "g"
                    suggestions.add(
                        "You need ${formatValue(remaining)} ${target.unit} more ${target.label}. " +
                        "Best source: ${bestFood.name} — eat ~${bestAmount.toInt()}$bestServing to hit your target " +
                        "(${formatValue(bestValue)} ${target.unit} per ${if (bestFood.servingUnit == ServingUnit.PIECE) "piece" else "100g"})."
                    )

                    // Meal/snack combo suggestion
                    if (topFoods.size >= 3) {
                        val combo = topFoods.take(3)
                        val comboText = combo.joinToString(" + ") { (f, v, amt) ->
                            val u = if (f.servingUnit == ServingUnit.PIECE) "pc" else "g"
                            "${amt.toInt()}$u ${f.name}"
                        }
                        val comboTotal = combo.sumOf { (f, v, amt) ->
                            (v * amt / if (f.servingUnit == ServingUnit.PIECE) 1f else 100f).toDouble()
                        }.toFloat()
                        suggestions.add(
                            "Combo idea: $comboText — together that's ~${formatValue(comboTotal)} ${target.unit} of ${target.label}."
                        )
                    }

                    // Snack-sized suggestion
                    val snackFoods = topFoods.filter { (f, _, amt) ->
                        amt <= 150 || f.servingUnit == ServingUnit.PIECE
                    }
                    if (snackFoods.isNotEmpty()) {
                        val snack = snackFoods.first()
                        val snackServing = if (snack.first.servingUnit == ServingUnit.PIECE) "piece(s)" else "g"
                        suggestions.add(
                            "Quick snack: ${snack.third.toInt().coerceAtMost(100)}$snackServing of ${snack.first.name} as a ${timeOfDay} snack — " +
                            "easy way to add ${target.label.lowercase()} without a full meal."
                        )
                    }

                    // Progress-based tip
                    suggestions.add(when {
                        pct >= 75 -> "You're at ${pct}% — almost there! Just a small serving of ${topFoods.first().first.name} will close the gap on ${target.label}."
                        pct >= 50 -> "Halfway on ${target.label}. Add ${topFoods.first().first.name} or ${topFoods.getOrElse(1) { topFoods[0] }.first.name} to your next meal."
                        else -> "You're at ${pct}% for ${target.label}. Include ${target.label.lowercase()}-rich foods like ${topFoods.take(3).joinToString(", ") { it.first.name }} in your remaining meals today."
                    })

                    suggestions
                } else {
                    // No food data — generic
                    listOf(
                        "You need ${formatValue(remaining)} ${target.unit} more ${target.label}. Look for foods naturally rich in ${target.label.lowercase()} or consider a supplement.",
                        when {
                            pct >= 75 -> "Almost at your ${target.label} target (${pct}%). A small dietary adjustment should close the gap."
                            else -> "You're at ${pct}% for ${target.label}. Plan your remaining meals to prioritize ${target.label.lowercase()}-rich foods."
                        }
                    )
                }
            }

            // GENERIC HABIT
            else -> listOf(
                "You have ${formatValue(remaining)} ${target.unit} of ${target.label} left today. Block out time in your schedule now — don't leave it to chance.",
                when {
                    pct >= 75 -> "Almost there with ${target.label}! Just ${formatValue(remaining)} ${target.unit} to go. Push through the last stretch."
                    pct >= 50 -> "You're ${pct}% through your ${target.label} goal. Keep the momentum going with a focused session."
                    else -> "Start your ${target.label} now. Beginning is the hardest part — once you start, it gets easier."
                }
            )
        }
    }

    /**
     * Get top N foods from FoodDatabase sorted by nutrient content for a given target.
     * Returns list of (FoodItem, valuePerBase, amountNeededToFillRemaining).
     */
    private fun getTopFoodsForTarget(
        target: TargetProgress,
        count: Int
    ): List<Triple<FoodItem, Float, Float>> {
        val fieldKey = labelToFieldKey(target.label) ?: return emptyList()

        return FoodDatabase.foods
            .map { food ->
                val value = getFoodFieldValue(food, fieldKey)
                // Calculate how much of this food needed to fill the remaining target
                val amountNeeded = if (value > 0) {
                    if (food.servingUnit == ServingUnit.PIECE) {
                        // For pieces: remaining / valuePerPiece
                        (target.remaining / value).coerceAtLeast(1f)
                    } else {
                        // For g/ml: (remaining / valuePer100) * 100
                        (target.remaining / value) * 100f
                    }
                } else Float.MAX_VALUE
                Triple(food, value, amountNeeded)
            }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(count)
    }

    /**
     * Map target labels to FoodItem field keys.
     */
    private fun labelToFieldKey(label: String): String? {
        return when (label.lowercase().trim()) {
            "calories", "calorie", "cal" -> "calories"
            "protein", "proteins" -> "protein"
            "carbs", "carbohydrates" -> "carbs"
            "fat", "fats", "dietary fat" -> "fat"
            "fiber", "fibre", "dietary fiber" -> "fiber"
            "vitamin a", "vit a" -> "vitA"
            "vitamin b1", "vit b1", "thiamine" -> "vitB1"
            "vitamin b2", "vit b2", "riboflavin" -> "vitB2"
            "vitamin b3", "vit b3", "niacin" -> "vitB3"
            "vitamin b6", "vit b6" -> "vitB6"
            "vitamin b12", "vit b12" -> "vitB12"
            "vitamin c", "vit c" -> "vitC"
            "vitamin d", "vit d" -> "vitD"
            "vitamin e", "vit e" -> "vitE"
            "vitamin k", "vit k" -> "vitK"
            "folate", "folic acid" -> "folate"
            "iron" -> "iron"
            "calcium" -> "calcium"
            "magnesium" -> "magnesium"
            "potassium" -> "potassium"
            "zinc" -> "zinc"
            "copper" -> "copper"
            "selenium" -> "selenium"
            else -> null
        }
    }

    /**
     * Extract nutrient value from a FoodItem by field key.
     */
    private fun getFoodFieldValue(food: FoodItem, fieldKey: String): Float {
        return when (fieldKey) {
            "calories" -> food.caloriesPerBase
            "protein" -> food.proteinPerBase
            "carbs" -> food.carbsPerBase
            "fat" -> food.fatPerBase
            "fiber" -> food.fiberPerBase
            "vitA" -> food.vitAPerBase
            "vitB1" -> food.vitB1PerBase
            "vitB2" -> food.vitB2PerBase
            "vitB3" -> food.vitB3PerBase
            "vitB6" -> food.vitB6PerBase
            "vitB12" -> food.vitB12PerBase
            "vitC" -> food.vitCPerBase
            "vitD" -> food.vitDPerBase
            "vitE" -> food.vitEPerBase
            "vitK" -> food.vitKPerBase
            "folate" -> food.folatePerBase
            "iron" -> food.ironPerBase
            "calcium" -> food.calciumPerBase
            "magnesium" -> food.magnesiumPerBase
            "potassium" -> food.potassiumPerBase
            "zinc" -> food.zincPerBase
            "copper" -> food.copperPerBase
            "selenium" -> food.seleniumPerBase
            else -> 0f
        }
    }

    private fun formatValue(value: Float): String {
        return if (value == value.toLong().toFloat()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
