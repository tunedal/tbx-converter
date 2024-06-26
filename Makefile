WINTER_PATH = ../winter
WINTER_VERSION = 574608938f9475933bcb739a320896bc459e92fb

build: check build/winter-clone
	git -c advice.detachedHead=false \
	  -C build/winter-clone \
	  reset --hard $(WINTER_VERSION)

	PYTHONPATH=build/winter-clone/src python3 \
	  -m winter.packaging build \
	  --outdir=build \
	  --entrypoint=tbxconverter.gui:main \
	  --name="TBX Converter" \
	  --author="Henrik Tunedal" \
	  --version="1.0.0" \
	  --guid="91eb01d3-4f2c-485e-8e69-c4c8c4a566df" \
	  .//tbxconverter

run: build
	WINEDEBUG=-all wine build/runtime/python.exe \
		-X dev \
		build/app.pyzw \
		tests/data/sdl-trados-example.xml

check:
	scripts/check.sh

build/winter-clone:
	git clone --branch=main $(WINTER_PATH) build/winter-clone

clean:
	rm -rf build

.PHONY: build run check clean
