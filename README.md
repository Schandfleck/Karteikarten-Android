# Karteikarten für Android

Eine lokale Karteikarten-App mit Googles offiziellen Jetpack-Compose-Material-3-Komponenten. Du entscheidest, wann und wie oft du lernst: alle Karten sind jederzeit verfügbar, ohne Lerntermine oder Wartezeiten.

**Version 1.2.0 · Android 8.0 oder neuer · Deutsch und Englisch**

## Funktionen

- Stapel und Karten erstellen, bearbeiten und löschen.
- Antworten einblenden und mit Nochmal, Schwer, Gut oder Einfach bewerten.
- Bewertungen zum Filtern und Sortieren verwenden; weitere Lernrunden selbst starten.
- Lernen, Karten und Einstellungen über das Menü oben rechts öffnen.
- Systemdesign, Hell/Dunkel, dynamische Hintergrundbildfarben und eigene Farben.
- SQLite-Datenbank importieren und exportieren; Karten bleiben auf dem Gerät.
- Keine Internetberechtigung und keine automatische Lernplanung.

## Installation

Die lokal gebaute, signierte APK liegt in `APK/Karteikarten.apk`. Zum Installieren auf das Android-Gerät kopieren und öffnen. Updates mit derselben Paketkennung und demselben Signierschlüssel erhalten vorhandene Karten.

## Projekt bauen

Voraussetzungen: Java 17, Android SDK Plattform 37 und Build Tools 36.0.0. SDK-Pfad mit `ANDROID_HOME` oder einer lokalen `local.properties` angeben. Der Gradle Wrapper ist enthalten.

```sh
./build.sh debug
```

Für eine signierte Release-APK:

```sh
# signing.properties.example lokal kopieren und ausfüllen.
./build.sh release
```

Details zur Einrichtung und Signierung: [Build-Anleitung](docs/BUILDING.md).

## Ordnerstruktur

```text
app/                  Android-App und Ressourcen
gradle/wrapper/       Gradle Wrapper
scripts/              Portable Build-Skripte
docs/                 Build, Datenübertragung und Testberichte
.github/workflows/    Automatischer Android-Build und Lint
APK/                  Lokale Build-Ausgabe (APK-Dateien ignoriert)
signing/              Privater Signierschlüssel (ignoriert)
```

## Dokumentation

- [Build und Signierung](docs/BUILDING.md)
- [Daten übertragen und sichern](docs/DATA.md)
- [Repository-Vorbereitung und Prüfungen](docs/REPOSITORY.md)
- [Änderungen](CHANGELOG.md)
- [Drittanbieter-Hinweise](NOTICE.md)
