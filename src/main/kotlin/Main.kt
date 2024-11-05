import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.nio.file.Files
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter

private val f = JFrame()
private val plainTextDisplay = JTextArea("The plain text of the file will be displayed here")
private val optionalAnnotationButton = JToggleButton("Keep Markdown Annotation")
private var keepAnnotaion = false

fun main(args: Array<String>) {

    //Opening the mainFrame
    mainFrame()
}

fun mainFrame(){

    //Button for selecting a file to open
    val openFileButton = JButton("Select File")
    openFileButton.setBounds(50, 20, 100, 40)
    //adding functionality
    openFileButton.addActionListener { e -> selectFileMouseClicked(e) }

    //Button for selecting whether to keep Markdown annotation
    optionalAnnotationButton.setBounds(200, 20, 200, 40)
    optionalAnnotationButton.isSelected = keepAnnotaion
    optionalAnnotationButton.addActionListener { e -> toggleAnnotationMouseClicked(e)}

    //Text display
    plainTextDisplay.setEnabled(false) //disables editing of the text display area
    plainTextDisplay.disabledTextColor = Color.BLACK //making text easier to read
    plainTextDisplay.setLineWrap(true)
    //OPTIONAL: Make it scrollable
    val scrollableText = JScrollPane(plainTextDisplay)
    scrollableText.setBounds(50, 75, 400, 250)


    //Adding elements to frame
    f.add(openFileButton)
    f.add(scrollableText)
    f.add(optionalAnnotationButton)

    //Formatting frame
    f.setSize(500, 400)
    f.layout = null
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    f.isVisible = true //making the frame visible
}

fun toggleAnnotationMouseClicked(e: ActionEvent) {
    keepAnnotaion = !keepAnnotaion
}

fun selectFileMouseClicked(e: ActionEvent){
    val fileChooser = JFileChooser()

    //OPTIONAL: Filter file types
    fileChooser.setFileFilter(object: FileFilter() {
        override fun getDescription(): String {
            return "Markdown Files (*.md)"
        }

        override fun accept(f: File): Boolean {
            return if (f.isDirectory()) {
                true
            } else {
                val filename = f.getName().lowercase(Locale.getDefault())
                filename.endsWith(".md")
            }
        }
    })

    val returnValue = fileChooser.showOpenDialog(null)
    if (returnValue == JFileChooser.APPROVE_OPTION){
        plainTextDisplay.text = fileToPlainText(fileChooser.selectedFile)
    }
}

fun fileToPlainText(file: File): String {
    val finalStringBuilder: StringBuilder = StringBuilder()

    val fileLines = Files.readAllLines(file.toPath())
    fileLines.forEach {
        finalStringBuilder.append(it)
        finalStringBuilder.append("\n")
    }

    return if (keepAnnotaion){
        finalStringBuilder.toString()
    } else {
        removeFormatting(finalStringBuilder.toString())
    }
}

fun removeFormatting(unformattedText: String) : String{
    var formattedText = unformattedText

    //somewhat extensive list of regex expressions for detecting various Markdown elements
    //source: https://www.markdownguide.org/cheat-sheet/

    //tables make more visual sense without removing the formatting
    //val tablePattern = "/^(\\|[^\\n]+\\|\\r?\\n)((?:\\|:?[-]+:?)+\\|)(\\n(?:\\|[^\\n]+\\|\\r?\\n?)*)?\$/gm"

    //elements that make the text clearer when removed
    val headersDividersPattern = Regex("(?m)^#{1,6}[^\\\\](.*)|^[-=]{2,}\n|^[\\*_-]{3,}\n")
    val boldItalicPattern = Regex("([^\\\\\\*])[\\*_]{1}([^\\*\\r\\n\\f\\v\\\\]+)([^\\\\\\*])[\\*_]{1}([^\\s\\\\\\*]*)|" +
            "([^\\\\\\*])[\\*_]{2}([^\\*\\r\\n\\f\\v\\\\]+)([^\\\\\\*])[\\*_]{2}([^\\s\\\\\\*]*)|" +
            "([^\\\\\\*])[\\*_]{3}([^\\*\\r\\n\\f\\v\\\\]+)([^\\\\\\*])[\\*_]{3}([^\\s\\\\\\*]*)")
    val strikethroughSubscrSuperscrPattern = Regex("~{2}(.*)~{2}|[~^]{1}(.)[~^]{1}")
    val highlightPattern = Regex("={2}(.*)={2}")
    val escapingCharsPattern = Regex("\\\\+([\\\\`\\*_\\{\\}\\[\\]<>\\(\\)#+-\\.\\!\\|])")
    val removeExtraSlashesPattern = Regex("\\\\*+")

    //elements that are possibly visually better left with Markdown formatting for legibility
    val emojiPattern = Regex(":{1}(.+):{1}")
    val listsPattern = Regex("(?m)^\\s*[-\\*+](.*)|^\\s*\\d+\\.(.*)")
    val blockQuotePattern = Regex("(?m)^>+(.*)")
    val codePattern = Regex("(?m)`{3}\\n([\\S\\s]+)`{3}\\n|`{1,2}(.*)`{1,2}")


    //links do not work in plain text format, but can still be left to copy
    //images cannot be displayed in plain text and are therefore simply described by file name
    val linkImagePattern = Regex("(?m)\\!?\\[.*\\]\\((.*)\\)|<(.*)>")
    val refLinkPattern = Regex("(?m)\\[([^\\[\\]]*)\\]\\s*\\[\\d+\\]|^\\[\\d+\\]:\\s+<?([^\\\"'(]*)>?(\\s*[\\\"'(](.*)[\\\"')])?")

    //common replacement groups
    val repl1gr = "\$1"
    val repl2gr = "\$1\$2"

    //applying all the patterns and replacements accordingly
    formattedText = formattedText.replace(blockQuotePattern, repl1gr) //goes first since other elements can be nested within
    formattedText = formattedText.replace(boldItalicPattern, "\$1\$2\$3\$4\$5\$6\$7\$8\$9\$10\$11\$12")
    formattedText = formattedText.replace(headersDividersPattern, repl1gr)
    formattedText = formattedText.replace(strikethroughSubscrSuperscrPattern, repl2gr)
    formattedText = formattedText.replace(highlightPattern, repl1gr)
    formattedText = formattedText.replace(emojiPattern, repl1gr)
    formattedText = formattedText.replace(listsPattern, repl2gr)
    formattedText = formattedText.replace(codePattern, repl2gr)
    formattedText = formattedText.replace(linkImagePattern, repl1gr)
    formattedText = formattedText.replace(refLinkPattern, "\$1\$2\$3")
    formattedText = formattedText.replace(escapingCharsPattern, repl1gr)
    formattedText = formattedText.replace(removeExtraSlashesPattern, "")

    return formattedText
}

