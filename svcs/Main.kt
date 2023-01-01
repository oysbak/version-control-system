package svcs

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

fun main(args: Array<String>) {
    val userInput = UserInput(args)
    when (userInput.command) {
        "", "--help" -> printHelpPage()
        "add" -> Add(userInput.name)
        "checkout" -> println("Restore a file.")
        "commit" -> println("Save changes.")
        "config" -> Config(userInput.name)
        "log" -> println("Show commit logs.")
        else -> println("'${userInput.command}' is not a SVCS command.")
    }
}

class Config(inputName: String) {
    private val configFile = ProjectFile("vcs", "config.txt", "")

    init {
        if (inputName.isNotEmpty()) {
            configFile.write(inputName)
        }
        val nameOnFile = configFile.read()
        if (nameOnFile.isEmpty()) {
            println("Please, tell me who you are.")
        } else {
            println("The username is $nameOnFile.")
        }
    }
}

class Add(filename: String) {
    private val indexFile = ProjectFile("vcs", "index.txt", "Tracked files:")

    init {
        if (filename.isNotEmpty()) {
            if (File(filename).exists()) {
                indexFile.append("\n$filename")
                println("The file '$filename' is tracked.")
            } else {
                println("Can't find '$filename'.")
            }
        }
        val filesOnFile = indexFile.read()
        if (filesOnFile.length < 15) {
            println("Add a file to the index.")
        } else {
            if (filename.isEmpty()) {
                println(filesOnFile)
            }
        }
    }
}

class ProjectFile(directory: String, filename: String, initialContent: String) {
    private val file = File("$directory/$filename")

    init {
        if (!Path(directory).exists()) {
            Path(directory).createDirectory()
        }
        if (!file.exists()) {
            file.writeText(initialContent)
        }
    }

    fun read() = file.readText().trim()
    fun write(text: String) = file.writeText(text)
    fun append(text: String) = file.appendText(text)
}

class UserInput(args: Array<String>) {
    val command = if (args.isEmpty()) "--help" else args[0].trim().lowercase()
    val name = if (args.size > 1) args[1].trim().lowercase() else ""
    fun isNotEmpty() = name.isNotEmpty()
}

fun printHelpPage() {
    println("These are SVCS commands:")
    println("config     Get and set a username.")
    println("add        Add a file to the index.")
    println("log        Show commit logs.")
    println("commit     Save changes.")
    println("checkout   Restore a file.")
}
