import sys
from datetime import datetime, timezone
from typing import Iterable

from .trados_reader import Concept, TransactionType
from .trados_reader import read_trados_file
from .tbx_writer import TermEntry, Term, TermNote, LangSet, Descrip
from .tbx_writer import write_tbx_file

ORIGINATION = TransactionType.ORIGINATION
MODIFICATION = TransactionType.MODIFICATION


def trados_to_tbx(
        trados_data: Iterable[Concept],
        id_prefix=None,
) -> Iterable[TermEntry]:

    def convert_terms(terms):
        return [Term(term.text,
                     convert_comment(term.descriptions),
                     [note for t in term.transactions
                      for note in convert_transaction(t)])
                for term in terms]

    def convert_comment(descriptions):
        if len(descriptions) > 1:
            return "\n".join(f"{k}: {v}" for k, v in descriptions.items())
        elif not descriptions:
            return None
        else:
            return next(iter(descriptions.values()))

    def convert_transaction(transaction):
        if transaction.type == ORIGINATION:
            return [TermNote("createdBy", transaction.source),
                    TermNote("createdAt", convert_date(transaction.date))]
        else:
            return [TermNote("lastModifiedBy", transaction.source),
                    TermNote("lastModifiedAt", convert_date(transaction.date))]

    def convert_date(date_str):
        d = datetime.fromisoformat(date_str)
        if d.tzinfo is None:
            d = d.replace(tzinfo=timezone.utc)
        return str(int(d.timestamp()) * 1000)

    for concept in trados_data:
        tbx_concept_id = (f"{id_prefix}-{concept.id}"
                          if id_prefix else str(concept.id))
        descrips = [Descrip("conceptId", tbx_concept_id)]
        languages = [LangSet(lang.code, convert_terms(lang.terms))
                     for lang in concept.languages]

        yield TermEntry(descrips, languages)


def main():
    with open(sys.argv[1]) as f:
        trados_data = read_trados_file(f)
        tbx_data = trados_to_tbx(trados_data)
        write_tbx_file(sys.stdout.buffer, tbx_data)


if __name__ == "__main__":
    main()
