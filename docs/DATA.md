# Daten übertragen und sichern

Alle Stapel und Karten liegen lokal in einer SQLite-Datenbank. Im Menü oben rechts kann diese exportiert oder importiert werden. Ein Import ersetzt vorhandene Stapel und Karten; vorher bei Bedarf exportieren.

Für Daten aus der ursprünglichen Desktop-App:

1. Desktop-App schließen.
2. Ihre `karten.db` auf das Handy kopieren. Unter Linux liegt sie normalerweise in `$XDG_DATA_HOME/karteikarten/` beziehungsweise `~/.local/share/karteikarten/`.
3. Im Menü Datenbank importieren wählen und die Datei auswählen.

Fehlerhafte Importe werden zurückgerollt. Ältere Datenbanken ohne Bewertungsspalte werden unterstützt. Historische Termin- und Intervallfelder bleiben aus Kompatibilitätsgründen in der Datei, werden aber nicht zum Lernen oder Bewerten verwendet.

Exportierte Dateien können bei geschlossener Desktop-App als deren Datenbank verwendet werden; die bisherige Datei vorher sichern. Sprache und Farben bleiben gerätespezifisch. Private Datenbanken gehören nicht ins Git-Repository.
