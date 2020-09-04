package converters

import readers.Concept
import readers.Transaction
import writers.*

class Converter {
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
                        Term(term.value, null, term.transactions.flatMap {
                            when (it) {
                                is Transaction.Origination -> listOf(
                                    TermNote("createdBy", it.author),
                                    TermNote("createdAt", it.date),
                                )
                                is Transaction.Modification -> listOf(
                                    TermNote("lastModifiedBy", it.author),
                                    TermNote("lastModifiedAt", it.date),
                                )
                            }
                        })
                    })
                },
            )
        }
    }
}
