import readers.Concept
import readers.SdlTradosReader
import kotlin.test.Test
import kotlin.test.assertEquals

class SdlTradosReaderTest {
    @Test
    fun `reads concepts`() {
        val reader = SdlTradosReader(createParser())
        val xmlData = readResource("sdl-trados-example.xml")
        val concepts = reader.read(xmlData).toList()
        val expected = listOf(
            Concept(1),
            Concept(60),
            Concept(3000),
        )
        assertEquals(expected, concepts)
    }

    private fun readResource(name: String) =
        this.javaClass.getResourceAsStream(name).reader().readText()
}
