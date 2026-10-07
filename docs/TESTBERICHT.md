# Testbericht

## Aktuelle Version 1.2.0

Gerätetest auf einem Pixel 7 Pro mit Android 17. Die Ergebnisse beziehen sich auf dieses Testgerät; andere Geräte und große Kartensammlungen wurden nicht vollständig geprüft.

| Prüfung | Ergebnis |
|---|---|
| Alle Karten ohne Datumsfilter bei App-Start verfügbar | Erfolgreich |
| Gut, Einfach, Schwer und Nochmal speichern | Erfolgreich mit eigens angelegter Testkarte |
| Neue Lernrunde sofort nach jeder Bewertung starten | Erfolgreich |
| Keine Fälligkeits- oder Tagesangaben in Lernansicht und Kartenliste | Erfolgreich |
| Schwere/einfache Karten filtern | Erfolgreich |
| Temporäre Testdaten entfernen und vorhandene Daten erhalten | Erfolgreich |
| Release-Build und Signaturprüfung | Erfolgreich |
| Android Lint | Keine Fehler, 7 Hinweise |

Historische Termin- und Intervallfelder werden für Datenbank-Kompatibilität beibehalten, beeinflussen aber keine Lernrunde.

## Frühere Funktionsprüfungen

Die folgenden Funktionen wurden während der Entwicklung der Versionen 1.0.1 bis 1.1.3 auf demselben Gerät geprüft; diese Ergebnisse ersetzen keine vollständige erneute Prüfung in jeder Version.

- Stapel und Karten anlegen, bearbeiten und löschen.
- Lernzustand bei Bildschirmdrehung erhalten.
- Ansichtswechsel über das Menü in Hoch- und Querformat.
- Android-Zurück, Zurück-Wischgeste und Zurück-Pfeil aus den Einstellungen.
- Einheitliche Bewertungsfarben in Hell und Dunkel, anhand von Screenshots geprüft.
- Deutsch/Englisch sowie System/Hell/Dunkel umschalten.
- Datenbank exportieren und wieder importieren.
- Fehlerhaften Import nach bereits erfolgten Schreiboperationen vollständig zurückrollen.

Die technische Build-Prüfung der Repository-Vorbereitung ist in [REPOSITORY.md](REPOSITORY.md) dokumentiert.
