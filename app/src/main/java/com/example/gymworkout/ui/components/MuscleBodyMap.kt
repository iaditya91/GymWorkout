package com.example.gymworkout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymworkout.data.MuscleTarget

// Region defined as fraction of canvas (0..1) for sub-target ellipse overlays
private data class MuscleRegion(
    val cx: Float, val cy: Float, val rx: Float, val ry: Float
)

// ─── Sub-target ellipse regions (for sub-target specificity) ──────

private val frontSubTargetRegions: Map<String, MuscleRegion> = mapOf(
    "scalenes" to MuscleRegion(0.50f, 0.145f, 0.04f, 0.025f),
    "sternocleidomastoid" to MuscleRegion(0.50f, 0.145f, 0.04f, 0.025f),
    "upper traps" to MuscleRegion(0.50f, 0.175f, 0.10f, 0.02f),
    "front delts" to MuscleRegion(0.305f, 0.205f, 0.055f, 0.04f),
    "front delts_r" to MuscleRegion(0.695f, 0.205f, 0.055f, 0.04f),
    "side delts" to MuscleRegion(0.27f, 0.20f, 0.04f, 0.04f),
    "side delts_r" to MuscleRegion(0.73f, 0.20f, 0.04f, 0.04f),
    "rotator cuff" to MuscleRegion(0.31f, 0.21f, 0.04f, 0.035f),
    "rotator cuff_r" to MuscleRegion(0.69f, 0.21f, 0.04f, 0.035f),
    "upper chest" to MuscleRegion(0.42f, 0.22f, 0.075f, 0.025f),
    "upper chest_r" to MuscleRegion(0.58f, 0.22f, 0.075f, 0.025f),
    "mid chest" to MuscleRegion(0.41f, 0.25f, 0.08f, 0.025f),
    "mid chest_r" to MuscleRegion(0.59f, 0.25f, 0.08f, 0.025f),
    "lower chest" to MuscleRegion(0.42f, 0.275f, 0.07f, 0.02f),
    "lower chest_r" to MuscleRegion(0.58f, 0.275f, 0.07f, 0.02f),
    "inner chest" to MuscleRegion(0.46f, 0.25f, 0.035f, 0.035f),
    "inner chest_r" to MuscleRegion(0.54f, 0.25f, 0.035f, 0.035f),
    "outer chest" to MuscleRegion(0.37f, 0.245f, 0.04f, 0.035f),
    "outer chest_r" to MuscleRegion(0.63f, 0.245f, 0.04f, 0.035f),
    "short head" to MuscleRegion(0.26f, 0.29f, 0.03f, 0.045f),
    "short head_r" to MuscleRegion(0.74f, 0.29f, 0.03f, 0.045f),
    "long head_biceps" to MuscleRegion(0.25f, 0.285f, 0.025f, 0.045f),
    "long head_biceps_r" to MuscleRegion(0.75f, 0.285f, 0.025f, 0.045f),
    "brachialis" to MuscleRegion(0.255f, 0.32f, 0.025f, 0.025f),
    "brachialis_r" to MuscleRegion(0.745f, 0.32f, 0.025f, 0.025f),
    "brachioradialis" to MuscleRegion(0.245f, 0.35f, 0.02f, 0.035f),
    "brachioradialis_r" to MuscleRegion(0.755f, 0.35f, 0.02f, 0.035f),
    "wrist flexors" to MuscleRegion(0.24f, 0.38f, 0.022f, 0.04f),
    "wrist flexors_r" to MuscleRegion(0.76f, 0.38f, 0.022f, 0.04f),
    "wrist extensors" to MuscleRegion(0.255f, 0.38f, 0.018f, 0.04f),
    "wrist extensors_r" to MuscleRegion(0.745f, 0.38f, 0.018f, 0.04f),
    "upper abs" to MuscleRegion(0.50f, 0.305f, 0.055f, 0.025f),
    "lower abs" to MuscleRegion(0.50f, 0.365f, 0.05f, 0.025f),
    "obliques" to MuscleRegion(0.39f, 0.34f, 0.03f, 0.04f),
    "obliques_r" to MuscleRegion(0.61f, 0.34f, 0.03f, 0.04f),
    "transverse abdominis" to MuscleRegion(0.50f, 0.335f, 0.06f, 0.04f),
    "rectus femoris" to MuscleRegion(0.42f, 0.52f, 0.035f, 0.07f),
    "rectus femoris_r" to MuscleRegion(0.58f, 0.52f, 0.035f, 0.07f),
    "vastus lateralis" to MuscleRegion(0.395f, 0.52f, 0.03f, 0.065f),
    "vastus lateralis_r" to MuscleRegion(0.605f, 0.52f, 0.03f, 0.065f),
    "vastus medialis" to MuscleRegion(0.445f, 0.55f, 0.025f, 0.04f),
    "vastus medialis_r" to MuscleRegion(0.555f, 0.55f, 0.025f, 0.04f),
    "adductor longus" to MuscleRegion(0.455f, 0.49f, 0.02f, 0.05f),
    "adductor longus_r" to MuscleRegion(0.545f, 0.49f, 0.02f, 0.05f),
    "adductor magnus" to MuscleRegion(0.46f, 0.50f, 0.018f, 0.045f),
    "adductor magnus_r" to MuscleRegion(0.54f, 0.50f, 0.018f, 0.045f),
    "gracilis" to MuscleRegion(0.455f, 0.52f, 0.015f, 0.05f),
    "gracilis_r" to MuscleRegion(0.545f, 0.52f, 0.015f, 0.05f),
    "tensor fasciae latae" to MuscleRegion(0.385f, 0.44f, 0.025f, 0.03f),
    "tensor fasciae latae_r" to MuscleRegion(0.615f, 0.44f, 0.025f, 0.03f),
    "tibialis anterior" to MuscleRegion(0.425f, 0.67f, 0.022f, 0.05f),
    "tibialis anterior_r" to MuscleRegion(0.575f, 0.67f, 0.022f, 0.05f),
)

