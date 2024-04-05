import sys
from dataclasses import dataclass, field
from enum import Enum
from typing import Iterable
import xml.etree.ElementTree as ET


class TransactionType(Enum):
    ORIGINATION = "origination"
    MODIFICATION = "modification"


@dataclass(frozen=True)
class Transaction:
    type: TransactionType
    source: str
    date: str


@dataclass(frozen=True)
class Term:
    text: str
    transactions: list[Transaction]
    descriptions: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class Language:
    name: str
    code: str
    terms: list[Term]


@dataclass(frozen=True)
class Concept:
    id: int
    languages: list[Language]
    transactions: list[Transaction]


def read_trados_file(stream) -> Iterable[Concept]:
    def read_transactions(element):
        transactions = []

        for trans_grp in element.iterfind("transacGrp"):
            trans = trans_grp.find("transac")
            trans_date = trans_grp.find("date")

            if trans is None or not trans.text:
                continue
            if trans_date is None or not trans_date.text:
                continue

            transactions.append(Transaction(
                TransactionType(trans.attrib["type"]),
                trans.text,
                trans_date.text))

        return transactions

    doc_iterator = ET.iterparse(stream, events=("start", "end"))

    root = next(doc_iterator)[1]
    if root.tag != "mtf":
        raise ValueError(f"Unexpected root element {root.tag!r}")

    for event, iter_element in doc_iterator:
        if event != "end" or iter_element.tag != "conceptGrp":
            continue

        concept_grp = iter_element

        concept = concept_grp.find("concept")
        if concept is None or not concept.text:
            continue

        languages = []
        for lang_grp in concept_grp.iterfind("languageGrp"):
            lang = lang_grp.find("language")
            if lang is None:
                continue

            terms = []
            for term_grp in lang_grp.iterfind("termGrp"):
                term = term_grp.find("term")
                if term is None or not term.text:
                    continue

                descriptions = term_grp.iterfind("descripGrp/descrip")
                terms.append(Term(
                    term.text,
                    read_transactions(term_grp),
                    {d.attrib["type"]: d.text
                     for d in descriptions
                     if d.text}))

            languages.append(Language(
                lang.attrib["type"],
                lang.attrib["lang"],
                terms))

        yield Concept(int(concept.text), languages,
                      read_transactions(concept_grp))

        root.clear()


def main():
    with open(sys.argv[1], "rb") as f:
        for c in read_trados_file(f):
            print(c)


if __name__ == "__main__":
    main()
