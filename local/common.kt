package runner

import java.net.ConnectException
import java.util.ArrayList
import org.apache.log4j.Logger

val LOG_FILE = "out/log.txt"

open class Player(val name: String, val classFile: String)
object MyStrategy : Player("MyStrategy", "#LocalTestPlayer")
object KeyboardPlayer : Player("KeyboardPlayer", "#KeyboardPlayer")
object EmptyPlayer : Player("EmptyPlayer", javaClass<com.a.b.a.a.e.a>().getSimpleName() + ".class")
object QuickStartGuy : Player("QuickStartGuy", javaClass<com.a.b.a.a.e.b>().getSimpleName() + ".class")

fun localRunner(vis: Boolean, seed: Long, players: List<Player>): Runnable {
    Logger.getRootLogger()?.removeAllAppenders()

    val args = arrayListOf(
            "-tick-count=6000",
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

fun runGame(vis: Boolean, seed: Long, players: List<Player>) {
    val threads = ArrayList<Thread>(2)
    threads add Thread(localRunner(vis, seed, players))

    // TODO: support multiple local strategies on different ports
    for (player in players) {
        if (player == MyStrategy) {
            threads add Thread {
                Thread.currentThread().setName("local")
                runMyStrategy(31001)
            }
            break
        }
    }

    threads forEach { it.start() }
    threads forEach { it.join() }
}

fun runMyStrategy(port: Long) {
    while (true) {
        try {
            val main = Class.forName("Runner").getDeclaredMethod("main", javaClass<Array<String>>())
            main(null, array("127.0.0.1", "$port", "0000000000000000"))
        } catch (e: ConnectException) {
            if (e.getMessage()?.startsWith("Connection refused") ?: false) {
                Thread.sleep(40)
                continue
            } else throw e
        }
        break
    }
}
