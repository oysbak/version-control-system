package svcs

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.streams.toList

const val VCS_ROOT = "vcs"
const val COMMITS_DIRECTORY = "$VCS_ROOT/commits"
const val CONFIG_FILE = "$VCS_ROOT/config.txt"
const val INDEX_FILE = "$VCS_ROOT/index.txt"
const val LOG_FILE = "$VCS_ROOT/log.txt"

fun directoryCreate(directory: String) = Path.of(directory).createDirectory()
fun fileCreate(filename: String) = Path.of(filename).createFile()
fun fileWrite(filename: String, text: String) = Path.of(filename).toFile().writeText(text)
fun fileAppend(filename: String, text: String) = Path.of(filename).toFile().appendText(text)
fun fileRead(filename: String) = Path.of(filename).toFile().readText()
fun fileExists(filename: String) = Path.of(filename).exists()
fun fileCopy(filePathFrom: String, filePathTo: String): Path = Files.copy(Path.of(filePathFrom), Path.of(filePathTo))

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
        "checkout" -> checkout()
        "commit" -> commit(userInput)
        "config" -> config(userInput)
        "log" -> log()
        "fil" -> fil(userInput)
        "exit" -> "..bye"
        else -> "'${userInput.command}' is not a SVCS command."
    }.also { println(it) }
}

fun fil(userInput: UserInput): String {
    val nameAndContent = userInput.parameter.split(" ", ignoreCase = false, limit = 2).toTypedArray()
    if (!fileExists(nameAndContent[0])) {
        fileCreate(nameAndContent[0])
    }
    fileWrite(nameAndContent[0], nameAndContent[1])
    return "fil opprettet/endret"
}

fun isChanges(): Boolean {
    val loggedFiles = fileRead(INDEX_FILE).split("\n").toList().filter { it.isNotEmpty() }.sorted().toList()
    val latestCommitId = fileRead(LOG_FILE).split("\n")[0].replace("commit ", "")
    val commitedFiles = Files.walk(Path.of("$COMMITS_DIRECTORY/$latestCommitId")).filter { it.isRegularFile() }.map { it.name }.toList<String>()

    if (loggedFiles.size != commitedFiles.toList().size) {
        return true
    }

    loggedFiles.forEach { filename ->
        val innhold = fileRead(filename)
        val tidligere = fileRead("$COMMITS_DIRECTORY/$latestCommitId/$filename")
        if (innhold != tidligere) {
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
                "$COMMITS_DIRECTORY/$uuid".also { directorypath ->
                    directoryCreate(directorypath)
                    fileRead(INDEX_FILE).split("\n").forEach { filename ->
                        if (filename.isNotEmpty()) {
                            fileCopy(filename, "$directorypath/$filename")
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

fun log(): String {
    fileRead(LOG_FILE).also {
        return it.ifEmpty {
            "No commits yet."
        }
    }
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

fun checkout(): String = "Restore a file."

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

fun getHelpPage() = StringBuilder("These are SVCS commands:\n").also {
    it.appendLine("config     Get and set a username.")
    it.appendLine("add        Add a file to the index.")
    it.appendLine("log        Show commit logs.")
    it.appendLine("commit     Save changes.")
    it.appendLine("checkout   Restore a file.")
}.toString()