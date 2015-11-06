package runner.local

import org.apache.log4j.Logger
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.ConnectException
import java.net.URLClassLoader
import java.util.*

val LOG_FILE = "out/log.txt"

open class Player(val name: String, val classFile: String)
object MyStrategy : Player("MyStrategy", "#LocalTestPlayer")
object BootstrapStrategy : Player("Bootstrap", "#LocalTestPlayer")
object KeyboardPlayer : Player("KeyboardPlayer", "#KeyboardPlayer")
object EmptyPlayer : Player("EmptyPlayer", com.a.b.a.a.e.a::class.java.simpleName + ".class")
object QuickStartGuy : Player("QuickStartGuy", com.a.b.a.a.e.b::class.java.simpleName + ".class")

fun localRunner(vis: Boolean, ticks: Int, seed: Long, teamSize: Int, players: List<Player>): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    val args = arrayListOf(
            "-tick-count=$ticks",
            "-render-to-screen=$vis",
            "-render-to-screen-scale=${if ("Windows" in System.getProperty("os.name")!!) 1.0 else 0.8}",
            "-render-to-screen-sync=true",
            "-results-file=$LOG_FILE",
            "-debug=true",
            "-base-adapter-port=31001",
            "-seed=$seed",
            "-swap-sides=false",
            "-plugins-directory=out/production/plugins"
    )

    for ((index, player) in players.withIndex()) {
        val i = index + 1
        args.add("-p$i-name=${player.name}")
        args.add("-p$i-team-size=$teamSize")
        args.add(player.classFile)
    }

    return com.a.b.c(args.toTypedArray())
}

fun runGame(vis: Boolean, ticks: Int, seed: Long, teamSize: Int, players: List<Player>) {
    val threads = ArrayList<Thread>(2)
    threads.add(Thread(localRunner(vis, ticks, seed, teamSize, players)))

    var nextPort = 31001L
    for (player in players) {
        if (player == MyStrategy) {
            val port = nextPort++
            threads.add(Thread {
                Thread.currentThread().name = if (vis) "local-vis" else "local"
                val runnerClass = Class.forName("Runner")
                runMyStrategy(runnerClass, port)
                val score = runnerClass.getDeclaredMethod("getScore").invoke(null) as IntArray
                val outcome = if (score[0] > score[1]) "WIN" else if (score[0] < score[1]) "LOSE" else "DRAW"
                println("$outcome ${score[0]}:${score[1]}")
            })
        }
        else if (player == BootstrapStrategy) {
            val port = nextPort++
            val classLoader = URLClassLoader(arrayOf(File("out/bootstrap").toURI().toURL()), null)
            val runnerClass = classLoader.loadClass("Runner")!!
            threads.add(Thread {
                runMyStrategy(runnerClass, port)
            })
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

fun runMyStrategy(runnerClass: Class<*>, port: Long) {
    val main = runnerClass.getDeclaredMethod("main", Array<String>::class.java)
    while (true) {
        try {
            main(null, arrayOf("127.0.0.1", "$port", "0000000000000000"))
        } catch (e: InvocationTargetException) {
            if (e.targetException is ConnectException) {
                Thread.sleep(40)
                continue
            } else throw e.targetException ?: e
        }
        break
    }
}

/**
 * usage: ... player1 player2 teamSize ticks seed [-vis]
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
    val teamSize = args[2].toInt()
    val ticks = args[3].toInt()
    var seed = args[4].toLong()
    if (seed == 0L) seed = Math.abs(Random().nextLong())
    val vis = "-vis" in args || KeyboardPlayer in players
    if (vis) println("SEED $seed")

    runGame(vis, ticks, seed, teamSize, players)

    val log = File(LOG_FILE).readText()
    if (vis && !log.startsWith("OK")) println(log)

    val endTime = System.nanoTime()
    if (vis) println("%.3fs".format((endTime - startTime) * 1e-9))
}
