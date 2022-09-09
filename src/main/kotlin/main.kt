import converters.Converter
import converters.TimestampConverter
import readers.SdlTradosReader
import writers.TbxWriter
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.xml.parsers.SAXParserFactory

fun main(args: Array<String>) {
    val inputFile = File(args[0])
    if (!inputFile.exists())
        throw FileNotFoundException(inputFile.path)
    val outputDir = createDirectory(args[1])
    val batchSize = args.elementAtOrNull(2)?.toInt(10)
    inputFile.useLines(detectEncoding(inputFile)) { inputLines ->
        SdlTradosConverter().use { converter ->
            val outputBatches = converter.convert(
                inputLines.asIterable(),
                batchSize = batchSize)
            outputBatches.forEachIndexed { i, outputLines ->
                val n = "${i + 1}".padStart(3, '0')
                val outputFile = outputDir.resolve("${outputDir.name}-$n.tbx")
                outputFile.writer().use { writer ->
                    for (line in outputLines) {
                        writer.write(line)
                    }
                }
            }
        }
    }
}

fun detectEncoding(file: File): Charset {
    val buf = ByteArray(32)
    file.inputStream().buffered().use {
        var pos = 0
        while (pos < buf.size) {
            val bytesRead = it.read(buf, pos, buf.size - pos)
            if (bytesRead == -1)
                break
            pos += bytesRead
        }
    }
    return if (buf.slice(0..1).toSet() == setOf(0xff.toByte(), 0xfe.toByte()))
        Charsets.UTF_16
    else
        Charsets.UTF_8
}

private fun createDirectory(path: String): File {
    val file = File(path)
    val success = file.mkdir()
    if (!success)
        error("Failed to create directory: $path")
    return file
}

class SdlTradosConverter : AutoCloseable {
    private val xmlParser = createParser()
    private val reader = SdlTradosReader(xmlParser)
    private val converter = Converter(JTimestampConverter())
    private val writer = TbxWriter()

    fun convert(
        inputLines: Iterable<String>,
        commentTags: Set<String> = setOf("Kommentar"),
        idPrefix: String? = null,
        batchSize: Int? = null
    ): Sequence<Sequence<String>> {
        val concepts = reader.read(inputLines.joinToString("\n"))
        val convertedTerms = converter.convert(concepts, commentTags, idPrefix)
        val batches = when (batchSize) {
            null -> sequenceOf(convertedTerms.toList())
            else -> convertedTerms.chunked(batchSize)
        }
        return sequence {
            for (batch in batches) {
                yield(writer.write(batch.asSequence()))
            }
        }
    }

    override fun close() {
        xmlParser.close()
    }
}

class JTimestampConverter : TimestampConverter {
    override fun timestampFromIsoDate(date: String): Long {
        val format = ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC)
        val dateTime = ZonedDateTime.parse(date, format)
        return dateTime.toEpochSecond()
    }
}

class ExecutorServiceXmlParser(
    private val executor: ExecutorService,
    parser: XmlParser,
) : XmlParser by parser, AutoCloseable {
    override fun close() {
        executor.shutdown()
    }
}

fun createParser(): ExecutorServiceXmlParser {
    val executor = Executors.newSingleThreadExecutor()
    val saxFactory = SAXParserFactory.newDefaultInstance()
    val parser = SaxXmlParser(saxFactory, executor)
    return ExecutorServiceXmlParser(executor, parser)
}
