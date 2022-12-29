package svcs

fun main(args: Array<String>) {
    println("Hello this World!")
    if (args.isEmpty() || args[0] == "--help") {
        println(getHelpPage())
    } else {
        when (args[0]) {
            "config" -> println("Get and set a username.")
            "add" -> println("Add a file to the index.")
            "log" -> println("Show commit logs.")
            "commit" -> println("Save changes.")
            "checkout" -> println("Restore a file.")
            else -> println("'${args[0]}' is not a SVCS command.")
        }
    }
}

fun getHelpPage() =
    "These are SVCS commands:\n" +
            "config     Get and set a username.\n" +
            "add        Add a file to the index.\n" +
            "log        Show commit logs.\n" +
            "commit     Save changes.\n" +
            "checkout   Restore a file."