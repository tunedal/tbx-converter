import xml.etree.ElementTree as ET
from unittest import TestCase
from io import BytesIO
from importlib.resources import files

from tbxconverter.trados_reader import read_trados_file
from tbxconverter.trados_reader import Concept, Language
from tbxconverter.trados_reader import Transaction
from tbxconverter.trados_reader import TransactionType
from tbxconverter.trados_reader import Term as TradosTerm
from tbxconverter.tbx_writer import TermEntry, LangSet
from tbxconverter.tbx_writer import TermNote, Descrip
from tbxconverter.tbx_writer import Term as TbxTerm
from tbxconverter.tbx_writer import write_tbx_file
from tbxconverter.converter import trados_to_tbx

ORIGINATION = TransactionType.ORIGINATION
MODIFICATION = TransactionType.MODIFICATION


class ConverterTest(TestCase):
    maxDiff = 5000

    def test_converts_concept_id(self) -> None:
        trados_data = [Concept(12, [], []), Concept(34, [], [])]

        expected = [
            TermEntry([Descrip("conceptId", "12")], []),
            TermEntry([Descrip("conceptId", "34")], []),
        ]

        self.assertEqual(expected, list(trados_to_tbx(trados_data)))

    def test_adds_concept_id_prefix(self) -> None:
        trados_data = [Concept(12, [], []), Concept(34, [], [])]

        expected = [
            TermEntry([Descrip("conceptId", "test-12")], []),
            TermEntry([Descrip("conceptId", "test-34")], []),
        ]

        result = list(trados_to_tbx(trados_data, id_prefix="test"))
        self.assertEqual(expected, result)

    def test_converts_terms(self) -> None:
        trados_data = [Concept(
            1,
            [Language(
                "lang1",
                "code1",
                [TradosTerm("term1-lang1", [], {}),
                 TradosTerm("term2-lang1", [], {})]),
             Language(
                 "lang2",
                 "code2",
                 [TradosTerm("term1-lang2", [], {})])],
            [])]

        expected = [
            TermEntry([Descrip("conceptId", "1")], [
                LangSet("code1", [
                    TbxTerm("term1-lang1", None, []),
                    TbxTerm("term2-lang1", None, []),
                ]),
                LangSet("code2", [
                    TbxTerm("term1-lang2", None, []),
                ])
            ])
        ]

        self.assertEqual(expected, list(trados_to_tbx(trados_data)))

    def test_converts_comments(self) -> None:
        trados_data = [Concept(
            1,
            [Language(
                "lang1",
                "code1",
                [TradosTerm("term1-lang1", [], {
                    "desc1": "comment1",
                    "desc2": "comment2",
                }),
                 TradosTerm("term2-lang1", [], {
                     "desc3": "comment3",
                 }),
                 TradosTerm("term3-lang1", [], {})]),
             Language(
                 "lang2",
                 "code2",
                 [TradosTerm("term1-lang2", [], {
                     "desc4": "comment4",
                 })])],
            [])]

        expected = [
            TermEntry([Descrip("conceptId", "1")], [
                LangSet("code1", [
                    TbxTerm("term1-lang1",
                            "desc1: comment1\ndesc2: comment2",
                            []),
                    TbxTerm("term2-lang1", "comment3", []),
                    TbxTerm("term3-lang1", None, []),
                ]),
                LangSet("code2", [
                    TbxTerm("term1-lang2", "comment4", []),
                ])
            ])
        ]

        self.assertEqual(expected, list(trados_to_tbx(trados_data)))

    def test_converts_term_transactions(self) -> None:
        trados_data = [Concept(
            1,
            [Language(
                "lang1",
                "code1",
                [TradosTerm("term1-lang1", [
                    Transaction(ORIGINATION, "source1",
                                "2000-04-01T00:00:01"),
                    Transaction(MODIFICATION, "source2",
                                "2000-04-01T00:00:02"),
                ], {}),
                 TradosTerm("term2-lang1", [], {})]),
             Language(
                 "lang2",
                 "code2",
                 [TradosTerm("term1-lang2", [
                     Transaction(MODIFICATION, "source3",
                                 "2000-04-01T00:00:03"),
                 ], {})])],
            [])]

        expected = [
            TermEntry([Descrip("conceptId", "1")], [
                LangSet("code1", [
                    TbxTerm("term1-lang1", None, [
                        TermNote("createdBy", "source1"),
                        TermNote("createdAt", "954547201000"),
                        TermNote("lastModifiedBy", "source2"),
                        TermNote("lastModifiedAt", "954547202000"),
                    ]),
                    TbxTerm("term2-lang1", None, []),
                ]),
                LangSet("code2", [
                    TbxTerm("term1-lang2", None, [
                        TermNote("lastModifiedBy", "source3"),
                        TermNote("lastModifiedAt", "954547203000"),
                    ]),
                ])
            ])
        ]

        self.assertEqual(expected, list(trados_to_tbx(trados_data)))


class ExampleFileConverterTest(TestCase):
    trados_xml: bytes
    tbx_xml: bytes

    maxDiff = 10000

    @classmethod
    def setUpClass(self) -> None:
        trados_filename = "data/sdl-trados-example.xml"
        tbx_filename = "data/memsource-example.xml"
        with files(__package__).joinpath(trados_filename).open("rb") as f:
            self.trados_xml = f.read()
        with files(__package__).joinpath(tbx_filename).open("rb") as f:
            self.tbx_xml = f.read()

    def test_converts_example_file(self) -> None:
        input_buf = BytesIO(self.trados_xml)
        output_buf = BytesIO()

        write_tbx_file(
            output_buf,
            trados_to_tbx(
                read_trados_file(input_buf),
                id_prefix="testprefix"))

        self.assert_xml_equal(self.tbx_xml, output_buf.getvalue())

    def assert_xml_equal(self, expected, actual, *, indent=True):
        def normalize(data):
            tree = ET.fromstring(data)
            if indent:
                ET.indent(tree)
            return ET.tostring(tree, encoding="unicode")

        self.assertEqual(normalize(expected), normalize(actual))
