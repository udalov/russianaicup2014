package runner.local

import runner.*
import java.io.File

fun main(args: Array<String>) {
    runGame(true, 0, listOf(QuickStartGuy, MyStrategy))
    println(File(LOG_FILE).readText())
}
