import xml.etree.ElementTree as ET
from xml.etree.ElementTree import Element, SubElement
from dataclasses import dataclass
from typing import Optional, Iterable


@dataclass
class Descrip:
    type: str
    value: str


@dataclass
class TermNote:
    type: str
    value: str


@dataclass
class Term:
    value: str
    note: Optional[str]
    term_notes: list[TermNote]


@dataclass
class LangSet:
    iso_code: str
    terms: list[Term]


@dataclass
class TermEntry:
    descrips: list[Descrip]
    languages: list[LangSet]


def write_tbx_file(stream, terms: Iterable[TermEntry], indent="  ") -> None:
    indent_bytes = indent.encode("utf-8")

    stream.write(b'<?xml version="1.0" encoding="utf-8"?>\n')
    stream.write(b'<martif type="TBX">\n')
    stream.write(indent_bytes)
    stream.write(b'<text>\n')
    stream.write(indent_bytes * 2)
    stream.write(b'<body>\n')

    for entry in terms:
        stream.write(indent_bytes * 3)
        root = Element("termEntry")

        for desc in entry.descrips:
            desc_element = SubElement(root, "descrip", {"type": desc.type})
            desc_element.text = desc.value

        for lang in entry.languages:
            lang_element = SubElement(root, "langSet",
                                      {"xml:lang": lang.iso_code})
            for term in lang.terms:
                tig = SubElement(lang_element, "tig")
                SubElement(tig, "term").text = term.value
                if term.note:
                    SubElement(tig, "note").text = term.note

                for note in term.term_notes:
                    note_element = SubElement(
                        tig, "termNote", {"type": note.type})
                    note_element.text = note.value

        ET.indent(root, space=indent, level=3)
        stream.write(ET.tostring(root))
        stream.write(b'\n')

    stream.write(indent_bytes * 2)
    stream.write(b'</body>\n')
    stream.write(indent_bytes)
    stream.write(b'</text>\n')
    stream.write(b'</martif>\n')
