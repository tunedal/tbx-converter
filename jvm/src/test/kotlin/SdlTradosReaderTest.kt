import readers.*
import kotlin.test.Test
import kotlin.test.assertEquals

class SdlTradosReaderTest {
    @Test
    fun `reads concepts`() {
        val reader = SdlTradosReader(createParser())
        val xmlData = readResource("sdl-trados-example.xml")
        val concepts = reader.read(xmlData).toList()
        val expected = listOf(
            Concept(
                1,
                listOf(
                    Language("swedish", "SV-SE", listOf(
                        Term("Swedish term one", listOf()),
                        Term("synonym for Swedish term one", listOf()),
                    )),
                    Language("english", "EN-GB", listOf(
                        Term("term one in English", listOf()),
                    )),
                ),
                listOf(
                    Transaction.Origination("super", "2000-09-13T03:25:46"),
                    Transaction.Modification("super", "2000-09-13T03:25:46"),
                )
            ),
            Concept(
                60,
                listOf(
                    Language("swedish", "SV-SE", listOf(
                        Term("term 60 in Swedish", listOf()),
                    )),
                    Language("english", "EN-GB", listOf(
                        Term("term 60 in English", listOf()),
                    )),
                ),
                listOf(
                    Transaction.Origination("super", "2000-09-13T03:25:47"),
                    Transaction.Modification("super", "2001-01-24T01:44:12"),
                )
            ),
            Concept(
                3000,
                listOf(
                    Language("english", "EN-GB", listOf(
                        Term("term 3000 in English", listOf()),
                        Term("synonym for term 3000 in English", listOf()),
                    )),
                    Language("swedish", "SV-SE", listOf(
                        Term("term 3000 in Swedish", listOf()),
                    )),
                ),
                listOf(
                    Transaction.Origination("Example User", "2020-08-22T18:47:42"),
                    Transaction.Modification("Example User", "2020-08-22T18:47:42"),
                )
            ),
        )
        assertEquals(expected, concepts)
    }

    private fun readResource(name: String) =
        this.javaClass.getResourceAsStream(name).reader().readText()
}
