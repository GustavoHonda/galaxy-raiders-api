@file:Suppress("UNUSED_PARAMETER") // <- REMOVE
package galaxyraiders.core.physics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.math.pow
import kotlin.math.atan2
import kotlin.math.atan
import kotlin.math.PI
import kotlin.math.round

@JsonIgnoreProperties("unit", "normal", "degree", "magnitude")
data class Vector2D(val dx: Double, val dy: Double) {
  override fun toString(): String {
    return "Vector2D(dx=$dx, dy=$dy)"
  }

  val base: Double = kotlin.math.sqrt(this.dx.pow(2) + this.dy.pow(2))

  val magnitude: Double
    get() = base

  val radiant: Double
    get() = atan2(this.dy,this.dx)

  val degree: Double
    get() = atan(this.dy/this.dx) * (180/PI)

  val unit: Vector2D
    get() = Vector2D(this.dx / base, this.dy / base)

  val normal: Vector2D 
      get() = Vector2D(this.dy / base, -this.dx / base)

  operator fun times(scalar: Double): Vector2D {
    return Vector2D(this.dx * scalar, this.dy * scalar)
  }

  operator fun div(scalar: Double): Vector2D {
    return Vector2D(this.dx / scalar, this.dy / scalar)
  }

  operator fun times(v: Vector2D): Double {
    return this.dx * v.dx + this.dy * v.dy
  }

  operator fun plus(v: Vector2D): Vector2D {
    return Vector2D(this.dx + v.dx, this.dy + v.dy)
  }

  operator fun plus(p: Point2D): Point2D {
    return Point2D(p.x + this.dx, p.y + this.dy)
  }

  operator fun unaryMinus(): Vector2D {
    return INVALID_VECTOR
  }

  operator fun minus(v: Vector2D): Vector2D {
    if(v.dx.isNaN()){
      return Vector2D(-this.dx, -this.dy )  
    }
    return Vector2D(this.dx - v.dx, this.dy - v.dy)
  }

  fun scalarProject(target: Vector2D): Double {
    return this.times(target)/target.magnitude
  }

  

  fun vectorProject(target: Vector2D): Vector2D {
    val coef: Double = (this.times(target)/target.dx.pow(2) + target.dy.pow(2))
    return Vector2D(round(coef * target.dx), round(coef * target.dy)) 
  }
}

operator fun Double.times(v: Vector2D): Vector2D {
  return Vector2D(this * v.dx, this * v.dy)
}
