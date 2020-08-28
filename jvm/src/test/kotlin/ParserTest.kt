import XmlParser.Event.*
import java.util.concurrent.Executors
import javax.xml.parsers.SAXParserFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun `parses start and end tags`() {
        val events = parse("<one><two></two></one>").toList()
        val expected = listOf(
            TagStart("one", emptyMap()),
            TagStart("two", emptyMap()),
            TagEnd("two"),
            TagEnd("one"),
        )
        assertEquals(expected, events)
    }

    private fun parse(xml: String): Sequence<XmlParser.Event> {
        val saxFactory = SAXParserFactory.newDefaultInstance()
        val executor = Executors.newSingleThreadExecutor()
        val parser: XmlParser = SaxXmlParser(saxFactory, executor)
        return try {
            parser.parse(xml)
        } finally {
            executor.shutdown()
        }
    }
}
