package com.maltaisn.mazegen.render


/**
 * Path point that draws the arc of an ellipse centered at ([x]; [y]),
 * with radii of [rx] and [ry], from a [start] angle, spanning an [extent] angle.
 * Angles are in radians. If the arc doesn't connect with last point in path
 * a line will be made between the start of the arc and the last point.
 */
class ArcPoint(cx: Double, cy: Double,
               val rx: Double, val ry: Double,
               val start: Double, val extent: Double): Point(cx, cy)
