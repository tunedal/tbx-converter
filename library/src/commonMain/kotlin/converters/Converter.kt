package converters

import readers.Concept
import readers.Transaction
import writers.*

class Converter(private val timestampConverter: TimestampConverter) {
    fun convert(
        concepts: Sequence<Concept>,
        idPrefix: String,
    ): Sequence<TermEntry> {
        return concepts.map { c ->
            TermEntry(
                listOf(
                    Descrip(
                        "conceptId",
                        listOf(idPrefix, c.id.toString()).joinToString("-"))
                ),
                c.languages.map { lang ->
                    LangSet(lang.lang, lang.terms.map { term ->
                        Term(term.value, null,
                            term.transactions
                                .flatMap(this::convertTransaction))
                    })
                },
            )
        }
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
