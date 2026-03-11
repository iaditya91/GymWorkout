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
        val summary: String,
        val actionPlans: List<HabitActionPlan> = emptyList()
    )

    /**
     * A structured, per-target action plan with a timed schedule, practical tips,
     * and fresh/creative ideas to help the user reach their specific goal.
     */
    data class HabitActionPlan(
        val label: String,
        val icon: String,          // emoji for visual identity
        val unit: String,
        val current: Float,
        val target: Float,
        val remaining: Float,
        val schedule: List<ScheduleItem>,
        val tips: List<String>,
        val freshIdeas: List<String>
    ) {
        val percentage get() = if (target > 0) (current / target * 100).coerceIn(0f, 100f) else 0f
    }

    data class ScheduleItem(
        val time: String,      // e.g. "3:00 PM"
        val action: String,    // e.g. "Drink 500ml water"
        val amount: String     // e.g. "500ml"
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
                summary = "All ${progressList.size} targets hit!",
                actionPlans = emptyList()
            )
        }

        val hour = LocalTime.now().hour
        val timeOfDay = when {
            hour < 12 -> "morning"
            hour < 17 -> "afternoon"
            else -> "evening"
        }

        // Generate per-target action plans
        val actionPlans = incomplete.map { generateActionPlanForTarget(it, timeOfDay) }

        // Try Gemini Nano first
        if (isNanoAvailable(context)) {
            try {
                val result = generateWithNano(incomplete, completed, timeOfDay)
                if (result != null) return result.copy(actionPlans = actionPlans)
            } catch (_: Exception) {
                // Fall through to rule-based
            }
        }

        // Fallback: rule-based suggestions
        return generateRuleBased(incomplete, completed, timeOfDay).copy(actionPlans = actionPlans)
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

    // ── Per-Target Action Plan Generator ──────────────────────────────

    private fun generateActionPlanForTarget(
        target: TargetProgress,
        timeOfDay: String
    ): HabitActionPlan {
        val label = target.label.lowercase().trim()
        val remaining = target.remaining
        val hour = LocalTime.now().hour

        val icon = getHabitIcon(label)
        val schedule = generateSchedule(label, target, hour)
        val tips = generateTips(label, target, timeOfDay)
        val freshIdeas = generateFreshIdeas(label, target, timeOfDay)

        return HabitActionPlan(
            label = target.label,
            icon = icon,
            unit = target.unit,
            current = target.current,
            target = target.target,
            remaining = remaining,
            schedule = schedule,
            tips = tips,
            freshIdeas = freshIdeas
        )
    }

    private fun getHabitIcon(label: String): String = when {
        label == "water" -> "\uD83D\uDCA7"
        label == "calories" -> "\uD83D\uDD25"
        label == "protein" -> "\uD83E\uDD69"
        label == "carbs" -> "\uD83C\uDF5A"
        label in listOf("fat", "fats") -> "\uD83E\uDDC8"
        label == "fiber" -> "\uD83E\uDD66"
        label == "sleep" -> "\uD83D\uDE34"
        label == "steps" -> "\uD83D\uDEB6"
        label == "meditation" -> "\uD83E\uDDD8"
        label == "reading" -> "\uD83D\uDCDA"
        label in listOf("workout", "cardio") -> "\uD83C\uDFCB\uFE0F"
        label == "yoga" -> "\uD83E\uDDD8"
        label == "stretching" -> "\uD83E\uDD38"
        label == "cold shower" -> "\uD83E\uDDCA"
        label == "journaling" -> "\u270D\uFE0F"
        label == "creatine" -> "\uD83D\uDCAA"
        label == "breathwork" -> "\uD83C\uDF2C\uFE0F"
        label == "sunlight" -> "\u2600\uFE0F"
        label in listOf("no sugar", "no alcohol") -> "\uD83D\uDEAB"
        label == "screen limit" -> "\uD83D\uDCF1"
        label == "gratitude" -> "\uD83D\uDE4F"
        label.contains("vitamin") || label.contains("vit ") -> "\uD83D\uDC8A"
        label in listOf("iron", "calcium", "magnesium", "potassium", "zinc", "copper", "selenium") -> "\u2697\uFE0F"
        label == "omega-3" || label == "omega 3" -> "\uD83D\uDC1F"
        label == "caffeine" -> "\u2615"
        else -> "\uD83C\uDFAF"
    }

    /**
     * Generate a time-based schedule to spread the remaining amount across the rest of the day.
     */
    private fun generateSchedule(
        label: String,
        target: TargetProgress,
        currentHour: Int
    ): List<ScheduleItem> {
        val remaining = target.remaining
        val timeOfDay = when {
            currentHour < 12 -> "morning"
            currentHour < 17 -> "afternoon"
            else -> "evening"
        }

        return when {
            // ── WATER ──
            label == "water" -> {
                val hoursLeft = (22 - currentHour).coerceAtLeast(1)
                val slots = hoursLeft.coerceIn(2, 6)
                val mlPerSlot = ((remaining * 1000) / slots).toInt()
                val interval = hoursLeft / slots

                (0 until slots).map { i ->
                    val slotHour = (currentHour + 1 + i * interval).coerceAtMost(22)
                    ScheduleItem(
                        time = formatHour(slotHour),
                        action = "Drink ${mlPerSlot}ml water",
                        amount = "${mlPerSlot}ml"
                    )
                }
            }

            // ── CALORIES ──
            label == "calories" -> {
                val cal = remaining.toInt()
                // Realistic max: ~700 cal per meal, ~300 cal snack
                val startHour = (currentHour + 1).coerceAtMost(21)
                when {
                    currentHour < 12 -> listOf(
                        ScheduleItem(formatHour(startHour.coerceAtMost(9)), "Breakfast — ${(cal * 0.30).toInt()} cal", "${(cal * 0.30).toInt()} cal"),
                        ScheduleItem(formatHour(13), "Lunch — ${(cal * 0.35).toInt()} cal", "${(cal * 0.35).toInt()} cal"),
                        ScheduleItem(formatHour(16), "Snack — ${(cal * 0.10).toInt()} cal", "${(cal * 0.10).toInt()} cal"),
                        ScheduleItem(formatHour(19), "Dinner — ${(cal * 0.25).toInt()} cal", "${(cal * 0.25).toInt()} cal")
                    )
                    currentHour < 17 -> {
                        val lunchAmt = cal.coerceAtMost(700)
                        val snackAmt = (cal - lunchAmt).coerceAtMost(300)
                        val dinnerAmt = cal - lunchAmt - snackAmt
                        val items = mutableListOf(
                            ScheduleItem(formatHour(startHour), "Lunch — ${lunchAmt} cal", "${lunchAmt} cal")
                        )
                        if (snackAmt > 0) items.add(ScheduleItem(formatHour(16.coerceAtLeast(currentHour + 2)), "Snack — ${snackAmt} cal", "${snackAmt} cal"))
                        if (dinnerAmt > 0) items.add(ScheduleItem(formatHour(19), "Dinner — ${dinnerAmt} cal", "${dinnerAmt} cal"))
                        items
                    }
                    else -> {
                        // Evening: split into dinner + snack if too much for one sitting
                        if (cal <= 700) {
                            listOf(ScheduleItem(formatHour(startHour), "Dinner — ${cal} cal", "${cal} cal"))
                        } else {
                            val dinnerAmt = cal.coerceAtMost(700)
                            val snackAmt = cal - dinnerAmt
                            listOf(
                                ScheduleItem(formatHour(startHour), "Dinner — ${dinnerAmt} cal", "${dinnerAmt} cal"),
                                ScheduleItem(formatHour((startHour + 2).coerceAtMost(22)), "Late snack — ${snackAmt} cal (nuts, shake, fruit)", "${snackAmt} cal")
                            )
                        }
                    }
                }
            }

            // ── PROTEIN ──
            label == "protein" -> {
                val g = remaining.toInt()
                // Realistic max protein per sitting: ~40g meal, ~20g snack
                val maxPerMeal = 40
                val maxPerSnack = 20
                val startHour = (currentHour + 1).coerceAtMost(21)

                when {
                    // Small amount — one snack is enough
                    g <= maxPerSnack -> listOf(
                        ScheduleItem(
                            time = formatHour(startHour),
                            action = "High-protein snack — ${g}g (e.g. yogurt, eggs, paneer)",
                            amount = "${g}g"
                        )
                    )
                    // Medium — fits in one proper meal
                    g <= maxPerMeal -> {
                        val mealName = when {
                            currentHour < 11 -> "Breakfast"
                            currentHour < 16 -> "Lunch"
                            else -> "Dinner"
                        }
                        listOf(
                            ScheduleItem(
                                time = formatHour(startHour),
                                action = "$mealName — ${g}g protein (chicken, fish, dal + paneer)",
                                amount = "${g}g"
                            )
                        )
                    }
                    // Large — split across meal + snack or multiple meals
                    else -> {
                        val items = mutableListOf<ScheduleItem>()
                        var left = g

                        if (currentHour < 11) {
                            val bfAmt = left.coerceAtMost(30)
                            items.add(ScheduleItem(formatHour(startHour), "Breakfast — ${bfAmt}g protein (eggs, oats + milk)", "${bfAmt}g"))
                            left -= bfAmt
                        }
                        if (left > 0 && currentHour < 15) {
                            val lunchHour = if (currentHour < 11) 13 else startHour
                            val lunchAmt = left.coerceAtMost(maxPerMeal)
                            items.add(ScheduleItem(formatHour(lunchHour), "Lunch — ${lunchAmt}g protein (chicken, dal, paneer)", "${lunchAmt}g"))
                            left -= lunchAmt
                        }
                        if (left > 0 && left <= maxPerSnack) {
                            val snackHour = if (items.isEmpty()) startHour else (16).coerceAtLeast(currentHour + 1)
                            items.add(ScheduleItem(formatHour(snackHour), "Snack — ${left}g protein (shake, nuts, yogurt)", "${left}g"))
                            left = 0
                        }
                        if (left > 0) {
                            val dinnerAmt = left.coerceAtMost(maxPerMeal)
                            items.add(ScheduleItem(formatHour(19), "Dinner — ${dinnerAmt}g protein (chicken, fish, tofu)", "${dinnerAmt}g"))
                            left -= dinnerAmt
                        }
                        if (left > 0) {
                            items.add(ScheduleItem(formatHour(21), "Protein shake — ${left}g (whey + milk)", "${left}g"))
                        }

                        items
                    }
                }
            }

            // ── CARBS ──
            label == "carbs" -> {
                val g = remaining.toInt()
                // Realistic max carbs: ~80g per meal, ~30g per snack
                val maxPerMeal = 80
                val startHour = (currentHour + 1).coerceAtMost(21)

                when {
                    g <= 30 -> listOf(
                        ScheduleItem(formatHour(startHour), "Quick carb snack — ${g}g (banana, toast, fruit)", "${g}g")
                    )
                    g <= maxPerMeal -> {
                        val mealName = when {
                            currentHour < 11 -> "Breakfast"
                            currentHour < 16 -> "Lunch"
                            else -> "Dinner"
                        }
                        listOf(
                            ScheduleItem(formatHour(startHour), "$mealName — ${g}g carbs (rice, roti, oats)", "${g}g")
                        )
                    }
                    else -> {
                        val items = mutableListOf<ScheduleItem>()
                        var left = g

                        if (currentHour < 11) {
                            val amt = left.coerceAtMost(60)
                            items.add(ScheduleItem(formatHour(startHour), "Breakfast — ${amt}g carbs (oats, toast, fruit)", "${amt}g"))
                            left -= amt
                        }
                        if (left > 0 && currentHour < 15) {
                            val lunchHour = if (currentHour < 11) 13 else startHour
                            val amt = left.coerceAtMost(maxPerMeal)
                            items.add(ScheduleItem(formatHour(lunchHour), "Lunch — ${amt}g carbs (rice, roti, pasta)", "${amt}g"))
                            left -= amt
                        }
                        if (left > 0 && left <= 30) {
                            val snackHour = if (items.isEmpty()) startHour else 16.coerceAtLeast(currentHour + 1)
                            items.add(ScheduleItem(formatHour(snackHour), "Snack — ${left}g carbs (banana, crackers)", "${left}g"))
                            left = 0
                        }
                        if (left > 0) {
                            val amt = left.coerceAtMost(maxPerMeal)
                            items.add(ScheduleItem(formatHour(19), "Dinner — ${amt}g carbs (rice, sweet potato)", "${amt}g"))
                            left -= amt
                        }
                        if (left > 0) {
                            items.add(ScheduleItem(formatHour(21), "Light snack — ${left}g carbs (fruit, milk)", "${left}g"))
                        }

                        items
                    }
                }
            }

            // ── SLEEP ──
            label == "sleep" -> {
                val hrs = remaining
                val bedtimeHour = (23 - hrs.toInt()).coerceIn(20, 23)
                listOf(
                    ScheduleItem(formatHour(bedtimeHour - 1), "Start winding down — dim lights, no screens", "prep"),
                    ScheduleItem(formatHour(bedtimeHour), "Get into bed — target ${formatValue(hrs)} hrs of sleep", "${formatValue(hrs)} hrs")
                )
            }

            // ── STEPS ──
            label == "steps" -> {
                val stepsLeft = remaining.toInt()
                val walks = when {
                    stepsLeft > 8000 -> 3
                    stepsLeft > 4000 -> 2
                    else -> 1
                }
                val perWalk = stepsLeft / walks
                val minutes = (perWalk / 100) // ~100 steps per minute
                (0 until walks).map { i ->
                    val walkHour = (currentHour + 1 + i * 3).coerceAtMost(21)
                    ScheduleItem(
                        time = formatHour(walkHour),
                        action = "${minutes}-min walk (~${perWalk} steps)",
                        amount = "$perWalk steps"
                    )
                }
            }

            // ── MEDITATION ──
            label == "meditation" -> {
                val mins = remaining.toInt()
                if (mins > 15) {
                    listOf(
                        ScheduleItem(formatHour((currentHour + 1).coerceAtMost(21)), "Session 1 — ${mins / 2} min focused breathing", "${mins / 2} min"),
                        ScheduleItem(formatHour((currentHour + 4).coerceAtMost(21)), "Session 2 — ${mins - mins / 2} min body scan", "${mins - mins / 2} min")
                    )
                } else {
                    listOf(
                        ScheduleItem(formatHour((currentHour + 1).coerceAtMost(21)), "Meditate for $mins minutes — find a quiet spot", "$mins min")
                    )
                }
            }

            // ── READING ──
            label == "reading" -> {
                val mins = remaining.toInt()
                if (mins > 20) {
                    listOf(
                        ScheduleItem(formatHour((currentHour + 1).coerceAtMost(20)), "Read for ${mins / 2} min — replace phone scrolling", "${mins / 2} min"),
                        ScheduleItem(formatHour(21), "Read for ${mins - mins / 2} min before bed", "${mins - mins / 2} min")
                    )
                } else {
                    listOf(
                        ScheduleItem(formatHour(21), "Read for $mins min before bed", "$mins min")
                    )
                }
            }

            // ── WORKOUT / CARDIO / YOGA / STRETCHING ──
            label in listOf("workout", "cardio", "yoga", "stretching") -> {
                val mins = remaining.toInt()
                val bestHour = when (timeOfDay) {
                    "morning" -> (currentHour + 1).coerceAtMost(11)
                    "afternoon" -> (currentHour + 1).coerceAtMost(17)
                    else -> (currentHour + 1).coerceAtMost(20)
                }
                if (mins > 30) {
                    listOf(
                        ScheduleItem(formatHour(bestHour), "Session 1 — ${mins / 2} min ${target.label.lowercase()}", "${mins / 2} min"),
                        ScheduleItem(formatHour((bestHour + 4).coerceAtMost(21)), "Session 2 — ${mins - mins / 2} min ${target.label.lowercase()}", "${mins - mins / 2} min")
                    )
                } else {
                    listOf(
                        ScheduleItem(formatHour(bestHour), "${target.label} — $mins min session", "$mins min")
                    )
                }
            }

            // ── COLD SHOWER ──
            label == "cold shower" -> {
                val mins = remaining.toInt()
                listOf(
                    ScheduleItem(
                        time = formatHour((currentHour + 1).coerceAtMost(21)),
                        action = "Cold shower — ${mins} min (start with 30s, build up)",
                        amount = "$mins min"
                    )
                )
            }

            // ── JOURNALING ──
            label == "journaling" -> {
                val mins = remaining.toInt()
                listOf(
                    ScheduleItem(
                        time = if (timeOfDay == "morning") formatHour(8) else formatHour(21),
                        action = "Journal for $mins min — write freely",
                        amount = "$mins min"
                    )
                )
            }

            // ── BREATHWORK ──
            label == "breathwork" -> {
                val mins = remaining.toInt()
                if (mins > 10) {
                    listOf(
                        ScheduleItem(formatHour((currentHour + 1).coerceAtMost(20)), "Session 1 — ${mins / 2} min box breathing", "${mins / 2} min"),
                        ScheduleItem(formatHour((currentHour + 5).coerceAtMost(21)), "Session 2 — ${mins - mins / 2} min 4-7-8 technique", "${mins - mins / 2} min")
                    )
                } else {
                    listOf(
                        ScheduleItem(formatHour((currentHour + 1).coerceAtMost(21)), "Breathwork — $mins min focused session", "$mins min")
                    )
                }
            }

            // ── SUNLIGHT ──
            label == "sunlight" -> {
                val mins = remaining.toInt()
                val bestSunHour = when {
                    currentHour < 10 -> 9
                    currentHour < 16 -> currentHour + 1
                    else -> currentHour // too late, acknowledge it
                }
                listOf(
                    ScheduleItem(formatHour(bestSunHour.coerceAtMost(16)), "Go outside for $mins min of sunlight", "$mins min")
                )
            }

            // ── CREATINE ──
            label == "creatine" -> listOf(
                ScheduleItem(
                    time = formatHour((currentHour + 1).coerceAtMost(21)),
                    action = "Take ${formatValue(remaining)}g creatine with water or shake",
                    amount = "${formatValue(remaining)}g"
                )
            )

            // ── SCREEN LIMIT ──
            label == "screen limit" -> {
                val hrs = remaining
                listOf(
                    ScheduleItem(formatHour(currentHour + 1), "Phone-free block — put device in another room", "${formatValue(hrs)} hrs"),
                    ScheduleItem(formatHour(20), "No screens after 8 PM — switch to a book or walk", "rest")
                )
            }

            // ── NO SUGAR / NO ALCOHOL ──
            label in listOf("no sugar", "no alcohol") -> listOf(
                ScheduleItem(formatHour(currentHour + 1), "Next craving window — have a healthy alternative ready", "prep"),
                ScheduleItem(formatHour(20), "Evening check-in — you're almost through the day!", "hold")
            )

            // ── GRATITUDE ──
            label == "gratitude" -> listOf(
                ScheduleItem(
                    time = if (timeOfDay == "morning") formatHour(8) else formatHour(21),
                    action = "Write ${remaining.toInt()} things you're grateful for",
                    amount = "${remaining.toInt()} items"
                )
            )

            // ── NUTRITION (vitamins, minerals, fat, fiber etc.) ──
            target.isNutrition -> {
                val topFoods = getTopFoodsForTarget(target, 3)
                val mealsLeft = when {
                    currentHour < 10 -> 3
                    currentHour < 15 -> 2
                    else -> 1
                }
                if (topFoods.isNotEmpty()) {
                    val perMeal = remaining / mealsLeft.coerceAtLeast(1)
                    (0 until mealsLeft.coerceAtMost(3)).mapIndexed { i, _ ->
                        val food = topFoods[i.coerceAtMost(topFoods.size - 1)]
                        val mealHour = when (i) {
                            0 -> (currentHour + 1).coerceAtMost(21)
                            1 -> 15.coerceAtLeast(currentHour + 2)
                            else -> 19
                        }
                        val serving = if (food.first.servingUnit == ServingUnit.PIECE) "pc" else "g"
                        val amt = (food.third / mealsLeft).toInt().coerceAtLeast(1)
                        ScheduleItem(
                            time = formatHour(mealHour),
                            action = "Eat ~${amt}${serving} ${food.first.name} (${formatValue(perMeal)} ${target.unit} ${target.label})",
                            amount = "${amt}${serving}"
                        )
                    }
                } else {
                    listOf(
                        ScheduleItem(
                            time = formatHour((currentHour + 1).coerceAtMost(21)),
                            action = "Include ${target.label.lowercase()}-rich foods in your next meal",
                            amount = "${formatValue(remaining)} ${target.unit}"
                        )
                    )
                }
            }

            // ── GENERIC HABIT ──
            else -> {
                val hoursLeft = (22 - currentHour).coerceAtLeast(1)
                listOf(
                    ScheduleItem(
                        time = formatHour((currentHour + 1).coerceAtMost(21)),
                        action = "${target.label} — ${formatValue(remaining)} ${target.unit} remaining",
                        amount = "${formatValue(remaining)} ${target.unit}"
                    )
                )
            }
        }
    }

    /**
     * Generate practical, actionable tips for the target.
     */
    private fun generateTips(label: String, target: TargetProgress, timeOfDay: String): List<String> {
        val lbl = label.lowercase().trim()
        return when {
            lbl == "water" -> listOf(
                "Keep a filled water bottle in front of you at all times as a visual reminder",
                "Set phone alarms at each scheduled time so you never forget",
                "Drink a full glass immediately after waking up — your body is dehydrated from sleep",
                "Pair water with existing habits: drink before every meal, after every bathroom break"
            )
            lbl == "calories" -> listOf(
                "Prep calorie-dense snacks (nuts, trail mix, peanut butter) to grab easily",
                "Eat on a schedule even if you're not hungry — don't rely on appetite alone",
                "Use bigger plates to naturally serve larger portions without thinking about it",
                "Add healthy fats (olive oil, ghee, butter) to meals for easy calorie boosts"
            )
            lbl == "protein" -> listOf(
                "Front-load protein at breakfast — eggs, paneer, or a shake sets a strong foundation",
                "Keep ready-to-eat protein snacks handy: boiled eggs, Greek yogurt, cheese cubes",
                "Add protein to every meal — even a small serving adds up over the day",
                "A protein shake before bed fills gaps and supports overnight muscle recovery"
            )
            lbl == "carbs" -> listOf(
                "Choose complex carbs (oats, brown rice, sweet potato) for sustained energy",
                "Add a banana or fruit to your snack — easy, natural carb boost",
                "Cook rice or pasta in bulk for the week so it's always ready to eat",
                "Pair carbs with protein for better absorption and satiety"
            )
            lbl == "sleep" -> listOf(
                "Set a \"wind-down alarm\" 30 min before your target bedtime",
                "Keep your bedroom cool (18-20°C) and completely dark for optimal sleep",
                "No caffeine after 2 PM — it stays in your system for 6+ hours",
                "Put your phone in another room to avoid doomscrolling in bed"
            )
            lbl == "steps" -> listOf(
                "Take phone calls while walking — multitask your way to more steps",
                "Park farther from entrances and take stairs instead of elevators",
                "A 10-min walk after each meal aids digestion AND adds ~1000 steps",
                "Walk during work breaks — even 5 min every hour adds up significantly"
            )
            lbl == "meditation" -> listOf(
                "Start with just 5 min if the full session feels intimidating — build up gradually",
                "Same time, same spot every day builds an automatic habit loop",
                "Focus on breath counting (1 to 10, restart) — it's simple and effective",
                "If your mind wanders, that's normal — noticing it IS the practice"
            )
            lbl == "reading" -> listOf(
                "Replace your first 15 min of phone scrolling with reading",
                "Keep your book/kindle on your pillow so it's the first thing you see at bedtime",
                "Read just one page if you're not in the mood — starting is the hardest part",
                "Audiobooks count! Listen during commute, cooking, or walks"
            )
            lbl in listOf("workout", "cardio") -> listOf(
                "Lay out your workout clothes the night before to reduce friction",
                "A 5-min warm-up makes the full workout feel much easier to start",
                "If motivation is low, commit to just 10 min — you'll usually keep going",
                "Track your workouts to see progress — visible improvement fuels motivation"
            )
            lbl == "yoga" -> listOf(
                "Morning yoga on an empty stomach gives the best flexibility and focus",
                "Follow a short YouTube routine if you don't know what to do",
                "Focus on breathing, not perfect poses — flexibility comes with time",
                "Even 10 min of sun salutations covers a full-body stretch"
            )
            lbl == "stretching" -> listOf(
                "Stretch right after waking up — your body responds well to gentle morning movement",
                "Hold each stretch for 30 seconds minimum for real flexibility gains",
                "Stretch during TV time or while waiting for food to cook",
                "Focus on areas that feel tight — hip flexors, hamstrings, shoulders are common"
            )
            lbl == "cold shower" -> listOf(
                "End your normal warm shower with cold for the last 30-60 seconds to start",
                "Take deep, slow breaths through the cold — it calms the shock response",
                "Focus on the amazing feeling AFTER the cold shower — it's worth it",
                "Gradually increase duration each week — your body adapts fast"
            )
            lbl == "journaling" -> listOf(
                "Use a simple template: 3 gratitudes, 1 win, 1 thing to improve",
                "Don't edit while writing — stream of consciousness is more effective",
                "Keep your journal by your bed for easy morning or evening sessions",
                "Even bullet points count — it doesn't need to be prose"
            )
            lbl == "breathwork" -> listOf(
                "Box breathing (4-4-4-4) is the simplest and most effective technique to start",
                "Practice right after waking for an energizing start to your day",
                "Set a gentle timer so you can fully focus without watching the clock",
                "Breathwork before bed (4-7-8 technique) dramatically improves sleep quality"
            )
            lbl == "sunlight" -> listOf(
                "First 30 min after sunrise is the most beneficial for circadian rhythm",
                "No sunglasses during your sunlight session — your eyes need the light signal",
                "Combine with a walk for double benefit: sunlight + steps",
                "Even cloudy days provide enough light — just stay outside longer"
            )
            lbl in listOf("no sugar", "no alcohol") -> listOf(
                "When a craving hits, wait 20 min — most cravings pass on their own",
                "Keep healthy alternatives ready: sparkling water, herbal tea, dark chocolate (85%+)",
                "Identify your trigger times and have a distraction plan ready",
                "Remind yourself how good you'll feel tomorrow having stayed on track"
            )
            lbl == "screen limit" -> listOf(
                "Turn on grayscale mode on your phone — screens become much less addictive",
                "Use app timers to auto-lock social media after set durations",
                "Create phone-free zones: bedroom, dining table",
                "Replace scrolling with a physical activity: walk, stretch, or read"
            )
            lbl == "gratitude" -> listOf(
                "Be specific: \"I'm grateful for the warm coffee this morning\" beats \"I'm grateful for food\"",
                "Include people, moments, and small wins — variety keeps it meaningful",
                "Write it down — thinking it isn't as effective as putting pen to paper",
                "Do it at the same time daily to make it automatic"
            )
            else -> listOf(
                "Set a specific time for ${target.label} to build a consistent routine",
                "Track your streak — visual progress is a powerful motivator",
                "Start small and increase gradually rather than going all-in from day one",
                "Pair ${target.label.lowercase()} with an existing habit for automatic reminders"
            )
        }
    }

    /**
     * Generate fresh, creative, and sometimes unconventional ideas.
     */
    private fun generateFreshIdeas(label: String, target: TargetProgress, timeOfDay: String): List<String> {
        val lbl = label.lowercase().trim()
        return when {
            lbl == "water" -> listOf(
                "\uD83C\uDF4B Infuse your water with fruit slices (lemon, cucumber, mint) — it tastes like a spa drink",
                "\uD83C\uDFAE Make it a game: finish a glass every time you complete a task, reply to an email, or stand up",
                "\uD83E\uDD64 Try sparkling water or herbal tea if plain water feels boring",
                "\uD83C\uDFC6 Challenge a friend: who hits their water target first today? Loser buys coffee tomorrow",
                "\uD83C\uDF21\uFE0F Match water temp to mood: cold for energy, warm with lemon for calm"
            )
            lbl == "calories" -> listOf(
                "\uD83E\uDD5C Calorie-bomb smoothie: banana + peanut butter + oats + milk + honey = 500+ cal in one glass",
                "\uD83C\uDF73 Cook with ghee or olive oil — a tablespoon adds ~120 cal invisibly to any dish",
                "\uD83C\uDF6B Healthy dessert: dates stuffed with almond butter = delicious + calorie-dense",
                "\uD83E\uDD57 Top salads with nuts, seeds, and avocado — turns a 200 cal salad into 500+",
                "\uD83C\uDF5E Make a loaded toast: bread + peanut butter + banana + honey + chia seeds"
            )
            lbl == "protein" -> listOf(
                "\uD83E\uDD5B Protein power smoothie: whey + banana + peanut butter + milk = 40g protein",
                "\uD83E\uDDC0 Snack hack: cottage cheese or paneer cubes with spices — tasty and protein-packed",
                "\uD83C\uDF73 Add egg whites to omelettes for extra protein without extra fat",
                "\uD83E\uDD69 Make a protein trail mix: roasted chana + almonds + pumpkin seeds",
                "\uD83C\uDF75 Stir protein powder into overnight oats for a breakfast that works while you sleep"
            )
            lbl == "carbs" -> listOf(
                "\uD83C\uDF4C Frozen banana + milk blended = healthy ice cream that's carb-rich and delicious",
                "\uD83C\uDF5A Cook extra rice and make fried rice with veggies for an easy carb-loaded meal",
                "\uD83C\uDF6F Drizzle honey on yogurt, toast, or oatmeal for a natural carb boost",
                "\uD83E\uDD5E Pancakes or dosa for dinner? Why not! Carbs don't have a curfew",
                "\uD83C\uDF60 Sweet potato fries baked with a pinch of cinnamon — delicious carb snack"
            )
            lbl == "sleep" -> listOf(
                "\uD83C\uDFB5 Play brown noise or rain sounds — they mask disruptions and deepen sleep",
                "\uD83D\uDCD6 Read fiction before bed — it transports your mind away from tomorrow's worries",
                "\uD83E\uDDCA Try the \"cool socks\" trick: cold feet under warm blankets signals your body to sleep",
                "\u2615 Replace evening tea/coffee with chamomile or warm turmeric milk",
                "\uD83D\uDCDD Write tomorrow's to-do list before bed — clears your mind of planning anxiety"
            )
            lbl == "steps" -> listOf(
                "\uD83C\uDFB6 Create a walking playlist — upbeat music makes walks feel shorter and more fun",
                "\uD83D\uDCDE Take all phone calls standing/walking — you'll be surprised how much it adds",
                "\uD83D\uDED2 Walk to the farthest bathroom, water cooler, or store instead of the nearest one",
                "\uD83D\uDC36 Walk a neighbor's dog — social, fun, and healthy for both of you",
                "\uD83C\uDFAE Pokémon GO or walking-based games turn boring walks into adventures"
            )
            lbl == "meditation" -> listOf(
                "\uD83C\uDF3F Try walking meditation: slow, deliberate steps with full attention on each footfall",
                "\uD83D\uDEC1 Shower meditation: focus entirely on the sensation of water and let thoughts drift",
                "\uD83C\uDF4B Mindful eating: eat one meal in complete silence, savoring every bite",
                "\uD83C\uDF05 Watch a sunrise or sunset without your phone — nature is the original meditation",
                "\uD83E\uDDD8 Try humming meditation (Bhramari): humming creates vibrations that calm the nervous system"
            )
            lbl == "reading" -> listOf(
                "\uD83D\uDCDA Start a mini book club with one friend — accountability makes reading stick",
                "\uD83C\uDFA7 Switch between reading and audiobooks to match your energy levels",
                "\uD83D\uDCF1 Delete one social media app and replace its homescreen spot with a reading app",
                "\u2615 Pair reading with coffee or tea — create a cozy ritual you look forward to",
                "\uD83C\uDFB2 Pick a random book genre you've never tried — variety keeps reading exciting"
            )
            lbl in listOf("workout", "cardio") -> listOf(
                "\uD83C\uDFB5 Create a fire workout playlist — music at 120-140 BPM naturally boosts intensity",
                "\uD83D\uDCF8 Film a set and check your form — it's like having a free coach",
                "\uD83E\uDD1D Find a workout buddy — accountability is the #1 predictor of consistency",
                "\uD83C\uDFAE Try gamified fitness: beat yesterday's rep count, time, or distance",
                "\uD83C\uDF1F Try a completely new exercise — novelty prevents boredom and hits new muscles"
            )
            lbl == "yoga" -> listOf(
                "\uD83C\uDF1E Start with 5 sun salutations — it's a complete body flow in just minutes",
                "\uD83C\uDFB6 Try yoga with lo-fi music — creates a meditative flow state",
                "\uD83D\uDDFB Partner yoga: stretching with someone doubles the fun and depth",
                "\uD83C\uDF3F Take it outside: yoga in a park or balcony adds a nature boost",
                "\uD83D\uDE34 End with legs-up-the-wall pose for 5 min — ultimate relaxation hack"
            )
            lbl == "stretching" -> listOf(
                "\uD83D\uDCFA Stretch while watching TV — you won't even notice the time passing",
                "\uD83D\uDEBF Post-shower stretching: warm muscles are more flexible and respond better",
                "\u23F0 Set hourly reminders for 2-min micro-stretches throughout the day",
                "\uD83D\uDC6B Stretch with a partner — they can gently push your range of motion",
                "\uD83D\uDCF1 Follow a 10-min YouTube follow-along — no thinking required, just move"
            )
            lbl == "cold shower" -> listOf(
                "\uD83C\uDFB5 Play your most hype song during the cold shower — it distracts from the cold",
                "\uD83E\uDDCA Try ending with cold instead of starting cold — it's mentally easier",
                "\uD83C\uDFC6 Track your cold exposure streak — the visual chain motivates you not to break it",
                "\uD83E\uDDD8 Practice Wim Hof breathing before stepping in — pre-oxygenation reduces shock",
                "\uD83C\uDF1F Focus on counting breaths, not seconds — it feels much shorter"
            )
            lbl == "journaling" -> listOf(
                "\u2753 Use a prompt jar: write 30 different prompts, pick one randomly each day",
                "\uD83D\uDCF8 Photo journaling: take one meaningful photo daily and write a sentence about it",
                "\uD83D\uDE00 Start with emojis: rate your day with 3 emojis, then explain why you chose them",
                "\u2709\uFE0F Write a letter to your future self — re-read it in 6 months",
                "\uD83C\uDFA8 Try bullet journaling: minimal effort, maximum clarity"
            )
            lbl == "breathwork" -> listOf(
                "\uD83C\uDF0A Try ocean breathing (Ujjayi): breathe through the nose with a slight throat constriction",
                "\uD83E\uDDE0 Use breathwork before a stressful meeting — 2 min of box breathing resets your nervous system",
                "\uD83D\uDCAA Try power breathing (rapid inhale-exhale cycles) for an instant energy boost",
                "\uD83C\uDF19 Alternate nostril breathing before bed — it's like a lullaby for your nervous system",
                "\uD83C\uDFB5 Sync breathing to music: inhale for 4 beats, exhale for 4 beats"
            )
            lbl == "sunlight" -> listOf(
                "\u2615 Take your morning coffee outside — sunlight + caffeine is the ultimate wake-up combo",
                "\uD83E\uDDD8 Do your stretching or yoga outside for double benefits",
                "\uD83D\uDCDE Take phone calls on a balcony or near a window with direct sunlight",
                "\uD83C\uDF3F Start a small balcony garden — daily watering = daily sunlight habit",
                "\uD83D\uDEB6 Walk to get lunch instead of ordering delivery — sunlight + steps + food"
            )
            lbl in listOf("no sugar", "no alcohol") -> listOf(
                "\uD83C\uDF53 Freeze grapes or berries — they taste like candy and satisfy sweet cravings",
                "\uD83E\uDD64 Try kombucha or flavored sparkling water as a social drink replacement",
                "\uD83E\uDDD0 When craving hits, ask: \"Am I hungry, bored, or stressed?\" — address the real cause",
                "\uD83C\uDFC6 Calculate money saved by skipping ${lbl.removePrefix("no ")} this week — reward yourself with it",
                "\uD83D\uDCAA Every time you resist a craving, you're literally rewiring your brain — celebrate that"
            )
            lbl == "screen limit" -> listOf(
                "\uD83D\uDCF5 Use the \"phone parking\" trick: park your phone in a specific spot when you get home",
                "\u23F0 Set up Focus/DND mode with automatic schedules for phone-free hours",
                "\uD83C\uDFA8 Replace screen time with a creative hobby: drawing, cooking, music, puzzles",
                "\uD83D\uDC65 Plan one screen-free social activity today: board game, walk, or just talking",
                "\uD83D\uDCDA Swap 30 min of scrolling for 30 min of reading or podcasts"
            )
            lbl == "gratitude" -> listOf(
                "\uD83D\uDCF8 Take a \"gratitude photo\" each day — build a visual gratitude album",
                "\uD83D\uDCEC Send a thank-you text to someone you appreciate — gratitude shared is doubled",
                "\uD83C\uDF1F Gratitude walk: notice 5 beautiful things during a short walk",
                "\uD83D\uDE00 Before meals, pause and appreciate the food, the effort behind it, and who you share it with",
                "\uD83D\uDCDD End of day: \"What made me smile today?\" — it reframes your entire day"
            )
            else -> listOf(
                "\uD83D\uDD17 Stack ${target.label} with a habit you already love — it'll feel effortless",
                "\uD83C\uDFC6 Start a streak tracker — seeing the chain grow becomes its own motivation",
                "\uD83D\uDCF1 Set a creative phone reminder with an encouraging message to yourself",
                "\uD83D\uDC65 Find an accountability partner for ${target.label.lowercase()} — share your daily progress",
                "\uD83C\uDFAF Reward yourself after hitting the target 7 days in a row — you've earned it"
            )
        }
    }

    /**
     * Format an hour integer (0-23) into a readable time string.
     */
    private fun formatHour(hour: Int): String {
        val h = hour.coerceIn(0, 23)
        return when {
            h == 0 -> "12:00 AM"
            h < 12 -> "$h:00 AM"
            h == 12 -> "12:00 PM"
            else -> "${h - 12}:00 PM"
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
