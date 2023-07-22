package galaxyraiders.core.game

import galaxyraiders.Config
import galaxyraiders.ports.RandomGenerator
import galaxyraiders.ports.ui.Controller
import galaxyraiders.ports.ui.Controller.PlayerCommand
import galaxyraiders.ports.ui.Visualizer
import kotlin.system.measureTimeMillis
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime
import galaxyraiders.core.game.SpaceObject
import java.nio.file.Paths
import java.nio.file.Files

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

val mapper = ObjectMapper()

const val MILLISECONDS_PER_SECOND: Int = 1000


data class Classification(
  var player_id : Int,
  var score : Int,
  var time : String,
  var hit_asteroids : Int
)

data class Leaderboard(
  var next_player : Int, 
  var first : Classification,
  var second : Classification,
  var third : Classification
)

object GameEngineConfig {
  private val config = Config(prefix = "GR__CORE__GAME__GAME_ENGINE__")

  val frameRate = config.get<Int>("FRAME_RATE")
  val spaceFieldWidth = config.get<Int>("SPACEFIELD_WIDTH")
  val spaceFieldHeight = config.get<Int>("SPACEFIELD_HEIGHT")
  val asteroidProbability = config.get<Double>("ASTEROID_PROBABILITY")
  val coefficientRestitution = config.get<Double>("COEFFICIENT_RESTITUTION")

  val msPerFrame: Int = MILLISECONDS_PER_SECOND / this.frameRate
}

@Suppress("TooManyFunctions")
class GameEngine(
  val generator: RandomGenerator,
  val controller: Controller,
  val visualizer: Visualizer,
) {
  val field = SpaceField(
    width = GameEngineConfig.spaceFieldWidth,
    height = GameEngineConfig.spaceFieldHeight,
    generator = generator
  )

  var playing = true

  var score = 0
  var hit_asteroids = 0
  var player = Classification(0,0,"",0)
  var board : Leaderboard = Leaderboard(0,player,player,player)
  var time  = ""

  //Read from json file the 3 first high score players and the nº of the next player
  fun read_leaderboard(){
    
    //val path  = "src/main/kotlin/galaxyraiders/core/score/Leaderboard.json"
    // .readText(Charsets.UTF_8)
    //val jsonFile = File(path)
    //val nboard : Leaderboard = mapper.readValue(jsonFile, Leaderboard::class.java) // deserialize data to String
    
  }

  // get start time of game
  fun get_time(){
    time = LocalDateTime.now().toString()
  }
  
  fun update_leaderboard(){
    var new_player : Classification = Classification(board.next_player,score,time,hit_asteroids)
    if(score > board.third.score){
      board.third = new_player
      if(score > board.second.score){
        board.third = board.second
        board.second = new_player
        if(score > board.first.score){
          board.second = board.first
          board.first = new_player
        }
      }  
    }
    board.next_player = board.next_player + 1
  }


  //Write new Leaderboard.json
  fun write_leaderboard(){
    try{
      val path : String = "../score/Leaderboard.json"
      File(path).writeText("")   // empty Leaderboard.json
      update_leaderboard()
      val data = mapper.writeValueAsString(board)  //serialize data to json format
      FileWriter(path, true).use {
        it.write(data)
      }
    }
    catch(e : Exception){
      println("Exception Leaderboard.json file missing")
    }
  }

  //Write new score in Scoreboard.json
  fun write_scoreboard(){
    try{
      val path : String = "../score/Scoreboard.json"
      var new_player : Classification = Classification(board.next_player,score,time,hit_asteroids)
      val data = mapper.writeValueAsString(new_player)
      FileWriter(path, true).use {
        it.write(data)
      }
    }
    catch(e : Exception){
      println("Exception Scoreboard.json file missing")
    }
  }

  fun execute() {
    while (true) {
      val duration = measureTimeMillis { this.tick() }
      Thread.sleep(
        maxOf(0, GameEngineConfig.msPerFrame - duration)
      )
    }
  }

  fun execute(maxIterations: Int) {
    repeat(maxIterations) {
      this.tick()
    }
  }

  fun tick() {
    this.processPlayerInput()
    this.updateSpaceObjects()
    this.renderSpaceField()
  }

  fun processPlayerInput() {
    this.controller.nextPlayerCommand()?.also {
      when (it) {
        PlayerCommand.MOVE_SHIP_UP ->
          this.field.ship.boostUp()
        PlayerCommand.MOVE_SHIP_DOWN ->
          this.field.ship.boostDown()
        PlayerCommand.MOVE_SHIP_LEFT ->
          this.field.ship.boostLeft()
        PlayerCommand.MOVE_SHIP_RIGHT ->
          this.field.ship.boostRight()
        PlayerCommand.LAUNCH_MISSILE ->
          this.field.generateMissile()
        PlayerCommand.PAUSE_GAME ->
          this.playing = !this.playing
      }
    }
  }

  fun updateSpaceObjects() {
    if (!this.playing) return
    this.handleCollisions()
    this.moveSpaceObjects()
    this.trimSpaceObjects()
    this.generateAsteroids()
  }

  fun increase_score(asteroid : SpaceObject){
    var points : Int = (asteroid.radius + asteroid.mass).toInt()
    score = score + points
    hit_asteroids = hit_asteroids + 1
  }

  fun update_score(first : SpaceObject, second : SpaceObject){
    if(first.type == "Missile" && second.type == "Asteroid"){
      increase_score(second)
    }
    else if(first.type == "Asteroid" && second.type == "Missile"){
      increase_score(first)
    }
  }

  fun handleCollisions() {
    this.field.spaceObjects.forEachPair {
        (first, second) ->
      if (first.impacts(second)) {
        update_score(first, second)
        first.collideWith(second, GameEngineConfig.coefficientRestitution)
      }
    }
  }

  fun moveSpaceObjects() {
    this.field.moveShip()
    this.field.moveAsteroids()
    this.field.moveMissiles()
  }

  fun trimSpaceObjects() {
    this.field.trimAsteroids()
    this.field.trimMissiles()
  }

  fun generateAsteroids() {
    val probability = generator.generateProbability()

    if (probability <= GameEngineConfig.asteroidProbability) {
      this.field.generateAsteroid()
    }
  }

  fun renderSpaceField() {
    this.visualizer.renderSpaceField(this.field)
  }
}

fun <T> List<T>.forEachPair(action: (Pair<T, T>) -> Unit) {
  for (i in 0 until this.size) {
    for (j in i + 1 until this.size) {
      action(Pair(this[i], this[j]))
    }
  }
}
