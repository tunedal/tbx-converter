import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionTest {
    @Test
    fun `converts SDL Trados example to Memsource example`() {
        val inputData = readResource("sdl-trados-example.xml")

        val output = SdlTradosConverter().use { converter ->
            val outputLines = converter.convert(
                inputData, idPrefix = "testprefix")
            outputLines.joinToString("\n", transform = String::trim)
        }

        val expected = readResource("memsource-example.xml")
            .joinToString("\n", transform = String::trim)
        assertEquals(expected, output)
    }

    private fun readResource(name: String) =
        this.javaClass.getResourceAsStream(name).reader().readLines()
}
