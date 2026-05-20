package org.liftrr.domain.workout

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

object AngleCalculator {
    fun calculateAngle(
        a: NormalizedLandmark?,
        b: NormalizedLandmark?,
        c: NormalizedLandmark?
    ): Float? {
        if (a == null || b == null || c == null) return null

        val radians = atan2(c.y() - b.y(), c.x() - b.x()) -
                atan2(a.y() - b.y(), a.x() - b.x())

        var angle = abs(radians * 180.0 / PI).toFloat()
        if (angle > 180f) angle = 360f - angle

        return angle
    }
}