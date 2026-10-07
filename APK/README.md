# Lokale APK-Ausgabe

`./build.sh` legt die signierte Release-Datei `Karteikarten.apk` hier ab.
`./build.sh debug` erzeugt `Karteikarten-debug.apk` für Entwicklung.

Die APK-Dateien werden nicht in Git aufgenommen. Für Downloads können signierte APKs als Assets eines GitHub Release hochgeladen werden. Debug-APKs sind anders signiert und können eine bereits installierte Release-App nicht ersetzen.
