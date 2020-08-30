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
    val terms: List<Term>,
)

data class Term(
    val value: String,
    val note: String,
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
            yield(end("body"))
            yield(end("text"))
            yield(end("martif"))
        }
    }

    private fun start(name: String, vararg attrs: Pair<String, Any>) =
        "<$name" + attrs.joinToString { " ${it.first}=\"${it.second}\"" } + ">"

    private fun end(name: String): String = "</$name>"
}
