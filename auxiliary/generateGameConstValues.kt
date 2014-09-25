package generate.const

import java.io.File
import java.util.regex.Pattern

val pattern = Pattern.compile("^    public static (\\w+) (\\w+);$")

fun main(args: Array<String>) {
    val values = File("game-const.txt").readLines().map { line ->
        val a = line.split(' ')
        Pair(a[0], a[1])
    }.toMap()

    val file = File("src/Const.java")
    file.writeText(file.readLines().map { line ->
        val matcher = pattern.matcher(line)
        if (matcher.matches()) {
            line.substring(0, line.length - 1) + " = " + values[matcher.group(2)] + ";"
        } else {
            line
        }
    }.joinToString(separator = "\n", postfix = "\n"))
}
