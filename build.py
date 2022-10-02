#!/usr/bin/env python3

import sys, re, subprocess, platform
from pathlib import Path
from tempfile import TemporaryDirectory
from shutil import move, copy2


LIB_SUFFIXES = {".so", ".dll"}

PROJDIR = Path(__file__).parent.resolve()


def main(args):
    # XXX
    print("args:", repr(args))
    #set_output("MSI_PACKAGE_FILENAME", __file__)
    #set_output("VERSION", "1.2.3")
    #return



    mvn("clean", "install")

    mvn("dependency:copy-dependencies", "-DincludeScope=runtime",
        cwd=PROJDIR / "gui")

    depdir = PROJDIR / "gui/target/dependency"

    for jarpath in depdir.glob("org.eclipse.swt*.jar"):
       extract_native_libs(jarpath, depdir)

    main_jar, = list((PROJDIR / "gui/target").glob("tbx-converter*.jar"))
    print()
    print("Main JAR:", main_jar.name)
    copy2(main_jar, depdir / "tbx-converter.jar")

    if platform.system() == "Windows":
        if args:
            (tag,) = args
        else:
            tag = ""

        m = re.match(r"^v(\d+\.\d+\.\d+)$", tag)
        if m:
            version = m.group(1)
        else:
            version = "0.0.0"

        package = dummy_package  # XXX

        print("Packaging version:", version)
        package(depdir, version)


def dummy_package(depdir, version):
    print("Dummy packaging!")
    print("Depdir:", repr(depdir))
    print("Version:", repr(version))
    set_output("MSI_PACKAGE_FILENAME", str(depdir / "tbx-converter.jar"))
    set_output("VERSION", version)


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

        signature_files = [p for p in (tempdir / "META-INF").iterdir()
                           if p.suffix.lower() in [".rsa", ".sf"]]
        for p in signature_files:
            p.unlink()

        run("jar", "cf", f"{jarpath.stem}-pure{jarpath.suffix}",
            "-C", str(tempdir), ".",
            cwd=target_dir)

        jarpath.unlink()


def package(directory, version):
    print()
    print("Creating MSI package...")

    run("jpackage", "-i", str(directory.resolve()),
        "--type", "msi",
        "--name", "TBX Converter",
        "--vendor", "Henrik Tunedal",
        "--app-version", version,
        "--main-class", "SwtMainKt",
        "--main-jar", "tbx-converter.jar",
        "--win-upgrade-uuid", "56cee060-8736-42e4-8c76-5c23fb86e2c9",
        "--win-per-user-install",
        "--win-menu",
        "--win-menu-group", "",
        "--add-modules", ",".join(["java.base", "java.xml"]))

    filename, = list(Path(".").glob("*.msi"))
    set_output("MSI_PACKAGE_FILENAME", filename)
    set_output("VERSION", version)


def set_output(key, value):
    # GitHub Actions in-band signaling.
    print("::set-output", f"name={key}::{value}")


def mvn(*cmd, cwd=PROJDIR):
    cmd = ["mvn", "--batch-mode", "--update-snapshots"] + list(cmd)
    if platform.system() == "Windows":
        cmd = ["cmd", "/c"] + cmd
    run(*cmd, cwd=cwd)


def run(*cmd, cwd=PROJDIR):
    subprocess.run(cmd, check=True, cwd=str(cwd))


if __name__ == "__main__":
    main(sys.argv[1:])