private val backSubTargetRegions: Map<String, MuscleRegion> = mapOf(
    "upper traps_back" to MuscleRegion(0.50f, 0.175f, 0.10f, 0.02f),
    "mid traps" to MuscleRegion(0.50f, 0.24f, 0.06f, 0.03f),
    "lower traps" to MuscleRegion(0.50f, 0.28f, 0.045f, 0.025f),
    "rear delts" to MuscleRegion(0.31f, 0.205f, 0.05f, 0.035f),
    "rear delts_r" to MuscleRegion(0.69f, 0.205f, 0.05f, 0.035f),
    "rhomboids" to MuscleRegion(0.50f, 0.24f, 0.055f, 0.035f),
    "upper lats" to MuscleRegion(0.38f, 0.26f, 0.04f, 0.04f),
    "upper lats_r" to MuscleRegion(0.62f, 0.26f, 0.04f, 0.04f),
    "lower lats" to MuscleRegion(0.39f, 0.31f, 0.04f, 0.035f),
    "lower lats_r" to MuscleRegion(0.61f, 0.31f, 0.04f, 0.035f),
    "erector spinae" to MuscleRegion(0.50f, 0.35f, 0.045f, 0.04f),
    "lateral head" to MuscleRegion(0.265f, 0.28f, 0.025f, 0.04f),
    "lateral head_r" to MuscleRegion(0.735f, 0.28f, 0.025f, 0.04f),
    "long head_triceps" to MuscleRegion(0.25f, 0.285f, 0.025f, 0.045f),
    "long head_triceps_r" to MuscleRegion(0.75f, 0.285f, 0.025f, 0.045f),
    "medial head" to MuscleRegion(0.255f, 0.31f, 0.02f, 0.025f),
    "medial head_r" to MuscleRegion(0.745f, 0.31f, 0.02f, 0.025f),
    "gluteus maximus" to MuscleRegion(0.43f, 0.415f, 0.05f, 0.04f),
    "gluteus maximus_r" to MuscleRegion(0.57f, 0.415f, 0.05f, 0.04f),
    "gluteus medius" to MuscleRegion(0.40f, 0.39f, 0.04f, 0.03f),
    "gluteus medius_r" to MuscleRegion(0.60f, 0.39f, 0.04f, 0.03f),
    "biceps femoris" to MuscleRegion(0.41f, 0.52f, 0.03f, 0.065f),
    "biceps femoris_r" to MuscleRegion(0.59f, 0.52f, 0.03f, 0.065f),
    "semitendinosus" to MuscleRegion(0.44f, 0.52f, 0.025f, 0.065f),
    "semitendinosus_r" to MuscleRegion(0.56f, 0.52f, 0.025f, 0.065f),
    "semimembranosus" to MuscleRegion(0.45f, 0.51f, 0.02f, 0.06f),
    "semimembranosus_r" to MuscleRegion(0.55f, 0.51f, 0.02f, 0.06f),
    "gastrocnemius" to MuscleRegion(0.42f, 0.66f, 0.028f, 0.045f),
    "gastrocnemius_r" to MuscleRegion(0.58f, 0.66f, 0.028f, 0.045f),
    "soleus" to MuscleRegion(0.425f, 0.71f, 0.022f, 0.035f),
    "soleus_r" to MuscleRegion(0.575f, 0.71f, 0.022f, 0.035f),
)

