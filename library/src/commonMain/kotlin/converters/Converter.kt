package converters

import readers.Concept
import readers.Transaction
import writers.*

class Converter(private val timestampConverter: TimestampConverter) {
    fun convert(
        concepts: Sequence<Concept>,
        commentTags: Set<String>,
        idPrefix: String?,
    ): Sequence<TermEntry> {
        return concepts.map { c ->
            TermEntry(
                listOf(
                    Descrip(
                        "conceptId",
                        when(idPrefix) {
                            is String -> "$idPrefix-${c.id}"
                            else -> "${c.id}"
                        }
                    )
                ),
                c.languages.map { lang ->
                    LangSet(lang.lang, lang.terms.map { term ->
                        Term(term.value,
                            convertComment(term, commentTags),
                            term.transactions
                                .flatMap(this::convertTransaction))
                    })
                },
            )
        }
    }

    private fun convertComment(term: readers.Term, commentTags: Set<String>): String? {
        val commentElements = term.descrip.asIterable()
            .filter { it.key in commentTags }
        if (commentElements.size > 1)
            error("Multiple comments elements for term ${term.value}")
        return commentElements.singleOrNull()?.value
    }

    private fun convertTransaction(it: Transaction): List<TermNote> {
        return when (it) {
            is Transaction.Origination -> listOf(
                TermNote("createdBy", it.author),
                TermNote("createdAt", convertDate(it.date)),
            )
            is Transaction.Modification -> listOf(
                TermNote("lastModifiedBy", it.author),
                TermNote("lastModifiedAt", convertDate(it.date)),
            )
        }
    }

    private fun convertDate(date: String): String {
        val timestamp = timestampConverter.timestampFromIsoDate(date)
        val millisecondTimestamp = timestamp * 1000
        return millisecondTimestamp.toString()
    }
}

interface TimestampConverter {
    fun timestampFromIsoDate(date: String): Long
}
