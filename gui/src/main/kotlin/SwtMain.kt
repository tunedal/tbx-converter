import org.eclipse.swt.widgets.Display

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.*
import org.eclipse.swt.layout.*
import org.eclipse.swt.events.*
import kotlinx.coroutines.*
import java.util.concurrent.Executor

fun main(args: Array<String>) {
    val display = Display()
    val dispatcher = Executor(display::asyncExec).asCoroutineDispatcher()

    val shell = Shell(display)
    shell.text = "Henriks SWT-test i Kotlin"
    shell.setSize(640, 480)

    val layout = RowLayout()
    layout.marginLeft = 50
    layout.marginTop = 50
    shell.layout = layout

    val b = Button(shell, SWT.PUSH)
    b.text = "Quit"
    b.layoutData = RowData(80, 30)
    // Eftersom SelectionListener har två metoder måste vi
    // implementera ett objekt istället för en lambdafunktion.
    b.addSelectionListener(object: SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent) {
            shell.display.dispose()
            System.exit(0)
        }
    })

    val b2 = button(shell, "Ytterligare en knapp") {
        println("Whee!")
    }
    b2.layoutData = RowData(160, 30)

    val b3 = Button(shell, SWT.PUSH)
    b3.text = "Och en till"
    b3.layoutData = RowData(120, 30)
    b3.addClickListener {
        println("Oh yeah, extension methods for the win!")
    }

    val b4 = Button(shell, SWT.PUSH)
    b4.text = "Hmm..."
    b4.layoutData = RowData(80, 30)
    b4.addListener(SWT.Selection) {
        display.asyncExec {
            println("Ha, async!")
        }
        GlobalScope.launch(dispatcher) {
            println("Hello, coroutines!")
            for (i in 1..30) {
                println(i)
                delay(150)
            }
            println("Boom!")
        }
    }

    // val menubar = Menu(shell, SWT.BAR)
    // shell.menuBar = menubar
    // val file = MenuItem(menubar, SWT.CASCADE)
    // file.text = "&File"
    // val submenu = Menu(shell, SWT.DROP_DOWN)
    // file.menu = submenu
    // val item = MenuItem(submenu, SWT.PUSH)
    // item.addListener(SWT.Selection) {
    //     println("Hoho.")
    // }
    // item.text = "Hm"

    shell.menuBar = menubar(shell) {
        menu("Ahaha") {
            (1..7).forEach { num ->
                item("Item $num") { println("Item $num selected.") }
            }
            menu("Min undermeny") {
                (100..300).forEach { num ->
                    item("Item $num") {
                        println("Submenu item $num selected.")
                    }
                }
                item("Kapow") {
                    println("Kapow!!")
                }
            }
            (8..14).forEach { num ->
                item("Item $num") { println("Item $num selected.") }
            }
        }
        menu("Hoho") {
            item("whut") {}
        }
    }

    shell.open()
    while (!shell.isDisposed()) {
        //println("hoho")
        if (!display.readAndDispatch()) display.sleep()
    }
    display.dispose()
}

fun button(parent: Composite, text: String,
           action: (SelectionEvent) -> Unit): Button {
    val b = Button(parent, SWT.PUSH)
    b.text = text
    // Eftersom SelectionListener har två metoder måste vi
    // implementera ett objekt istället för en lambdafunktion.
    b.addSelectionListener(object: SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent) {
            action(e)
        }
    })
    return b
}

fun Button.addClickListener(action: (SelectionEvent) -> Unit) {
    this.addSelectionListener(object: SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent) {
            action(e)
        }
    })
}


// item.text = "Hm"

fun menubar(parent: Shell, init: Menu.() -> Unit): Menu {
    val m = Menu(parent, SWT.BAR)
    m.init()
    return m
}

fun Menu.menu(text: String, init: Menu.() -> Unit): Menu {
    val m = Menu(this)
    val item = MenuItem(this, SWT.CASCADE)
    item.text = text
    item.menu = m
    m.init()
    return m
}

fun Menu.item(text: String, action: (SelectionEvent) -> Unit): MenuItem {
    val item = MenuItem(this, SWT.CASCADE)
    item.text = text
    item.addSelectionListener(object: SelectionAdapter() {
        override fun widgetSelected(e: SelectionEvent) {
            action(e)
        }
    })
    return item
}