// ─── Sub-target to ellipse key mapping ────────────────────────────

private fun getFrontRegionKeys(targetGroup: String, subTarget: String): List<String> {
    val sub = subTarget.lowercase()
    return when {
        sub == "scalenes" -> listOf("scalenes")
        sub == "sternocleidomastoid" -> listOf("sternocleidomastoid")
        sub == "upper traps" -> listOf("upper traps")
        sub == "front delts" -> listOf("front delts", "front delts_r")
        sub == "side delts" -> listOf("side delts", "side delts_r")
        sub == "rotator cuff" -> listOf("rotator cuff", "rotator cuff_r")
        sub == "upper chest" -> listOf("upper chest", "upper chest_r")
        sub == "mid chest" -> listOf("mid chest", "mid chest_r")
        sub == "lower chest" -> listOf("lower chest", "lower chest_r")
        sub == "inner chest" -> listOf("inner chest", "inner chest_r")
        sub == "outer chest" -> listOf("outer chest", "outer chest_r")
        sub == "short head" && targetGroup == "biceps" -> listOf("short head", "short head_r")
        sub == "long head" && targetGroup == "biceps" -> listOf("long head_biceps", "long head_biceps_r")
        sub == "brachialis" -> listOf("brachialis", "brachialis_r")
        sub == "brachioradialis" && targetGroup == "biceps" -> listOf("brachioradialis", "brachioradialis_r")
        sub == "wrist flexors" -> listOf("wrist flexors", "wrist flexors_r")
        sub == "wrist extensors" -> listOf("wrist extensors", "wrist extensors_r")
        sub == "brachioradialis" && targetGroup == "forearms" -> listOf("brachioradialis", "brachioradialis_r")
        sub == "upper abs" -> listOf("upper abs")
        sub == "lower abs" -> listOf("lower abs")
        sub == "obliques" -> listOf("obliques", "obliques_r")
        sub == "transverse abdominis" -> listOf("transverse abdominis")
        sub == "rectus femoris" -> listOf("rectus femoris", "rectus femoris_r")
        sub == "vastus lateralis" -> listOf("vastus lateralis", "vastus lateralis_r")
        sub == "vastus medialis" -> listOf("vastus medialis", "vastus medialis_r")
        sub == "adductor longus" -> listOf("adductor longus", "adductor longus_r")
        sub == "adductor magnus" -> listOf("adductor magnus", "adductor magnus_r")
        sub == "gracilis" -> listOf("gracilis", "gracilis_r")
        sub == "tensor fasciae latae" -> listOf("tensor fasciae latae", "tensor fasciae latae_r")
        sub == "gluteus medius" && targetGroup == "abductors" -> listOf("tensor fasciae latae", "tensor fasciae latae_r")
        sub == "tibialis anterior" -> listOf("tibialis anterior", "tibialis anterior_r")
        else -> emptyList()
    }
}

