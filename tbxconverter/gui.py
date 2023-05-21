#!python3

import sys

from winter.constants import VK, WM, MB, SS, OFN, COLOR  # type: ignore
from winter.wrappers import App, pump_messages        # type: ignore
from winter.layout import GridLayout                  # type: ignore
from winter.widgets import Toplevel, Button, Label    # type: ignore
from winter.dispatcher import Dispatcher              # type: ignore

from .gui_model import ConverterWindowModel, ConverterService


def main():
    app = App()
    app.set_thread_dpi_mode(app.get_supported_dpi_mode())

    model = ConverterWindowModel(ConverterService(Dispatcher(app)))

    window = build_ui(app, model)
    window.show()
    window.update()

    sys.exit(pump_messages())


def build_ui(app, model: ConverterWindowModel):
    def on_destroy(event):
        app.post_quit_message(0)

    def on_open_button_clicked(event):
        filename = toplevel.window.show_open_file_dialog(
            flags=OFN.FILEMUSTEXIST | OFN.PATHMUSTEXIST | OFN.HIDEREADONLY,
            filters=[("XML", "*.xml"), ("All Files", "*.*")])

        if filename:
            model.load_file(filename)

    def on_export_button_clicked(event):
        filename = toplevel.window.show_save_file_dialog(
            flags=OFN.OVERWRITEPROMPT,
            filters=[("TBX", "*.tbx"), ("All Files", "*.*")],
            extension="tbx")

        if filename:
            model.convert(filename)

    def on_escape_pressed(event):
        toplevel.window.destroy()

    def on_model_state_changed(state):
        open_button.enable(state.load_enabled)
        export_button.enable(state.export_enabled)
        label.set_text(state.status)
        print(state)

    def on_model_alert(message):
        toplevel.window.show_messagebox(
            text=message, caption="Fel",
            flags=MB.OK | MB.ICONERROR)

    handlers = {
        WM.DESTROY: on_destroy,
        WM.CTLCOLORSTATIC: lambda e: COLOR.WINDOW + 1,
    }

    key_handlers = {
        VK.ESCAPE: on_escape_pressed,
    }

    toplevel = Toplevel(app, "TBX Converter", handlers, key_handlers)

    open_button = Button(toplevel, caption="Öppna...",
                         command=on_open_button_clicked)
    export_button = Button(toplevel, caption="Exportera...",
                           command=on_export_button_clicked)
    label = Label(toplevel, caption="",
                  style=SS.CENTER | SS.CENTERIMAGE)

    grid = GridLayout(cell_size=(160, 32), spacing=10, padding=15)
    grid.add(open_button, sticky="ew")
    grid.add(export_button, sticky="ew")
    grid.break_row()
    grid.add(label, colspan=2, sticky="ew")
    toplevel.set_layout(grid)
    toplevel.set_size()

    model.add_state_callback(on_model_state_changed)
    model.add_alert_callback(on_model_alert)

    return toplevel.window


if __name__ == "__main__":
    main()
