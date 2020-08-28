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
