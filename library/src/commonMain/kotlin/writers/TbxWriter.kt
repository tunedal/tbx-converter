package writers

data class TermEntry(
    val descrips: List<Descrip>,
    val languages: List<LangSet>,
)

data class Descrip(
    val type: String,
    val value: String,
)

data class LangSet(
    val isoCode: String,
    val terms: List<Term>,
)

data class Term(
    val value: String,
    val note: String?,
    val termNotes: List<TermNote>,
)

data class TermNote(
    val type: String,
    val value: String,
)

class TbxWriter {
    fun write(concepts: Sequence<TermEntry>): Sequence<String> {
        return sequence {
            yield("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            yield(start("martif", "type" to "TBX"))
            yield(start("text"))
            yield(start("body"))
            for (termEntry in concepts) {
                yield(start("termEntry"))
                for (descrip in termEntry.descrips) {
                    yield(valueTag(
                        "descrip", descrip.value,
                        "type" to descrip.type))
                }
                for (lang in termEntry.languages) {
                    yield(start("langSet", "xml:lang" to lang.isoCode))
                    for (term in lang.terms) {
                        yield(start("tig"))
                        yield(valueTag("term", term.value))
                        term.note?.let {
                            yield(valueTag("note", it))
                        }
                        for(note in term.termNotes) {
                            yield(valueTag(
                                "termNote", note.value,
                                "type" to note.type))
                        }
                        yield(end("tig"))
                    }
                    yield(end("langSet"))
                }
                yield(end("termEntry"))
            }
            yield(end("body"))
            yield(end("text"))
            yield(end("martif"))
        }
    }

    private fun valueTag(
        name: String,
        value: String,
        vararg attrs: Pair<String, Any>,
    ) = start(name, *attrs) + value + end(name)

    private fun start(name: String, vararg attrs: Pair<String, Any>) =
        "<$name" + attrs.joinToString { " ${it.first}=\"${it.second}\"" } + ">"

    private fun end(name: String): String = "</$name>"
}
