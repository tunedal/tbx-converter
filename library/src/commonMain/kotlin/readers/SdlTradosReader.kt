package readers

import XmlParser
import XmlParser.Event.*

data class Concept(val id: Int, val transactions: List<Transaction>)

sealed class Transaction {
    abstract val author: String
    abstract val date: String

    data class Origination(
        override val author: String,
        override val date: String,
    ) : Transaction()

    data class Modification(
        override val author: String,
        override val date: String,
    ) : Transaction()
}

class SdlTradosReader(private val parser: XmlParser) {
    // TODO: Maybe look into using streams from kotlinx-io.

    fun read(data: String): Sequence<Concept> = sequence {
        val parseEvents = parser.parse(data).iterator()

        fun readTextContent(tag: TagStart): String {
            val buf = mutableListOf<String>()
            for (event in parseEvents.readUntilEndTag(tag.name)) {
                if (event !is Text)
                    error("Expected text content: $event")
                buf += event.contents
            }
            return buf.joinToString("")
        }

        fun readTransacGrp(tag: TagStart): Transaction {
            lateinit var transactionType: String
            lateinit var author: String
            lateinit var date: String
            for (event in parseEvents.readUntilEndTag(tag.name).nonBlanks()) {
                when ((event as TagStart).name) {
                    "transac" -> {
                        transactionType = event.attributes["type"]
                            ?: error("Missing transaction type")
                        author = readTextContent(event)
                    }
                    "date" -> {
                        date = readTextContent(event)
                    }
                    else -> error("Unexpected transaction element: $event")
                }
            }
            return when (transactionType) {
                "origination" -> Transaction.Origination(author, date)
                "modification" -> Transaction.Modification(author, date)
                else -> error("Unexpected transaction type: $transactionType")
            }
        }

        fun readConceptGrp(tag: TagStart): Concept {
            var id: Int? = null
            val transactions = mutableListOf<Transaction>()
            for (event in parseEvents.readUntilEndTag(tag.name)) {
                when (event) {
                    is TagStart -> {
                        if (event.name == "concept") {
                            id = readTextContent(event).toInt()
                        }
                        else if (event.name == "languageGrp") {
                            parseEvents.readUntilEndTag(event.name).consume()
                        }
                        else if (event.name == "transacGrp") {
                            transactions += readTransacGrp(event)
                        }
                        else {
                            error("Unexpected element: $event")
                        }
                    }
                    else -> Unit
                }
            }
            return Concept(
                id ?: error("Missing concept ID"),
                transactions,
            )
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

    private fun Sequence<XmlParser.Event>.nonBlanks() = iterator().nonBlanks()

    private fun <T> Sequence<T>.consume() {
        val iterator = this.iterator()
        while (iterator.hasNext())
            iterator.next()
    }
}
