import XmlParser.Event.*
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

    @Test
    fun `parses attributes`() {
        val events = parse("<e one=\"yi\" two=\"er\"></e>").toList()
        val expected = listOf(
            TagStart("e", mapOf("one" to "yi", "two" to "er")),
            TagEnd("e"),
        )
        assertEquals(expected, events)
    }
    private fun parse(xml: String) = createParser().use { it.parse(xml) }
}
