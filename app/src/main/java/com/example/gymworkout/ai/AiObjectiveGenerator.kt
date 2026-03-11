package com.example.gymworkout.ai

import com.example.gymworkout.data.NutritionCategory

/**
 * Offline NLP engine that extracts nutrition & habit objectives from free-form text.
 * Runs entirely on-device with zero model downloads — uses keyword extraction,
 * regex patterns, and entity recognition to parse natural language into structured objectives.
 */
object AiObjectiveGenerator {

    data class GeneratedObjective(
        val name: String,
        val unit: String,
        val target: Float,
        val isBuiltIn: Boolean,          // maps to NutritionCategory
        val builtInCategory: NutritionCategory? = null,
        val isNutritionRelated: Boolean = true
    )

    // ── Pattern library ──────────────────────────────────────────────────
    // Each rule: aliases → (canonical name, unit, default target, isNutrition, builtIn?)

    private data class ObjectiveRule(
        val aliases: List<String>,
        val name: String,
        val unit: String,
        val defaultTarget: Float,
        val isNutritionRelated: Boolean = true,
        val builtInCategory: NutritionCategory? = null
    )

    private val rules = listOf(
        // Built-in categories
        ObjectiveRule(
            aliases = listOf("water", "hydration", "drink water", "drinking water", "h2o", "fluid", "fluids", "liquid", "liquids"),
            name = "Water", unit = "L", defaultTarget = 3f,
            builtInCategory = NutritionCategory.WATER
        ),
        ObjectiveRule(
            aliases = listOf("calorie", "calories", "kcal", "energy", "caloric intake", "cal"),
            name = "Calories", unit = "cal", defaultTarget = 2000f,
            builtInCategory = NutritionCategory.CALORIES
        ),
        ObjectiveRule(
            aliases = listOf("protein", "proteins", "whey", "protein intake"),
            name = "Protein", unit = "g", defaultTarget = 120f,
            builtInCategory = NutritionCategory.PROTEIN
        ),
        ObjectiveRule(
            aliases = listOf("carb", "carbs", "carbohydrate", "carbohydrates", "carb intake"),
            name = "Carbs", unit = "g", defaultTarget = 200f,
            builtInCategory = NutritionCategory.CARBS
        ),
        ObjectiveRule(
            aliases = listOf("sleep", "sleeping", "rest", "bedtime", "hours of sleep", "sleep time"),
            name = "Sleep", unit = "hrs", defaultTarget = 8f,
            builtInCategory = NutritionCategory.SLEEP, isNutritionRelated = false
        ),

        // Custom nutrition objectives
        ObjectiveRule(
            aliases = listOf("fat", "fats", "dietary fat", "total fat", "fat intake"),
            name = "Fat", unit = "g", defaultTarget = 65f
        ),
        ObjectiveRule(
            aliases = listOf("fiber", "fibre", "dietary fiber", "roughage"),
            name = "Fiber", unit = "g", defaultTarget = 25f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin a", "vit a", "retinol"),
            name = "Vitamin A", unit = "mcg", defaultTarget = 900f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin b1", "vit b1", "thiamine", "thiamin"),
            name = "Vitamin B1", unit = "mg", defaultTarget = 1.2f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin b2", "vit b2", "riboflavin"),
            name = "Vitamin B2", unit = "mg", defaultTarget = 1.3f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin b3", "vit b3", "niacin"),
            name = "Vitamin B3", unit = "mg", defaultTarget = 16f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin b6", "vit b6", "pyridoxine"),
            name = "Vitamin B6", unit = "mg", defaultTarget = 1.7f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin b12", "vit b12", "cobalamin"),
            name = "Vitamin B12", unit = "mcg", defaultTarget = 2.4f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin c", "vit c", "ascorbic acid"),
            name = "Vitamin C", unit = "mg", defaultTarget = 90f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin d", "vit d", "cholecalciferol", "sunshine vitamin"),
            name = "Vitamin D", unit = "mcg", defaultTarget = 20f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin e", "vit e", "tocopherol"),
            name = "Vitamin E", unit = "mg", defaultTarget = 15f
        ),
        ObjectiveRule(
            aliases = listOf("vitamin k", "vit k", "phylloquinone"),
            name = "Vitamin K", unit = "mcg", defaultTarget = 120f
        ),
        ObjectiveRule(
            aliases = listOf("folate", "folic acid", "vitamin b9", "vit b9"),
            name = "Folate", unit = "mcg", defaultTarget = 400f
        ),
        ObjectiveRule(
            aliases = listOf("iron", "fe"),
            name = "Iron", unit = "mg", defaultTarget = 18f
        ),
        ObjectiveRule(
            aliases = listOf("calcium", "ca"),
            name = "Calcium", unit = "mg", defaultTarget = 1000f
        ),
        ObjectiveRule(
            aliases = listOf("magnesium", "mg mineral", "magnesium intake"),
            name = "Magnesium", unit = "mg", defaultTarget = 400f
        ),
        ObjectiveRule(
            aliases = listOf("potassium", "k mineral"),
            name = "Potassium", unit = "mg", defaultTarget = 2600f
        ),
        ObjectiveRule(
            aliases = listOf("zinc", "zn"),
            name = "Zinc", unit = "mg", defaultTarget = 11f
        ),
        ObjectiveRule(
            aliases = listOf("copper", "cu"),
            name = "Copper", unit = "mg", defaultTarget = 0.9f
        ),
        ObjectiveRule(
            aliases = listOf("selenium", "se"),
            name = "Selenium", unit = "mcg", defaultTarget = 55f
        ),

        // Habit objectives (non-nutrition)
        ObjectiveRule(
            aliases = listOf("creatine", "creatine monohydrate"),
            name = "Creatine", unit = "g", defaultTarget = 5f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("step", "steps", "walking", "walk", "daily steps", "10000 steps", "10k steps"),
            name = "Steps", unit = "steps", defaultTarget = 10000f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("meditation", "meditate", "mindfulness", "mindful"),
            name = "Meditation", unit = "min", defaultTarget = 15f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("reading", "read", "book", "books"),
            name = "Reading", unit = "min", defaultTarget = 30f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("stretching", "stretch", "flexibility", "mobility"),
            name = "Stretching", unit = "min", defaultTarget = 15f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("cardio", "running", "run", "jogging", "jog"),
            name = "Cardio", unit = "min", defaultTarget = 30f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("cold shower", "cold bath", "cold plunge", "cold exposure", "ice bath"),
            name = "Cold Shower", unit = "min", defaultTarget = 3f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("journaling", "journal", "diary", "writing"),
            name = "Journaling", unit = "min", defaultTarget = 10f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("no sugar", "sugar free", "avoid sugar", "cut sugar", "zero sugar"),
            name = "No Sugar", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("screen time", "no phone", "phone free", "digital detox", "no screen"),
            name = "Screen Limit", unit = "hrs", defaultTarget = 2f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("omega 3", "omega-3", "fish oil", "epa", "dha"),
            name = "Omega-3", unit = "mg", defaultTarget = 1000f
        ),
        ObjectiveRule(
            aliases = listOf("caffeine", "coffee"),
            name = "Caffeine", unit = "mg", defaultTarget = 200f
        ),
        ObjectiveRule(
            aliases = listOf("sugar", "sugar intake", "added sugar"),
            name = "Sugar", unit = "g", defaultTarget = 25f
        ),
        ObjectiveRule(
            aliases = listOf("sodium", "salt", "na"),
            name = "Sodium", unit = "mg", defaultTarget = 2300f
        ),
        ObjectiveRule(
            aliases = listOf("workout", "exercise", "gym", "training", "weight training", "lift", "lifting"),
            name = "Workout", unit = "min", defaultTarget = 60f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("yoga", "yoga practice"),
            name = "Yoga", unit = "min", defaultTarget = 30f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("gratitude", "grateful", "thankful"),
            name = "Gratitude", unit = "entries", defaultTarget = 3f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("deep breathing", "breathwork", "breathing exercise", "breath work"),
            name = "Breathwork", unit = "min", defaultTarget = 10f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("no alcohol", "alcohol free", "sober", "sobriety"),
            name = "No Alcohol", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("sunlight", "sun exposure", "morning sun", "sunlight exposure"),
            name = "Sunlight", unit = "min", defaultTarget = 15f, isNutritionRelated = false
        ),

        // ── Rule-based / sentence-style habits ────────────────────────────
        ObjectiveRule(
            aliases = listOf("wake up at same time", "consistent wake", "wake up consistently", "fixed wake time", "same wake time"),
            name = "Wake Up at Same Time Daily", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("drink water immediately after waking", "water after waking", "morning water", "water first thing"),
            name = "Drink Water After Waking", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("avoid phone for first", "no phone first", "phone free morning", "no phone morning", "avoid phone morning", "avoid phone after waking"),
            name = "Avoid Phone for First 30 Min", unit = "min", defaultTarget = 30f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("light exercise", "morning exercise", "morning stretching", "light stretching"),
            name = "Light Exercise / Stretching", unit = "min", defaultTarget = 10f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("quiet breathing", "breathing exercise morning", "calm breathing"),
            name = "Quiet Breathing", unit = "min", defaultTarget = 10f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("review priorities", "top 3 priorities", "plan the day", "daily planning", "review top priorities", "plan priorities"),
            name = "Review Top 3 Priorities", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("start hardest task", "eat the frog", "hardest task first", "most important task first", "mit first"),
            name = "Start Hardest Task First", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("deep focus block", "deep work block", "focus block", "pomodoro", "50 minute block", "deep-focus block"),
            name = "Deep Focus Blocks", unit = "blocks", defaultTarget = 4f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("no multitasking", "single tasking", "one task at a time", "focus on one task", "single-tasking"),
            name = "Focus on One Task at a Time", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("reversible decisions", "decide quickly", "quick decisions", "70% information", "bias for action"),
            name = "Make Quick Decisions (~70% Info)", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("no junk food", "avoid junk", "clean eating", "eat clean"),
            name = "No Junk Food", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("no social media", "social media free", "avoid social media", "social media detox"),
            name = "No Social Media", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("early to bed", "sleep early", "bed by 10", "bed by 11", "early bedtime", "consistent bedtime", "same bedtime"),
            name = "Early / Consistent Bedtime", unit = "days", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("no screen before bed", "no phone before bed", "screen free before bed", "no blue light"),
            name = "No Screens Before Bed", unit = "min", defaultTarget = 30f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("meal prep", "prepare meals", "cook meals", "batch cook"),
            name = "Meal Prep", unit = "meals", defaultTarget = 3f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("posture check", "fix posture", "good posture", "posture correction"),
            name = "Posture Check", unit = "times", defaultTarget = 5f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("eye break", "20-20-20", "eye rest", "look away from screen"),
            name = "Eye Break (20-20-20)", unit = "times", defaultTarget = 8f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("floss", "flossing", "dental floss"),
            name = "Flossing", unit = "times", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("affirmation", "affirmations", "positive affirmation", "self affirmation"),
            name = "Affirmations", unit = "times", defaultTarget = 1f, isNutritionRelated = false
        ),
        ObjectiveRule(
            aliases = listOf("learn something new", "learning", "study", "skill building", "upskill"),
            name = "Learn Something New", unit = "min", defaultTarget = 30f, isNutritionRelated = false
        )
    )

