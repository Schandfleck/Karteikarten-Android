# Repository-Vorbereitung

Stand: 7. Oktober 2026. App-Version 1.2.0.

## Struktur und Git

- Git-Repository mit Hauptzweig main vorbereitet; nur geprüfte Projektdateien für den ersten Commit aufgenommen.
- App und Gradle Wrapper im üblichen Android-Projektlayout beibehalten.
- Dokumentation nach docs verschoben und portable Build-Skripte unter scripts eingeordnet.
- README, Änderungsverzeichnis, Formatierungsregeln und GitHub-Build-Workflow ergänzt.
- Leere, ungenutzte tests/tools-Verzeichnisse entfernt.
- Die nachstehenden Prüfungen wurden vor dem ersten GitHub-Upload durchgeführt. Hinweise für weitere Uploads: GITHUB.md.

## Private Dateien

Signierschlüssel, echte signing.properties, SDK-Konfiguration, APKs, Caches und SQLite-Datenbanken werden von Git ausgeschlossen. Die bisherige Release-Signatur bleibt erhalten. Es gibt keine festen Benutzerpfade und keine echten Signierpasswörter in den geprüften Repository-Dateien.

## Prüfung

- Release-Build mit ausgelagerter lokaler Signierkonfiguration erfolgreich.
- Signaturprüfung erfolgreich; Zertifikat entspricht der bisherigen APK.
- Debug-Build aus einem Export ausschließlich der Git-Dateien erfolgreich: kein Signierschlüssel, keine signing.properties, keine local.properties.
- Der saubere Build verwendet den Gradle Wrapper; Download mit konfigurierter SHA-256-Prüfsumme erfolgreich.
- Android Lint in Release und Debug ohne Fehler, jeweils 7 Hinweise.
- Build-Skripte syntaktisch geprüft; fehlende Release-Konfiguration führt zu einem verständlichen Fehler.
- Workflow-YAML und Git-Inhalt geprüft. Der eigentliche GitHub-Actions-Lauf steht nach dem ersten Hochladen aus.

Die eigene Projektlizenz wurde nicht vorweg festgelegt. Der Apache-2.0-Lizenztext für Drittanbieter ist enthalten.
