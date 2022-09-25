#!/usr/bin/env python3

import sys, subprocess
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
        with TemporaryDirectory() as tempdir:
            tempdir = Path(tempdir)
            run("jar", "xf", str(jarpath.resolve()),
                cwd=tempdir)
            lib_files = [p for p in tempdir.iterdir()
                         if p.suffix.lower() in LIB_SUFFIXES]
            if lib_files:
                print()
                print("Extracting native libraries:", jarpath.name)
                for p in lib_files:
                    print(f"  {p.name}")
                    move(p, depdir)

                run("jar", "cf", f"{jarpath.stem}-pure{jarpath.suffix}",
                    "-C", str(tempdir), ".",
                    cwd=depdir)

                jarpath.unlink()

    main_jar, = list((PROJDIR / "gui/target").glob("tbx-converter*.jar"))
    print()
    print("Main JAR:", main_jar.name)
    copy2(main_jar, depdir / "tbx-converter.jar")


def run(*cmd, cwd=PROJDIR):
    subprocess.run(cmd, check=True, cwd=str(cwd))


if __name__ == "__main__":
    main(sys.argv[1:])
