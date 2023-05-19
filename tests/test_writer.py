import re
import xml.etree.ElementTree as ET
from unittest import TestCase
from io import BytesIO

from tbxconverter.tbx_writer import TermEntry, Term, TermNote
from tbxconverter.tbx_writer import LangSet, Descrip
from tbxconverter.tbx_writer import write_tbx_file


class TbxWriterTest(TestCase):
    maxDiff = 5000

    def test_writes_martif_document_structure(self):
        buf = BytesIO()
        terms = []
        write_tbx_file(buf, terms)

        expected_decl = b'<?xml version="1.0" encoding="utf-8"?>'

        expected = b"""
        <martif type="TBX">
        <text><body></body></text>
        </martif>
        """.strip()

        self.assert_xml_equal(expected, buf.getvalue(), strip=True)
        self.assertTrue(buf.getvalue().startswith(expected_decl))

    def test_writes_terms(self):
        buf = BytesIO()
        terms = [
            TermEntry(
                [Descrip("concept1", "desc1")],
                [
                    LangSet("code1", [
                        Term("term1", "comment1", [
                            TermNote("ntype1", "value1")
                        ]),
                        Term("term2", None, [
                            TermNote("ntype1", "value1"),
                            TermNote("ntype2", "value2")
                        ]),
                    ]),
                    LangSet("code2", [
                        Term("term1", None, []),
                        Term("term2", None, []),
                    ])
                ],
            ),
        ]

        write_tbx_file(buf, terms)

        expected = b"""
        <martif type="TBX"><text><body>
        <termEntry>
          <descrip type="concept1">desc1</descrip>
          <langSet xml:lang="code1">
            <tig>
              <term>term1</term>
              <note>comment1</note>
              <termNote type="ntype1">value1</termNote>
            </tig>
            <tig>
              <term>term2</term>
              <termNote type="ntype1">value1</termNote>
              <termNote type="ntype2">value2</termNote>
            </tig>
          </langSet>
          <langSet xml:lang="code2">
            <tig>
              <term>term1</term>
            </tig>
            <tig>
              <term>term2</term>
            </tig>
          </langSet>
        </termEntry>
        </body></text></martif>
        """.strip()

        self.assert_xml_equal(expected, buf.getvalue())

    def test_indents_output_with_two_spaces(self):
        buf = BytesIO()
        terms = [
            TermEntry(
                [Descrip("concept1", "desc1")],
                [
                    LangSet("code1", [
                        Term("term1", "comment1", [
                            TermNote("ntype1", "value1")
                        ]),
                        Term("term2", None, [
                            TermNote("ntype1", "value1"),
                            TermNote("ntype2", "value2")
                        ]),
                    ]),
                    LangSet("code2", [
                        Term("term1", None, []),
                        Term("term2", None, []),
                    ])
                ],
            ),
            TermEntry(
                [Descrip("concept2", "desc2")], []
            ),
        ]

        start_pat = re.compile(r"<[^/?]")
        end_pat = re.compile(r"</")
        indent_step = 2
        indent_char = " "

        write_tbx_file(buf, terms)

        expected_indent = 0
        for line in buf.getvalue().decode("utf-8").splitlines():
            is_start = start_pat.search(line)
            is_end = end_pat.search(line)
            if is_end and not is_start:
                expected_indent -= indent_step

            self.assertLessEqual(len(start_pat.findall(line)), 1,
                                 "one start tag per line")
            self.assertLessEqual(len(end_pat.findall(line)), 1,
                                 "one end tag per line")
            self.assertLess(is_start.start() if is_start else -1,
                            is_end.start() if is_end else 2**32,
                            f"start should precede end: {line!r}")

            actual_indent = len(line) - len(line.lstrip(indent_char))
            self.assertEqual(expected_indent, actual_indent,
                             f"indent size for line: {line!r}")
            self.assertTrue(c == indent_char for c in line[:actual_indent])

            if is_start and not is_end:
                expected_indent += indent_step

    def assert_xml_equal(self, expected, actual, *, indent=True, strip=False):
        def normalize(data):
            data = ET.canonicalize(data, strip_text=strip)
            tree = ET.fromstring(data)
            if indent:
                ET.indent(tree)
            return ET.tostring(tree, encoding="unicode")

        self.assertEqual(normalize(expected), normalize(actual))
