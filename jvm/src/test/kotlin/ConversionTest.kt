import writers.TbxWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionTest {
    @Test
    fun `converts SDL Trados example to Memsource example`() {
        val writer = TbxWriter()
        val output = writer.write(emptySequence())
            .joinToString("\n", transform = String::trim)
        val expected = readResource("memsource-example.xml")
            .joinToString("\n", transform = String::trim)
        assertEquals(expected, output)
    }

    private fun readResource(name: String) =
        this.javaClass.getResourceAsStream(name).reader().readLines()
}
