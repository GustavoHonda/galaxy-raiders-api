@file:Suppress("UNUSED_PARAMETER") // <- REMOVE
package galaxyraiders.core.physics

import kotlin.math.pow


data class Point2D(val x: Double, val y: Double) {
  operator fun plus(p: Point2D): Point2D {
    val pointer: Point2D = Point2D(this.x + p.x, this.y + p.y)
    return pointer
  }

  operator fun plus(v: Vector2D): Point2D {
    val pointer: Point2D = Point2D(this.x + v.dx, this.y + v.dy)
    return pointer
  }

  override fun toString(): String {
    return "Point2D(x=$x, y=$y)"
  }

  val base: Double = kotlin.math.sqrt(this.x.pow(2) + this.y.pow(2))

  fun toVector(): Vector2D {
    val vector: Vector2D = Vector2D(this.x,this.y)
    return vector
  }

  fun impactVector(p: Point2D): Vector2D {
    val vector: Vector2D = Vector2D(p.x-this.x,p.y-this.y)
    return vector
  }

  fun impactDirection(p: Point2D): Vector2D {
    return INVALID_VECTOR
  }

  fun contactVector(p: Point2D): Vector2D {
    val vector: Vector2D = Vector2D(1.0,2.0)
    return vector
    // Vector2D(this.x / base, this.y / base)
  }

  fun contactDirection(p: Point2D): Vector2D {
    val vector: Vector2D = Vector2D(1.0,2.0)
    return vector
  }


  fun distance(p: Point2D): Double {
    val distance: Double = kotlin.math.sqrt((this.x-p.x).pow(2)+(this.y-p.y).pow(2)) 
    return distance
  }
}
