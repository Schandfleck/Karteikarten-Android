# Build und Signierung

## Voraussetzungen

- JDK 17.
- Android SDK: Plattform 37, Build Tools 36.0.0, Platform Tools.
- Gradle 9.6.0 über den enthaltenen Wrapper (Download mit SHA-256-Prüfung).

Android Studio kann das Repository direkt als Gradle-Projekt öffnen. Es erstellt bei Bedarf `local.properties`. Alternativ `ANDROID_HOME` auf die SDK-Installation setzen. Maschinenpfade gehören nicht ins Repository.

Festgelegt sind Android Gradle Plugin 9.4.1, Kotlin/Compose Compiler 2.2.10 und Compose BOM 2026.02.01. Die bestehenden Kompatibilitätsoptionen in gradle.properties sind für diese Konfiguration erforderlich.

## Debug

```sh
./build.sh debug
```

Ergebnis: `APK/Karteikarten-debug.apk`. Das Skript baut und führt Android Lint aus. Alternativ unter Windows `gradlew.bat :app:assembleDebug :app:lintDebug` verwenden; die APK liegt dann unter `app/build/outputs/apk/debug/`.

Debug benötigt keinen Release-Schlüssel. Eine Debug-APK kann eine mit dem privaten Release-Schlüssel signierte App nicht ersetzen. Für Updates auf dem bereits verwendeten Gerät den Release-Build nutzen.

## Release

1. `signing.properties.example` nach `signing.properties` kopieren.
2. Eigenen Schlüssel bereitstellen und die vier Werte lokal eintragen. `storeFile` ist relativ zum Repository-Stamm oder ein absoluter Pfad.
3. `./build.sh release` ausführen.

Ergebnis: `APK/Karteikarten.apk`, nach erfolgreicher Lint- und Signaturprüfung. Ohne vollständige Signierkonfiguration kann Gradle nur eine unsigned Release-APK erzeugen; das Skript gibt diese nicht als installierbare Release-Datei aus.

Der vorhandene Schlüssel bleibt lokal in `signing/karteikarten.jks`; die lokale signing.properties ist bereits dafür eingerichtet. Beide Dateien sind von Git ausgeschlossen. Schlüssel und Zugangsdaten privat sichern: denselben Schlüssel für künftige Updates behalten. Passwörter werden nicht in Build-Dateien oder Dokumentation abgelegt.

## Build-Skript

`build.sh` im Projektstamm bleibt als Einstieg bestehen; die Implementierung liegt unter `scripts/build.sh`. Standard ist Release. Ohne `GRADLE_BIN` verwendet das Skript den Wrapper. Optional kann `GRADLE_BIN` auf eine bereits vorhandene Installation derselben Gradle-Version zeigen. `GRADLE_USER_HOME` bleibt frei konfigurierbar; keine benutzerspezifischen Cache-Pfade sind fest codiert.

## GitHub Actions

`.github/workflows/android.yml` baut Debug und führt Lint auf GitHub aus. APK und Lint-Berichte stehen danach als Workflow-Artefakte bereit. Keine privaten Signierdaten oder Veröffentlichungsrechte nötig. Der eigentliche GitHub-Lauf kann erst nach dem Hochladen geprüft werden.
