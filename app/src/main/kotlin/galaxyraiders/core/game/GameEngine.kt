package galaxyraiders.core.game

import galaxyraiders.Config
import galaxyraiders.ports.RandomGenerator
import galaxyraiders.ports.ui.Controller
import galaxyraiders.ports.ui.Controller.PlayerCommand
import galaxyraiders.ports.ui.Visualizer
import kotlin.system.measureTimeMillis
import java.io.File
import java.time.LocalDateTime
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


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
  var board : Leaderboard 
  var time  = ""

  //Read from json file the 3 first high score players and the nº of the next player
  fun read_leaderboard(){
    val path : String = "../score/Leaderboard.json"
    var json_string : String = File(path).readText(Charsets.UTF_8)
    leader_board = mapper.readValue(json_string) // deserialize data to String

  }

  // get start time of game
  fun get_time(){
    time = LocalDateTime.now().toString()
  }
  
  fun update_leaderboard(){
    var new_player : Classification = Classification(next_player,score,time,hit_asteroids)
    if(score > leader_board.third.score){
      leader_board.third = next_player
      if(score > leader_board.second.score){
        leader_board.third = leader_board.second
        leader_board.second = next_player
        if(score > leader_board.first.score){
          leader_board.second = leader_board.first
          leader_board.first = next_player
        }
      }  
    }
    leader_board.next_player = leader_board.next_player + 1
  }

  //Write new Leaderboard.json
  fun write_leaderboard(){
    val path : String = "../score/Leaderboard.json"
    File(path).writeText("")   // empty Leaderboard.json
    update_leaderboard()
    val data = mapper.writeValueAsString(leader_board)  //serialize data to json format
    Files.write(path, data.toByteArray(), StandardOpenOption.APPEND)
  }

  //Write new score in Scoreboard.json
  fun write_scoreboard(){
    val path : String = "../score/Scoreboard.json"
    var new_player : Classification = Classification(next_player,score,time,hit_asteroids)
    val data = mapper.writeValueAsString(next_player)
    Files.write(path, data.toByteArray(), StandardOpenOption.APPEND)
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

  fun increase_score(asteroid : spaceObjects){
    var points = asteroid.radius*asteroid.mass
    score = score + points
    hit_asteroids = hit_asteroids + 1
  }

  fun update_score(first : spaceObjects, second : spaceObjects){
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
