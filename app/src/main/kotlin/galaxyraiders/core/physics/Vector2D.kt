@file:Suppress("UNUSED_PARAMETER") // <- REMOVE
package galaxyraiders.core.physics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlin.math.pow
import kotlin.math.atan2
import kotlin.math.atan
import kotlin.math.PI
import kotlin.math.abs

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

  val x_sign: Boolean = this.dx > 0

  val y_sign: Boolean = this.dy > 0

  fun Boolean.toInt() = if (this) 1 else 0

  val degree_in_rad: Double = atan(abs(this.dy)/abs(this.dx)) 

  val ajust_sign: Double = (-1.0).pow((!y_sign).toInt()) 

  fun ajust_side(degree: Double): Double{
      if(!x_sign){
        return 180 - degree;
      }
      else{
        return degree;
      }
  }

  val degree: Double
    get() =  ajust_sign * ajust_side(degree_in_rad * (180/PI))

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
    return Vector2D(-this.dx,-this.dy )
  }

  operator fun minus(v: Vector2D): Vector2D {
    //maybe transverse implementation?
    if(v.dx.isNaN()){
      return Vector2D(-this.dx, -this.dy )  
    }
    return Vector2D(this.dx - v.dx, this.dy - v.dy)
  }

  fun scalarProject(target: Vector2D): Double {
    val scalar: Double = this.times(target/target.magnitude)
    return scalar
  }

  fun Double.round() = String.format("%.3f", this).toDouble()

  fun vectorProject(target: Vector2D): Vector2D {
    val lenth: Double = (target.dx.pow(2) + target.dy.pow(2))
    val coef: Double = (this.times(target)/lenth)
    return Vector2D(
                    (coef * target.dx).round(),
                    (coef * target.dy).round())
  }
}

operator fun Double.times(v: Vector2D): Vector2D {
  return Vector2D(this * v.dx, this * v.dy)
}
