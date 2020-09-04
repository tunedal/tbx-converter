import converters.Converter
import readers.SdlTradosReader
import writers.TbxWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionTest {
    @Test
    fun `converts SDL Trados example to Memsource example`() {
        val inputData = readResource("sdl-trados-example.xml")
        val reader = SdlTradosReader(createParser())
        val concepts = reader.read(inputData.joinToString("\n"))

        val converter = Converter(JTimestampConverter())
        val convertedConcepts = converter.convert(concepts, "testprefix")

        val writer = TbxWriter()
        val output = writer.write(convertedConcepts)
            .joinToString("\n", transform = String::trim)

        val expected = readResource("memsource-example.xml")
            .joinToString("\n", transform = String::trim)
        assertEquals(expected, output)
    }

    private fun readResource(name: String) =
        this.javaClass.getResourceAsStream(name).reader().readLines()
}
