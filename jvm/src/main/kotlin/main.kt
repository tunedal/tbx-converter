import converters.TimestampConverter
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.xml.parsers.SAXParserFactory

fun main() {
    println(hello())

    val xml = "<roten><ett ichi=\"1\">Hej</ett><två>Hopp</två></roten>"
    createParser().use { parser ->
        val events = parser.parse(xml)
        events.forEach {
            println("Got parser event: $it")
        }
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
