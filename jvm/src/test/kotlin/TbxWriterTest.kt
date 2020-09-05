import writers.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TbxWriterTest {
    @Test
    fun `escapes XML special characters`() {
        val writer = TbxWriter()
        val data = listOf(
            TermEntry(
                listOf(
                    Descrip("<>&type;'\"", "\"'<&value>'\"")
                ),
                listOf(
                    LangSet("en-US", listOf(Term("<term value=\"x\">")))
                ),
            )
        )
        val result = writer.write(data.asSequence())
            .filter { "<term>" in it || "<descrip " in it }
            .toList()
        val expected = listOf(
            "<descrip type=\"&lt;&gt;&amp;type;&apos;&quot;\">" +
                    "&quot;&apos;&lt;&amp;value&gt;&apos;&quot;" +
                    "</descrip>",
            "<term>&lt;term value=&quot;x&quot;&gt;</term>",
        )
        assertEquals(expected, result)
    }
}