private fun getBackRegionKeys(targetGroup: String, subTarget: String): List<String> {
    val sub = subTarget.lowercase()
    return when {
        sub == "upper traps" -> listOf("upper traps_back")
        sub == "mid traps" -> listOf("mid traps")
        sub == "lower traps" -> listOf("lower traps")
        sub == "rear delts" -> listOf("rear delts", "rear delts_r")
        sub == "rhomboids" -> listOf("rhomboids")
        sub == "upper lats" -> listOf("upper lats", "upper lats_r")
        sub == "lower lats" -> listOf("lower lats", "lower lats_r")
        sub == "erector spinae" -> listOf("erector spinae")
        sub == "lateral head" -> listOf("lateral head", "lateral head_r")
        sub == "long head" && targetGroup == "triceps" -> listOf("long head_triceps", "long head_triceps_r")
        sub == "medial head" -> listOf("medial head", "medial head_r")
        sub == "gluteus maximus" -> listOf("gluteus maximus", "gluteus maximus_r")
        sub == "gluteus medius" && (targetGroup == "glutes" || targetGroup == "abductors") -> listOf("gluteus medius", "gluteus medius_r")
        sub == "gluteus minimus" -> listOf("gluteus medius", "gluteus medius_r")
        sub == "biceps femoris" -> listOf("biceps femoris", "biceps femoris_r")
        sub == "semitendinosus" -> listOf("semitendinosus", "semitendinosus_r")
        sub == "semimembranosus" -> listOf("semimembranosus", "semimembranosus_r")
        sub == "gastrocnemius" -> listOf("gastrocnemius", "gastrocnemius_r")
        sub == "soleus" -> listOf("soleus", "soleus_r")
        else -> emptyList()
    }
}

// Fallback: map entire muscle group to ellipse region keys
private val frontGroupFallback: Map<String, List<String>> = mapOf(
    "chest" to listOf("mid chest", "mid chest_r", "upper chest", "upper chest_r"),
    "shoulders" to listOf("front delts", "front delts_r", "side delts", "side delts_r"),
    "biceps" to listOf("short head", "short head_r", "long head_biceps", "long head_biceps_r"),
    "forearms" to listOf("wrist flexors", "wrist flexors_r", "brachioradialis", "brachioradialis_r"),
    "abdominals" to listOf("upper abs", "lower abs", "obliques", "obliques_r"),
    "quadriceps" to listOf("rectus femoris", "rectus femoris_r", "vastus lateralis", "vastus lateralis_r"),
    "adductors" to listOf("adductor longus", "adductor longus_r", "adductor magnus", "adductor magnus_r"),
    "abductors" to listOf("tensor fasciae latae", "tensor fasciae latae_r"),
    "neck" to listOf("scalenes", "sternocleidomastoid"),
    "traps" to listOf("upper traps"),
    "calves" to listOf("tibialis anterior", "tibialis anterior_r"),
)

