import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.Node
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.isText
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.Element
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.dom.parsing.DOMParser
import org.w3c.files.File
import org.w3c.files.FileReader

class XmlParser(val domParser: DOMParser) {
    sealed class Event {
        data class TagStart(
            val name: String,
            val attributes: Map<String, String>
        ) : Event()
        data class TagEnd(val name: String) : Event()
        data class Text(val contents: String) : Event()
    }

    fun parse(xml: String): Sequence<Event> {
        val doc = domParser.parseFromString(xml, "application/xml")
        return when (val root = doc.firstChild) {
            null -> emptySequence()
            else -> parse(root)
        }
    }

    fun parse(node: Node): Sequence<Event> = sequence {
        if (node.isText) {
            yield(Event.Text(node.textContent ?: ""))
        }
        else {
            val element = node as Element
            val attributes = element.attributes.asList()
                .map { it.name to it.value }
                .toMap()
            yield(Event.TagStart(node.nodeName, attributes))
            for (child in node.childNodes.asList()) {
                yieldAll(parse(child))
            }
            yield(Event.TagEnd(node.nodeName))
        }
    }
}

fun main() {
    val parser = XmlParser(DOMParser())
    val xml = "<roten><ett ichi=\"1\">Hej</ett><två>Hopp</två></roten>"

    window.onload = {
        document.body?.sayHello()

        fun parse(data: String) {
            for (node in parser.parse(data)) {
                document.body?.showText(node.toString())
            }
        }

        fun print(text: Any?) = document.body?.showText(text.toString())

        document.body?.createFileInput { file ->
            val reader = FileReader()
            reader.readAsText(file)
            reader.onload = fun(event: Event) {
                print("Loaded!")
                val data = reader.result as String

                val nodes = parser.parse(data).iterator()
                var count = 0
                val statusElement = document.getElementById("status")

                fun process() {
                    if (!nodes.hasNext())
                        return

                    count++
                    nodes.next()
                    statusElement?.textContent = count.toString()

                    window.requestAnimationFrame {
                        process()
                    }
                }

                process()

//                for (node in parser.parse(data)) {
//                    count++
//                }
//                print("Parsed $count nodes.")
            }
//            window.alert("Got file: $file")
        }

        parse(xml)
    }
}

fun Node.showText(text: String?) {
    append {
        div {
            +(text ?: "")
        }
    }
}

fun Node.createFileInput(callback: (File) -> Unit) {
    append {
        input(InputType.file) {
            onChangeFunction = { event ->
                val fileInput = event.target.asDynamic()
                callback(fileInput.files[0])
            }
        }
    }
}

fun Node.sayHello() {
    append {
        div {
            id = "status"
            +"Hello from JS"
        }
    }
}
