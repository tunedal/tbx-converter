#!/usr/bin/env python3

import sys, subprocess, platform
from pathlib import Path
from tempfile import TemporaryDirectory
from shutil import move, copy2


LIB_SUFFIXES = {".so", ".dll"}

PROJDIR = Path(__file__).parent.resolve()


def main(args):
    run("mvn", "clean", "install")

    run("mvn", "dependency:copy-dependencies", "-DincludeScope=runtime",
        cwd=PROJDIR / "gui")

    depdir = PROJDIR / "gui/target/dependency"

    for jarpath in depdir.glob("org.eclipse.swt*.jar"):
       extract_native_libs(jarpath, depdir)

    main_jar, = list((PROJDIR / "gui/target").glob("tbx-converter*.jar"))
    print()
    print("Main JAR:", main_jar.name)
    copy2(main_jar, depdir / "tbx-converter.jar")

    if platform.system() == "Windows":
        package(depdir)


def extract_native_libs(jarpath, target_dir):
    with TemporaryDirectory() as tempdir:
        tempdir = Path(tempdir)
        run("jar", "xf", str(jarpath.resolve()),
            cwd=tempdir)
        lib_files = [p for p in tempdir.iterdir()
                     if p.suffix.lower() in LIB_SUFFIXES]
        if not lib_files:
            return

        print()
        print("Extracting native libraries:", jarpath.name)
        for p in lib_files:
            print(f"  {p.name}")
            move(p, target_dir)

        run("jar", "cf", f"{jarpath.stem}-pure{jarpath.suffix}",
            "-C", str(tempdir), ".",
            cwd=target_dir)

        jarpath.unlink()


def package(directory):
    run("jpackage", "-i", str(directory.resolve()),
        "--type", "msi",
        "--name", "TBX Converter",
        "--vendor", "Henrik Tunedal",
        "--app-version", "1.0.0",
        "--main-class", "SwtMainKt",
        "--main-jar", "tbx-converter.jar",
        "--win-upgrade-uuid", "56cee060-8736-42e4-8c76-5c23fb86e2c9",
        "--win-per-user-install",
        "--win-menu",
        "--win-menu-group", "",
        "--add-modules", ",".join(["java.base", "java.xml"]))


def run(*cmd, cwd=PROJDIR):
    subprocess.run(cmd, check=True, cwd=str(cwd))


if __name__ == "__main__":
    main(sys.argv[1:])