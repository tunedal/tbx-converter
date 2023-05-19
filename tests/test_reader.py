from unittest import TestCase
from importlib.resources import files

from tbxconverter.trados_reader import read_trados_file
from tbxconverter.trados_reader import Concept, Language, Term, Transaction
from tbxconverter.trados_reader import TransactionType

ORIGINATION = TransactionType.ORIGINATION
MODIFICATION = TransactionType.MODIFICATION


class TradosReaderTest(TestCase):
    @classmethod
    def setUpClass(self):
        filename = "data/sdl-trados-example.xml"
        with files(__package__).joinpath(filename).open() as f:
            self.trados_data = list(read_trados_file(f))

    def test_reads_concept_list(self):
        self.assertEqual(3, len(self.trados_data))
        self.assertEqual([1, 60, 3000], [c.id for c in self.trados_data])
        for c in self.trados_data:
            self.assertIsInstance(c, Concept)

    def test_reads_languages(self):
        concepts = {c.id: c for c in self.trados_data}
        for concept_id in [1, 60, 3000]:
            languages = sorted(concepts[concept_id].languages,
                               key=lambda lang: lang.name)
            self.assertEqual(
                [("english", "EN-GB"), ("swedish", "SV-SE")],
                [(lang.name, lang.code) for lang in languages])
            for lang in languages:
                self.assertIsInstance(lang, Language)

    def test_reads_terms(self):
        languages = {(c.id, lang.name): [term.text for term in lang.terms]
                     for c in self.trados_data
                     for lang in c.languages}

        expected = {
            (1, "swedish"): ["Swedish term one",
                             "synonym for Swedish term one"],
            (1, "english"): ["term one in English"],
            (60, "swedish"): ["term 60 in Swedish"],
            (60, "english"): ["term 60 in English"],
            (3000, "swedish"): ["term 3000 in Swedish"],
            (3000, "english"): ["term 3000 in English",
                                "synonym for term 3000 in English"],
        }

        self.assertEqual(expected, languages)

    def test_reads_term_comments(self):
        descriptions = {(c.id, term.text): term.descriptions
                        for c in self.trados_data
                        for lang in c.languages
                        for term in lang.terms}

        expected = {
            (1, "Swedish term one"): {},
            (1, "synonym for Swedish term one"): {},
            (1, "term one in English"): {},
            (60, "term 60 in Swedish"): {},
            (60, "term 60 in English"): {
                "Kommentar": "comment for term 60",
            },
            (3000, "term 3000 in Swedish"): {},
            (3000, "term 3000 in English"): {},
            (3000, "synonym for term 3000 in English"): {},
        }

        self.assertEqual(expected, descriptions)

    def test_reads_concept_transactions(self):
        transactions = {c.id: c.transactions for c in self.trados_data}

        expected = {
            1: [Transaction(ORIGINATION, "super",
                            "2000-09-13T03:25:46"),
                Transaction(MODIFICATION, "super",
                            "2000-09-13T03:25:46")],
            60: [Transaction(ORIGINATION, "super",
                             "2000-09-13T03:25:47"),
                 Transaction(MODIFICATION, "super",
                             "2001-01-24T01:44:12")],
            3000: [Transaction(ORIGINATION, "Example User",
                               "2020-08-22T18:47:42"),
                   Transaction(MODIFICATION, "Example User",
                               "2020-08-22T18:47:42")]
        }

        self.assertEqual(expected, transactions)

    def test_reads_term_transactions(self):
        transactions = {(c.id, term.text): term.transactions
                        for c in self.trados_data
                        for lang in c.languages
                        for term in lang.terms}

        expected = {
            (1, "Swedish term one"): [
                Transaction(ORIGINATION, "local", "2007-01-19T13:04:29"),
                Transaction(MODIFICATION, "local", "2007-01-19T13:04:29"),
            ],
            (1, "synonym for Swedish term one"): [
                Transaction(ORIGINATION, "local", "2007-01-19T13:04:29"),
                Transaction(MODIFICATION, "local", "2007-01-19T13:04:29"),
            ],
            (1, "term one in English"): [
                Transaction(ORIGINATION, "local", "2007-01-19T13:04:29"),
                Transaction(MODIFICATION, "local", "2007-01-19T13:04:29"),
            ],
            (60, "term 60 in Swedish"): [
                Transaction(ORIGINATION, "local", "2007-01-19T13:04:35"),
                Transaction(MODIFICATION, "local", "2007-01-19T13:04:35"),
            ],
            (60, "term 60 in English"): [
                Transaction(ORIGINATION, "local", "2007-01-19T13:04:35"),
                Transaction(MODIFICATION, "local", "2007-01-19T13:04:35"),
            ],
            (3000, "term 3000 in Swedish"): [
                Transaction(ORIGINATION, "Example User",
                            "2020-08-22T18:47:42"),
                Transaction(MODIFICATION, "Example User",
                            "2020-08-22T18:47:42"),
            ],
            (3000, "term 3000 in English"): [
                Transaction(ORIGINATION, "Example User",
                            "2020-08-22T18:47:42"),
                Transaction(MODIFICATION, "Example User",
                            "2020-08-22T18:47:42"),
            ],
            (3000, "synonym for term 3000 in English"): [
                Transaction(ORIGINATION, "Example User",
                            "2020-08-22T18:47:42"),
                Transaction(MODIFICATION, "Example User",
                            "2020-08-22T18:47:42"),
            ],
        }

        self.assertEqual(expected, transactions)

    def test_reads_complete_example_file_structure(self):
        expected = [
            Concept(
                1,
                [
                    Language("swedish", "SV-SE", [
                        Term("Swedish term one", [
                            Transaction(
                                ORIGINATION,
                                "local",
                                "2007-01-19T13:04:29"),
                            Transaction(
                                MODIFICATION,
                                "local",
                                "2007-01-19T13:04:29"),
                        ]),
                        Term("synonym for Swedish term one", [
                            Transaction(
                                ORIGINATION,
                                "local",
                                "2007-01-19T13:04:29"),
                            Transaction(
                                MODIFICATION,
                                "local",
                                "2007-01-19T13:04:29"),
                        ]),
                    ]),
                    Language("english", "EN-GB", [
                        Term("term one in English", [
                            Transaction(
                                ORIGINATION,
                                "local",
                                "2007-01-19T13:04:29"),
                            Transaction(
                                MODIFICATION,
                                "local",
                                "2007-01-19T13:04:29"),
                        ]),
                    ]),
                ],
                [
                    Transaction(
                        ORIGINATION,
                        "super",
                        "2000-09-13T03:25:46"),
                    Transaction(
                        MODIFICATION,
                        "super",
                        "2000-09-13T03:25:46"),
                ]
            ),
            Concept(
                60,
                [
                    Language("swedish", "SV-SE", [
                        Term("term 60 in Swedish", [
                            Transaction(
                                ORIGINATION,
                                "local",
                                "2007-01-19T13:04:35"),
                            Transaction(
                                MODIFICATION,
                                "local",
                                "2007-01-19T13:04:35"),
                        ]),
                    ]),
                    Language("english", "EN-GB", [
                        Term("term 60 in English", [
                            Transaction(
                                ORIGINATION,
                                "local",
                                "2007-01-19T13:04:35"),
                            Transaction(
                                MODIFICATION,
                                "local",
                                "2007-01-19T13:04:35"),
                        ], {
                            "Kommentar": "comment for term 60",
                        }),
                    ]),
                ],
                [
                    Transaction(
                        ORIGINATION,
                        "super",
                        "2000-09-13T03:25:47"),
                    Transaction(
                        MODIFICATION,
                        "super",
                        "2001-01-24T01:44:12"),
                ]
            ),
            Concept(
                3000,
                [
                    Language("english", "EN-GB", [
                        Term("term 3000 in English", [
                            Transaction(
                                ORIGINATION,
                                "Example User",
                                "2020-08-22T18:47:42"),
                            Transaction(
                                MODIFICATION,
                                "Example User",
                                "2020-08-22T18:47:42"),
                        ]),
                        Term("synonym for term 3000 in English", [
                            Transaction(
                                ORIGINATION,
                                "Example User",
                                "2020-08-22T18:47:42"),
                            Transaction(
                                MODIFICATION,
                                "Example User",
                                "2020-08-22T18:47:42"),
                        ]),
                    ]),
                    Language("swedish", "SV-SE", [
                        Term("term 3000 in Swedish", [
                            Transaction(
                                ORIGINATION,
                                "Example User",
                                "2020-08-22T18:47:42"),
                            Transaction(
                                MODIFICATION,
                                "Example User",
                                "2020-08-22T18:47:42"),
                        ]),
                    ]),
                ],
                [
                    Transaction(
                        ORIGINATION,
                        "Example User",
                        "2020-08-22T18:47:42"),
                    Transaction(
                        MODIFICATION,
                        "Example User",
                        "2020-08-22T18:47:42"),
                ]
            ),
        ]

        self.assertEqual(expected, self.trados_data)
