from dataclasses import dataclass, replace
from threading import Thread
from pathlib import Path

from .converter import convert_file


@dataclass(frozen=True)
class State:
    load_enabled: bool
    export_enabled: bool
    status: str


class ConverterWindowModel:
    def __init__(self, service):
        self._service = service
        self._state_callbacks = []
        self._alert_callbacks = []
        self._input_path = None
        self._output_path = None
        self._state = State(
            load_enabled=True,
            export_enabled=False,
            status="Välj en fil att konvertera.")

    def add_state_callback(self, callback):
        self._state_callbacks.append(callback)
        callback(self._state)

    def add_alert_callback(self, callback):
        self._alert_callbacks.append(callback)

    def load_file(self, filename: str):
        self._input_path = Path(filename)
        self._update_state(
            export_enabled=True,
            status=f"Öppnad: {self._input_path.name}")

    def convert(self, output_filename: str):
        self._output_path = Path(output_filename)
        self._update_state(
            load_enabled=False,
            export_enabled=False,
            status="Exporterar...")
        self._service.convert(
            self._input_path,
            self._output_path,
            self._on_finish)

    def _on_finish(self, count, error):
        path = self._output_path
        self._output_path = None

        print((count, error))

        if error is None and count > 0:
            self._update_state(
                load_enabled=True,
                export_enabled=False,
                status=f"Exporterade {count} termer till {path.name}.")
        else:
            if error:
                self._alert(f"Fel vid export:\n{error!r}")
            else:
                self._alert("Inga termer fanns att exportera.")

            self._update_state(
                load_enabled=True,
                export_enabled=True,
                status="Export misslyckades.")

    def _update_state(self, **updates):
        state = self._state = replace(self._state, **updates)
        for callback in self._state_callbacks:
            callback(state)

    def _alert(self, message):
        for callback in self._alert_callbacks:
            callback(message)


class ConverterService:
    def __init__(self, dispatcher):
        self._dispatcher = dispatcher

    def convert(self, input_path, output_path, callback):
        args = input_path, output_path, callback
        Thread(target=self._work, args=args).start()

    def _work(self, input_path, output_path, callback):
        try:
            count = convert_file(input_path, output_path)
        except Exception as ex:
            count = 0
            error = ex
        else:
            error = None

        self._dispatcher.submit(callback, count, error)