private val backGroupFallback: Map<String, List<String>> = mapOf(
    "traps" to listOf("upper traps_back", "mid traps", "lower traps"),
    "shoulders" to listOf("rear delts", "rear delts_r"),
    "lats" to listOf("upper lats", "upper lats_r", "lower lats", "lower lats_r"),
    "middle back" to listOf("rhomboids", "mid traps"),
    "lower back" to listOf("erector spinae"),
    "triceps" to listOf("lateral head", "lateral head_r", "long head_triceps", "long head_triceps_r"),
    "glutes" to listOf("gluteus maximus", "gluteus maximus_r", "gluteus medius", "gluteus medius_r"),
    "hamstrings" to listOf("biceps femoris", "biceps femoris_r", "semitendinosus", "semitendinosus_r"),
    "calves" to listOf("gastrocnemius", "gastrocnemius_r", "soleus", "soleus_r"),
    "neck" to listOf("upper traps_back"),
)

// ─── Collect SVG muscle slugs to highlight ────────────────────────

private fun collectSvgSlugs(
    muscles: List<MuscleTarget>,
    frontSlugs: MutableSet<String>,
    backSlugs: MutableSet<String>
) {
    for (muscle in muscles) {
        val group = muscle.target.lowercase()
        if (muscle.subTargets.isEmpty()) {
            MuscleSvgData.frontGroupToSlug[group]?.let { frontSlugs.addAll(it) }
            MuscleSvgData.backGroupToSlug[group]?.let { backSlugs.addAll(it) }
        } else {
            for (sub in muscle.subTargets) {
                val subLower = sub.lowercase()
                MuscleSvgData.frontSubTargetToSlug[subLower]?.let { frontSlugs.addAll(it) }
                MuscleSvgData.backSubTargetToSlug[subLower]?.let { backSlugs.addAll(it) }
            }
        }
    }
}

// ─── Main composable ─────────────────────────────────────────────

