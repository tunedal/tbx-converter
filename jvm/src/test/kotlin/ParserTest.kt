import XmlParser.Event.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun `parses start and end tags`() {
        val events = parse("<one><two></two></one>").toList()
        val expected = listOf(
            TagStart("one"),
            TagStart("two"),
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

    @Test
    fun `parses text content`() {
        val events = parse("<p>Greetings and <i>salutations</i>!</p>").toList()
        val expected = listOf(
            TagStart("p"),
            Text("Greetings and "),
            TagStart("i"),
            Text("salutations"),
            TagEnd("i"),
            Text("!"),
            TagEnd("p"),
        )
        assertEquals(expected, events)
    }

    private fun parse(xml: String) = createParser().use { it.parse(xml) }
}
