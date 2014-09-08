package runner.keyboard

import runner.*
import java.io.File

fun main(args: Array<String>) {
    runGame(true, 0, listOf(EmptyPlayer, KeyboardPlayer))
    println(File(LOG_FILE).readText())
}
