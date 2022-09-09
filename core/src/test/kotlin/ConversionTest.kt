import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionTest {
    @Test
    fun `converts SDL Trados example to Memsource example`() {
        val inputData = readResource("sdl-trados-example.xml")

        val output = SdlTradosConverter().use { converter ->
            val outputLines = converter.convert(
                inputData, idPrefix = "testprefix").flatten()
            outputLines.joinToString("\n", transform = String::trim)
        }

        val expected = readResource("memsource-example.xml")
            .joinToString("\n", transform = String::trim)
        assertEquals(expected, output)
    }

    @Test
    fun `splits output into batches of requested size`() {
        val inputData = readResource("sdl-trados-example.xml")

        val items = SdlTradosConverter().use { converter ->
            val batches = converter.convert(
                inputData, idPrefix = "batchitem", batchSize = 2)
            val pattern = ".*(batchitem-[0-9]+).*".toRegex()
            batches.flatMapIndexed { i, batch -> batch
                .filter { "conceptId" in it }
                .map { Pair(i, it.replace(pattern, "$1")) }
            }.toList()
        }

        val expected = listOf(
            0 to "batchitem-1",
            0 to "batchitem-60",
            1 to "batchitem-3000",
        )
        assertEquals(expected, items)
    }

    private fun readResource(name: String) =
        this.javaClass.getResourceAsStream(name).reader().readLines()
}
