package com.example.gymworkout.ui.components

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser

/**
 * Parses an SVG path data string (the "d" attribute) into a Compose [Path],
 * scaled from original SVG coordinates to the target canvas size.
 */
fun parseSvgPath(
    pathData: String,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path {
    val androidPath = PathParser.createPathFromPathData(pathData)
    val matrix = android.graphics.Matrix()
    matrix.postTranslate(-offsetX, -offsetY)
    matrix.postScale(scaleX, scaleY)
    androidPath.transform(matrix)
    return androidPath.asComposePath()
}

/**
 * Parses multiple SVG path strings and combines them into a single Compose [Path].
 */
fun parseSvgPaths(
    pathDataList: List<String>,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path {
    val combined = Path()
    for (pd in pathDataList) {
        try {
            combined.addPath(parseSvgPath(pd, scaleX, scaleY, offsetX, offsetY))
        } catch (_: Exception) {
            // Skip malformed paths
        }
    }
    return combined
}
