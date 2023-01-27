package svcs

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.*
import kotlin.streams.toList

const val VCS_ROOT = "vcs"
const val COMMITS_DIRECTORY = "$VCS_ROOT/commits"
const val CONFIG_FILE = "$VCS_ROOT/config.txt"
const val INDEX_FILE = "$VCS_ROOT/index.txt"
const val LOG_FILE = "$VCS_ROOT/log.txt"

fun directoryCreate(directory: String) = Path.of(directory).createDirectory()
fun fileWrite(filename: String, text: String) = Path.of(filename).toFile().writeText(text)
fun fileAppend(filename: String, text: String) = Path.of(filename).toFile().appendText(text)
fun fileRead(filename: String) = Path.of(filename).toFile().readText()
fun fileExists(filename: String) = Path.of(filename).exists()
fun fileCopy(filePathFrom: String, filePathTo: String): Path =
    Files.copy(Path.of(filePathFrom), Path.of(filePathTo), StandardCopyOption.REPLACE_EXISTING)

fun createFileStructure() {
    if (!Path.of(VCS_ROOT).exists()) {
        Path.of("$VCS_ROOT/commits").createDirectories()
        Files.createFile(Path.of("$VCS_ROOT/config.txt"))
        Files.createFile(Path.of("$VCS_ROOT/index.txt"))
        Files.createFile(Path.of("$VCS_ROOT/log.txt"))
    }
}

fun main(args: Array<String>) {
    createFileStructure()
    val userInput = UserInput(args)
    when (userInput.command) {
        "", "--help" -> getHelpPage()
        "add" -> add(userInput)
        "checkout" -> checkout(userInput)
        "commit" -> commit(userInput)
        "config" -> config(userInput)
        "log" -> log()
        else -> "'${userInput.command}' is not a SVCS command."
    }.also { println(it) }
}

fun config(userInput: UserInput): String {
    val name = userInput.parameter
    if (name.isNotEmpty()) {
        fileWrite(CONFIG_FILE, name)
    }
    val nameOnFile = fileRead(CONFIG_FILE)
    return if (nameOnFile.isEmpty()) {
        "Please, tell me who you are."
    } else {
        "The username is $nameOnFile."
    }
}

fun add(userInput: UserInput): String {
    val filenameToAdd = userInput.parameter
    return if (filenameToAdd.isNotEmpty()) {
        if (fileExists(filenameToAdd)) {
            fileAppend(INDEX_FILE, "$filenameToAdd\n")
            "The file '$filenameToAdd' is tracked."
        } else {
            "Can't find '$filenameToAdd'."
        }
    } else {
        val filesOnFile = fileRead(INDEX_FILE)
        if (filesOnFile.isEmpty()) {
            "Add a file to the index."
        } else {
            "Tracked files:\n$filesOnFile"
        }
    }
}

fun log(): String {
    fileRead(LOG_FILE).also {
        return it.ifEmpty {
            "No commits yet."
        }
    }
}

fun isChanges(): Boolean {
    val loggedFiles = fileRead(INDEX_FILE).split("\n").toList().filter { it.isNotEmpty() }.sorted().toList()
    val latestCommitId = fileRead(LOG_FILE).split("\n")[0].replace("commit ", "")
    val committedFiles =
        Files.walk(Path.of("$COMMITS_DIRECTORY/$latestCommitId")).filter { it.isRegularFile() }.map { it.name }
            .toList<String>()

    if (loggedFiles.size != committedFiles.toList().size) {
        return true
    }

    loggedFiles.forEach { filename ->
        val current = fileRead(filename)
        val previous = fileRead("$COMMITS_DIRECTORY/$latestCommitId/$filename")
        if (current != previous) {
            return true
        }
    }
    return false
}

fun commit(userInput: UserInput): String {
    val message = userInput.parameter
    return if (message.isEmpty()) {
        "Message was not passed."
    } else {
        if (isChanges()) {
            UUID.randomUUID().also { uuid ->
                "$COMMITS_DIRECTORY/$uuid".also { directoryPath ->
                    directoryCreate(directoryPath)
                    fileRead(INDEX_FILE).split("\n").forEach { filename ->
                        if (filename.isNotEmpty()) {
                            fileCopy(filename, "$directoryPath/$filename")
                        }
                    }
                }
                val author = fileRead(CONFIG_FILE)
                fileWrite(LOG_FILE, "commit $uuid\nAuthor: $author\n$message\n\n${fileRead(LOG_FILE)}")
            }
            "Changes are committed."
        } else {
            "Nothing to commit."
        }
    }
}

fun checkout(userInput: UserInput): String {
    if (userInput.parameter.isEmpty()) {
        return "Commit id was not passed."
    }
    val path = "$COMMITS_DIRECTORY/${userInput.parameter}"
    return if (!fileExists(path)) {
        "Commit does not exist."
    } else {
        // Copy all files from commitdirectory back to root-directory
        val committedFiles = Files.walk(Path.of(path)).filter { it.isRegularFile() }.map { it.name }.toList<String>()
        committedFiles.forEach { filename ->
            fileCopy("$path/$filename", filename)
        }
        "Switched to commit ${userInput.parameter}."
    }
}

fun getHelpPage() = StringBuilder("These are SVCS commands:\n").also {
    it.appendLine("config     Get and set a username.")
    it.appendLine("add        Add a file to the index.")
    it.appendLine("log        Show commit logs.")
    it.appendLine("commit     Save changes.")
    it.appendLine("checkout   Restore a file.")
}.toString()

class UserInput(args: Array<String>?) {
    private val arguments = arrayOf("--help", "", "")
    val command: String
    val parameter: String

    init {
        if (!args.isNullOrEmpty()) {
            (args.indices).forEach {
                if (args[it].isNotEmpty()) {
                    arguments[it] = args[it]
                }
            }
        }
        command = arguments[0]
        parameter = arguments[1]
    }
}
