package runner.local

import java.util.ArrayList
import org.apache.log4j.Logger
import java.lang.reflect.InvocationTargetException
import java.io.File
import java.util.Arrays
import java.util.Random
import java.net.URLClassLoader

val LOG_FILE = "out/log.txt"

open class Player(val name: String, val classFile: String)
object MyStrategy : Player("MyStrategy", "#LocalTestPlayer")
object BootstrapStrategy : Player("Bootstrap", "#LocalTestPlayer")
object KeyboardPlayer : Player("KeyboardPlayer", "#KeyboardPlayer")
object EmptyPlayer : Player("EmptyPlayer", javaClass<com.a.b.a.a.e.a>().getSimpleName() + ".class")
object QuickStartGuy : Player("QuickStartGuy", javaClass<com.a.b.a.a.e.b>().getSimpleName() + ".class")

fun localRunner(vis: Boolean, ticks: Int, seed: Long, players: List<Player>): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    val args = arrayListOf(
            "-tick-count=$ticks",
            "-render-to-screen=$vis",
            "-render-to-screen-scale=1.0",
            "-render-to-screen-sync=true",
            "-results-file=$LOG_FILE",
            "-debug=true",
            "-base-adapter-port=31001",
            "-seed=$seed"
    )

    for ((index, player) in players.withIndices()) {
        val i = index + 1
        args.add("-p$i-name=${player.name}")
        args.add("-p$i-team-size=2")
        args.add(player.classFile)
    }

    return com.a.b.c(args.copyToArray())
}

fun runGame(vis: Boolean, ticks: Int, seed: Long, players: List<Player>) {
    val threads = ArrayList<Thread>(2)
    threads add Thread(localRunner(vis, ticks, seed, players))

    var nextPort = 31001L
    for (player in players) {
        if (player == MyStrategy) {
            val port = nextPort++
            threads add Thread {
                Thread.currentThread().setName(if (vis) "local-vis" else "local")
                val runnerClass = Class.forName("Runner")
                runMyStrategy(runnerClass, port)
                val score = runnerClass.getDeclaredMethod("getScore").invoke(null) as IntArray
                val outcome = if (score[0] > score[1]) "WIN" else if (score[0] < score[1]) "LOSE" else "DRAW"
                println("$outcome ${score[0]}:${score[1]}")
            }
        }
        else if (player == BootstrapStrategy) {
            val port = nextPort++
            val classLoader = URLClassLoader(array(File("out/bootstrap").toURI().toURL()), null)
            val runnerClass = classLoader.loadClass("Runner")!!
            threads add Thread {
                runMyStrategy(runnerClass, port)
            }
        }
    }

    threads forEach { it.start() }
    threads forEach { it.join() }
}

fun runMyStrategy(runnerClass: Class<*>, port: Long) {
    val main = runnerClass.getDeclaredMethod("main", javaClass<Array<String>>())
    while (true) {
        try {
            main(null, array("127.0.0.1", "$port", "0000000000000000"))
        } catch (e: InvocationTargetException) {
            if (e.getTargetException()?.getMessage()?.contains("Connection refused") ?: false) {
                Thread.sleep(40)
                continue
            } else throw e.getTargetException() ?: e
        }
        break
    }
}

/**
 * usage: ... player1 player2 ticks seed [-vis]
 * players: empty, quick, keyboard, my, old
 */
fun main(args: Array<String>) {
    val startTime = System.nanoTime()

    fun player(s: String) = when (s) {
        "empty" -> EmptyPlayer
        "quick" -> QuickStartGuy
        "keyboard" -> KeyboardPlayer
        "my" -> MyStrategy
        "old" -> BootstrapStrategy
        else -> error("nice try: $s")
    }
    if (args.size < 3) error("nice try: ${Arrays.toString(args)}")
    val players = listOf(player(args[0]), player(args[1]))
    val ticks = args[2].toInt()
    var seed = args[3].toLong()
    if (seed == 0L) seed = Math.abs(Random().nextLong())
    println("SEED $seed")
    runGame("-vis" in args || KeyboardPlayer in players, ticks, seed, players)

    val log = File(LOG_FILE).readText()
    if (!log.startsWith("OK")) println(log)

    val endTime = System.nanoTime()
    println("%.3fs".format((endTime - startTime) * 1e-9))
}