    // Number extraction patterns
    private val numberPattern = Regex("""(\d+\.?\d*)\s*(k|K)?""")
    private val numberBeforeUnit = Regex("""(\d+\.?\d*)\s*(k|K)?\s*(g|mg|mcg|cal|kcal|calories|liters?|litres?|l|ml|hrs?|hours?|min|minutes?|steps?|entries|days?)""", RegexOption.IGNORE_CASE)
    private val numberAfterKeyword = Regex("""(\d+\.?\d*)\s*(k|K)?""")

    /**
     * Parse free-form text and extract structured objectives.
     *
     * Examples of supported input:
     * - "I want to drink 3 liters of water, eat 150g protein, and sleep 8 hours"
     * - "Track my calories at 2000, protein 120g, take creatine 5g, walk 10k steps"
     * - "More water, less sugar, meditate daily, take vitamins"
     * - "High protein diet with 2500 cal, 180g protein, low carb at 100g"
     */
    fun generateObjectives(text: String): List<GeneratedObjective> {
        val input = text.lowercase().trim()
        if (input.isBlank()) return emptyList()

        val results = mutableListOf<GeneratedObjective>()
        val matchedRules = mutableSetOf<String>()

        // Split into segments by commas, periods, newlines, semicolons
        val segments = input.split(Regex("""[,;.\n]+|(?:\band\b)"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // If no clear segments, treat whole text as one segment
        val effectiveSegments = if (segments.isEmpty()) listOf(input) else segments

        for (segment in effectiveSegments) {
            for (rule in rules) {
                if (rule.name in matchedRules) continue

                val matched = rule.aliases.any { alias ->
                    // Check for whole-word match of alias in segment
                    segment.contains(Regex("""\b${Regex.escape(alias)}\b"""))
                }

                if (matched) {
                    val target = extractNumber(segment, rule)
                    matchedRules.add(rule.name)
                    results.add(
                        GeneratedObjective(
                            name = rule.name,
                            unit = rule.unit,
                            target = target,
                            isBuiltIn = rule.builtInCategory != null,
                            builtInCategory = rule.builtInCategory,
                            isNutritionRelated = rule.isNutritionRelated
                        )
                    )
                }
            }
        }

        // Also scan the full text for rules not yet matched (handles cases
        // where objectives span across segment boundaries)
        for (rule in rules) {
            if (rule.name in matchedRules) continue

            val matched = rule.aliases.any { alias ->
                input.contains(Regex("""\b${Regex.escape(alias)}\b"""))
            }

            if (matched) {
                // Find the best segment containing this alias for number extraction
                val bestSegment = effectiveSegments.firstOrNull { seg ->
                    rule.aliases.any { alias ->
                        seg.contains(Regex("""\b${Regex.escape(alias)}\b"""))
                    }
                } ?: input

                val target = extractNumber(bestSegment, rule)
                matchedRules.add(rule.name)
                results.add(
                    GeneratedObjective(
                        name = rule.name,
                        unit = rule.unit,
                        target = target,
                        isBuiltIn = rule.builtInCategory != null,
                        builtInCategory = rule.builtInCategory,
                        isNutritionRelated = rule.isNutritionRelated
                    )
                )
            }
        }

        // ── Fallback: convert unmatched sentence-segments into custom habits ──
        // Track which segments were already matched by a rule
        val matchedSegments = mutableSetOf<Int>()
        for ((index, segment) in effectiveSegments.withIndex()) {
            for (rule in rules) {
                val segMatched = rule.aliases.any { alias ->
                    segment.contains(Regex("""\b${Regex.escape(alias)}\b"""))
                }
                if (segMatched) {
                    matchedSegments.add(index)
                    break
                }
            }
        }

        // For each unmatched segment that looks like a meaningful sentence (>3 words),
        // create a custom habit objective from the full sentence
        for ((index, segment) in effectiveSegments.withIndex()) {
            if (index in matchedSegments) continue

            val cleaned = segment.trim()
            if (cleaned.split("\\s+".toRegex()).size < 3) continue  // skip very short fragments
            if (cleaned.length < 8) continue  // skip trivially short text

            val habitName = sentenceToHabitName(cleaned)
            if (habitName.isBlank() || habitName in matchedRules) continue

            // Extract time/number if mentioned in the sentence
            val numberMatch = numberBeforeUnit.find(cleaned) ?: numberAfterKeyword.find(cleaned)
            val target: Float
            val unit: String
            if (numberMatch != null) {
                val raw = numberMatch.groupValues[1].toFloatOrNull() ?: 1f
                val multiplier = if (numberMatch.groupValues[2].equals("k", ignoreCase = true)) 1000f else 1f
                target = raw * multiplier
                // Infer unit from the sentence
                unit = inferUnitFromSegment(cleaned)
            } else {
                target = 1f
                unit = "days"  // daily check-off habit
            }

            matchedRules.add(habitName)
            results.add(
                GeneratedObjective(
                    name = habitName,
                    unit = unit,
                    target = target,
                    isBuiltIn = false,
                    builtInCategory = null,
                    isNutritionRelated = false
                )
            )
        }

        return results
    }

    // ── Filler words to strip when converting a sentence to a habit name ──
    private val fillerWords = setOf(
        "i", "want", "to", "my", "the", "a", "an", "do", "does", "did", "should",
        "need", "try", "have", "make", "sure", "be", "will", "am", "is", "are",
        "for", "of", "in", "on", "at", "by", "with", "it", "its", "that", "this",
        "also", "just", "so", "very", "really", "always", "every", "each"
    )

    /**
     * Convert a raw sentence into a clean, title-cased habit name.
     * e.g. "wake up at same time daily to maintain consistency"
     *       → "Wake Up at Same Time Daily"
     * Preserves meaningful structure; only strips leading filler.
     */
    private fun sentenceToHabitName(sentence: String): String {
        val words = sentence.trim().split("\\s+".toRegex())

        // Remove "to + reason" suffix (e.g. "to maintain consistency", "to refresh organs")
        // Find a "to" that appears after the first 3 words (to avoid stripping "to" from core phrases)
        var purposeIndex = -1
        for (i in 3 until words.size) {
            if (words[i] == "to") { purposeIndex = i; break }
        }
        val coreWords = if (purposeIndex > 2) words.subList(0, purposeIndex) else words

        // Drop leading filler words (I want to do → drop "I want to do")
        val startIndex = coreWords.indexOfFirst { it.lowercase() !in fillerWords }
        val meaningful = if (startIndex >= 0) coreWords.subList(startIndex, coreWords.size) else coreWords

        if (meaningful.isEmpty()) return ""

        // Title case: capitalize first letter of each word except small words
        val smallWords = setOf("at", "in", "on", "of", "for", "the", "a", "an", "by", "with", "or", "no")
        return meaningful.mapIndexed { i, word ->
            if (i == 0 || word.lowercase() !in smallWords) {
                word.replaceFirstChar { it.uppercase() }
            } else {
                word.lowercase()
            }
        }.joinToString(" ")
    }

    /**
     * Infer the most likely unit from a segment's text.
     */
    private fun inferUnitFromSegment(segment: String): String {
        return when {
            segment.contains(Regex("""\b(min|minutes?)\b""")) -> "min"
            segment.contains(Regex("""\b(hrs?|hours?)\b""")) -> "hrs"
            segment.contains(Regex("""\b(steps?)\b""")) -> "steps"
            segment.contains(Regex("""\b(times?|reps?|sets?)\b""")) -> "times"
            segment.contains(Regex("""\b(blocks?)\b""")) -> "blocks"
            segment.contains(Regex("""\b(g|grams?)\b""")) -> "g"
            segment.contains(Regex("""\b(ml|liters?|litres?)\b""")) -> "L"
            else -> "days"
        }
    }

    /**
     * Extract a numeric value from text near a matched keyword.
     * Falls back to the rule's default target if no number is found.
     */
    private fun extractNumber(segment: String, rule: ObjectiveRule): Float {
        // First try: number with explicit unit
        val unitMatch = numberBeforeUnit.find(segment)
        if (unitMatch != null) {
            val raw = unitMatch.groupValues[1].toFloatOrNull() ?: rule.defaultTarget
            val multiplier = if (unitMatch.groupValues[2].equals("k", ignoreCase = true)) 1000f else 1f
            return raw * multiplier
        }

        // Second try: any number in the segment
        val numMatch = numberAfterKeyword.find(segment)
        if (numMatch != null) {
            val raw = numMatch.groupValues[1].toFloatOrNull() ?: rule.defaultTarget
            val multiplier = if (numMatch.groupValues[2].equals("k", ignoreCase = true)) 1000f else 1f
            return raw * multiplier
        }

        return rule.defaultTarget
    }

    /**
     * Get example prompts to show the user as hints.
     */
    val examplePrompts = listOf(
        "I want to drink 3L water, eat 150g protein, and sleep 8 hours daily",
        "Track 2000 calories, 200g carbs, 120g protein, take 5g creatine",
        "Walk 10k steps, meditate 15 min, read 30 min, no sugar",
        "High protein: 2500 cal, 180g protein, low carb 100g, take omega-3",
        "Wake up at same time daily, drink water after waking, avoid phone for first 30 minutes",
        "Work in 50-minute deep focus blocks, focus on one task at a time, review top 3 priorities"
    )
}
