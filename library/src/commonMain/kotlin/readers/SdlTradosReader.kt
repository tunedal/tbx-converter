package readers

import XmlParser
import XmlParser.Event.*

data class Concept(val id: Int)

class SdlTradosReader(private val parser: XmlParser) {
    // TODO: Maybe look into using streams from kotlinx-io.
    fun read(data: String): Sequence<Concept> {
        val parseEvents = parser.parse(data)
        return sequence {
            var readingConcept = false
            val conceptText = mutableListOf<String>()
            for (event in parseEvents) {
                when (event) {
                    is TagStart -> {
                        readingConcept = event.name == "concept"
                        if (readingConcept)
                            conceptText.clear()
                    }
                    is Text -> {
                        if (readingConcept)
                            conceptText += event.contents
                    }
                    is TagEnd -> {
                        if (readingConcept) {
                            val text = conceptText.joinToString("")
                            yield(Concept(text.toInt()))
                        }
                    }
                    is DocumentEnd -> {}
                }
            }
        }
    }
}