@Composable
fun MuscleBodyMapCard(
    primaryMuscles: List<MuscleTarget>,
    secondaryMuscles: List<MuscleTarget>
) {
    // Collect ellipse region keys (for sub-target specificity)
    val primaryFrontKeys = mutableSetOf<String>()
    val primaryBackKeys = mutableSetOf<String>()
    val secondaryFrontKeys = mutableSetOf<String>()
    val secondaryBackKeys = mutableSetOf<String>()

    fun collectEllipseRegions(
        muscles: List<MuscleTarget>,
        frontSet: MutableSet<String>,
        backSet: MutableSet<String>
    ) {
        for (muscle in muscles) {
            if (muscle.subTargets.isEmpty()) {
                frontGroupFallback[muscle.target.lowercase()]?.let { frontSet.addAll(it) }
                backGroupFallback[muscle.target.lowercase()]?.let { backSet.addAll(it) }
            } else {
                for (sub in muscle.subTargets) {
                    frontSet.addAll(getFrontRegionKeys(muscle.target.lowercase(), sub))
                    backSet.addAll(getBackRegionKeys(muscle.target.lowercase(), sub))
                }
            }
        }
    }

    collectEllipseRegions(primaryMuscles, primaryFrontKeys, primaryBackKeys)
    collectEllipseRegions(secondaryMuscles, secondaryFrontKeys, secondaryBackKeys)

    // Collect SVG muscle slugs (for anatomical shape highlighting)
    val primaryFrontSlugs = mutableSetOf<String>()
    val primaryBackSlugs = mutableSetOf<String>()
    val secondaryFrontSlugs = mutableSetOf<String>()
    val secondaryBackSlugs = mutableSetOf<String>()

    collectSvgSlugs(primaryMuscles, primaryFrontSlugs, primaryBackSlugs)
    collectSvgSlugs(secondaryMuscles, secondaryFrontSlugs, secondaryBackSlugs)

    val hasAnything = primaryFrontKeys.isNotEmpty() || primaryBackKeys.isNotEmpty() ||
            secondaryFrontKeys.isNotEmpty() || secondaryBackKeys.isNotEmpty()
    if (!hasAnything) return

    val primaryColor = Color(0xFFEF5350)
    val secondaryColor = Color(0xFF42A5F5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Muscles Worked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Front view
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Front",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val bodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    Canvas(modifier = Modifier.size(width = 140.dp, height = 280.dp)) {
                        val sx = size.width / MuscleSvgData.FRONT_VB_W
                        val sy = size.height / MuscleSvgData.FRONT_VB_H

                        // Draw body outline
                        drawSvgOutline(sx, sy, bodyColor, outlineColor)

                        // Draw anatomical muscle shapes
                        drawSvgMuscles(primaryFrontSlugs, primaryColor.copy(alpha = 0.5f), MuscleSvgData.frontMusclePaths, sx, sy, 0f)
                        drawSvgMuscles(secondaryFrontSlugs, secondaryColor.copy(alpha = 0.35f), MuscleSvgData.frontMusclePaths, sx, sy, 0f)

                        // Draw sub-target ellipse overlays for specificity
                        drawEllipseRegions(primaryFrontKeys, primaryColor.copy(alpha = 0.3f), frontSubTargetRegions)
                        drawEllipseRegions(secondaryFrontKeys, secondaryColor.copy(alpha = 0.2f), frontSubTargetRegions)
                    }
                }

                // Back view
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Back",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val bodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    Canvas(modifier = Modifier.size(width = 140.dp, height = 280.dp)) {
                        val sx = size.width / MuscleSvgData.BACK_VB_W
                        val sy = size.height / MuscleSvgData.BACK_VB_H

                        // Draw body outline using front outline (same silhouette shape)
                        drawSvgOutline(sx, sy, bodyColor, outlineColor)

                        // Draw anatomical muscle shapes (back paths offset by 724)
                        drawSvgMuscles(primaryBackSlugs, primaryColor.copy(alpha = 0.5f), MuscleSvgData.backMusclePaths, sx, sy, MuscleSvgData.BACK_OFFSET_X)
                        drawSvgMuscles(secondaryBackSlugs, secondaryColor.copy(alpha = 0.35f), MuscleSvgData.backMusclePaths, sx, sy, MuscleSvgData.BACK_OFFSET_X)

                        // Draw sub-target ellipse overlays
                        drawEllipseRegions(primaryBackKeys, primaryColor.copy(alpha = 0.3f), backSubTargetRegions)
                        drawEllipseRegions(secondaryBackKeys, secondaryColor.copy(alpha = 0.2f), backSubTargetRegions)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(primaryColor.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Primary", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(secondaryColor.copy(alpha = 0.4f))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Secondary", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ─── Drawing helpers ──────────────────────────────────────────────

private fun DrawScope.drawSvgOutline(
    scaleX: Float,
    scaleY: Float,
    fillColor: Color,
    outlineColor: Color
) {
    try {
        val path = parseSvgPath(MuscleSvgData.frontOutline, scaleX, scaleY)
        drawPath(path, fillColor, style = Fill)
        drawPath(path, outlineColor, style = Stroke(width = 1.2f))
    } catch (_: Exception) {
        // Fallback: draw nothing if path parsing fails
    }
}

private fun DrawScope.drawSvgMuscles(
    activeSlugs: Set<String>,
    color: Color,
    musclePaths: Map<String, List<String>>,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float
) {
    for (slug in activeSlugs) {
        val paths = musclePaths[slug] ?: continue
        val combined = parseSvgPaths(paths, scaleX, scaleY, offsetX, 0f)
        drawPath(combined, color, style = Fill)
    }
}

private fun DrawScope.drawEllipseRegions(
    activeKeys: Set<String>,
    color: Color,
    regionMap: Map<String, MuscleRegion>
) {
    val w = size.width
    val h = size.height
    for (key in activeKeys) {
        val region = regionMap[key] ?: continue
        val cx = region.cx * w
        val cy = region.cy * h
        val rx = region.rx * w
        val ry = region.ry * h
        drawOval(
            color = color,
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2, ry * 2)
        )
    }
}

