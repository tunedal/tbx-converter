import java.util.concurrent.Executors
import javax.xml.parsers.SAXParserFactory

fun main() {
    println(hello())

    val xml = "<roten><ett ichi=\"1\">Hej</ett><två>Hopp</två></roten>"

    val saxFactory = SAXParserFactory.newDefaultInstance()
    val executor = Executors.newSingleThreadExecutor()
    val parser: XmlParser = SaxXmlParser(saxFactory, executor)
    val events = try {
        parser.parse(xml)
    } finally {
        executor.shutdown()
    }

    events.forEach {
        println("Got parser event: $it")
    }
}
