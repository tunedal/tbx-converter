import kotlinx.html.div
import kotlinx.html.dom.append
import org.w3c.dom.Node
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.isElement
import kotlinx.dom.isText
import org.w3c.dom.Element
import org.w3c.dom.asList
import org.w3c.dom.parsing.DOMParser

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
        for (node in parser.parse(xml)) {
            document.body?.showText(node.toString())
        }
    }
}

fun Node.showText(text: String?) {
    append {
        div {
            +(text ?: "")
        }
    }
}

fun Node.sayHello() {
    append {
        div {
            +"Hello from JS"
        }
    }
}
