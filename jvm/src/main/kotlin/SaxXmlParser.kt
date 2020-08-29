import XmlParser.Event.*
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class SaxXmlParser(
    private val saxParserFactory: SAXParserFactory,
    private val executor: Executor
) : XmlParser {
    private val queue = ArrayBlockingQueue<XmlParser.Event>(100)

    override fun parse(xml: String): Sequence<XmlParser.Event> {
        // See also: https://www.extreme.indiana.edu/xgws/papers/xml_push_pull/node3.html

        executor.execute {
            val xmlReader = createSaxParser().xmlReader.apply {
                contentHandler = Handler()
            }
            xmlReader.parse(InputSource(StringReader(xml)))
            queue.put(DocumentEnd)
        }

        return generateSequence {
            when (val event = queue.take()) {
                DocumentEnd -> null
                else -> event
            }
        }
    }

    private fun createSaxParser(): SAXParser {
        synchronized(this) {
            return saxParserFactory.newSAXParser()
        }
    }

    private fun push(event: XmlParser.Event) {
        queue.put(event)
    }

    private inner class Handler : DefaultHandler() {
        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String,
            attributes: Attributes
        ) {
            val attrMap = (0 until attributes.length)
                .map { i -> attributes.getQName(i) to attributes.getValue(i) }
                .toMap()
            push(TagStart(qName, attrMap))
        }

        override fun endElement(
            uri: String?,
            localName: String?,
            qName: String
        ) {
            push(TagEnd(qName))
        }
    }
}
