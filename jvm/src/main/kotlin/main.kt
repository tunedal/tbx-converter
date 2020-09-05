import converters.Converter
import converters.TimestampConverter
import readers.SdlTradosReader
import writers.TbxWriter
import java.io.File
import java.io.FileNotFoundException
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
    inputFile.useLines { inputLines ->
        SdlTradosConverter().use { converter ->
            val outputLines = converter.convert(inputLines.asIterable())
            val outputFile = outputDir.resolve(outputDir.name + ".tbx")
            outputFile.writer().use { writer ->
                for (line in outputLines) {
                    writer.write(line)
                }
            }
        }
    }
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
    ): Sequence<String> {
        val concepts = reader.read(inputLines.joinToString("\n"))
        val convertedTerms = converter.convert(concepts, commentTags, idPrefix)
        return writer.write(convertedTerms)
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
