package readers

import XmlParser
import XmlParser.Event.*

data class Concept(val id: Int)

data class Transaction(val type: String, val date: String)

class SdlTradosReader(private val parser: XmlParser) {
    // TODO: Maybe look into using streams from kotlinx-io.

    fun read(data: String): Sequence<Concept> = sequence {
        val parseEvents = parser.parse(data).iterator()

        fun readConcept(tag: TagStart): Int {
            val buf = mutableListOf<String>()
            for (event in parseEvents.readUntilEndTag(tag.name)) {
                if (event !is Text)
                    error("Expected text content: $event")
                buf += event.contents
            }
            val text = buf.joinToString("")
            return text.toInt()
        }

        fun readConceptGrp(tag: TagStart): Concept {
            var id: Int? = null
            for (event in parseEvents.readUntilEndTag(tag.name)) {
                when (event) {
                    is TagStart -> {
                        if (event.name == "concept") {
                            id = readConcept(event)
                        }
                    }
                    else -> Unit
                }
            }
            return Concept(id ?: error("Missing concept ID"))
        }

        fun readMtf(tag: TagStart): Sequence<Concept> = sequence {
            for (event in parseEvents.nonBlanks())
                if (event == TagEnd("mtf"))
                    break
                else if (event is TagStart && event.name == "conceptGrp") {
                    val concept = readConceptGrp(event)
                    yield(concept)
                }
                else {
                    error("Expected conceptGrp: $event")
                }
            }

        fun read(): Sequence<Concept> = sequence {
            val root = parseEvents.next()
            if (root == TagStart("mtf"))
                yieldAll(readMtf(root as TagStart))
        }

        yieldAll(read())
    }

    private fun Iterator<XmlParser.Event>.readStartTag(tag: String): TagStart {
        while (true) {
            when (val event = next()) {
                is TagStart -> {
                    if (event.name == tag)
                        return event
                    else
                        error("Expected $tag: $event")
                }
                is Text -> {
                    if (event.contents.isNotBlank())
                        error("Expected $tag: $event")
                }
                else -> error("Expected $tag: $event")
            }
        }
    }

    private fun Iterator<XmlParser.Event>.readUntilEndTag(endTag: String) =
        generateSequence {
            when (val event = next()) {
                TagEnd(endTag) -> null
                else -> event
            }
        }

    private fun Iterator<XmlParser.Event>.readNonBlank(): XmlParser.Event? {
        while (hasNext()) {
            val event = next()
            if (event is Text && event.contents.isBlank())
                continue
            return event
        }
        return null
    }

    private fun Iterator<XmlParser.Event>.nonBlanks() = sequence {
        while (true) {
            val event = readNonBlank() ?: break
            yield(event)
        }
    }
}
