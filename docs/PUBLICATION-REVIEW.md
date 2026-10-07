# Prüfung des Veröffentlichungsumfangs

Vor dem ersten GitHub-Upload werden ausschließlich die vorgesehenen Git-Dateien berücksichtigt.

- Keine echten Signierpasswörter, privaten Schlüssel, Tokens oder Zugangsdaten in den Git-Dateien gefunden.
- signing.properties, signing/, lokale SDK-Konfiguration, Build-Caches, APKs und private SQLite-Dateien sind ausgeschlossen.
- Keine Geräteseriennummern, lokalen Benutzerpfade oder privaten Kontaktadressen enthalten.
- Der öffentliche Testbericht enthält ausschließlich technische Ergebnisse und keine Karteninhalte oder persönlichen Einstellungen.
- Die einzige Binärdatei ist der Gradle Wrapper; seine SHA-256-Prüfsumme stimmt mit der offiziellen Gradle-9.6.0-Prüfsumme überein.
- signing.properties.example enthält ausschließlich Platzhalter.
- Keine bestehende Commit-Historie oder Remote-Verbindung aus einem anderen Projekt übernommen.

Die Paketkennung und Quellcode-Namensräume sind technische Bestandteile der App. Der vorhandene lokale Release-Schlüssel bleibt privat und wird nicht hochgeladen.

Automatische Mustersuche, Ausschlussregeln und Inhaltsprüfung verringern das Risiko einer unbeabsichtigten Veröffentlichung; sie ersetzen keine Geheimnisverwaltung für zukünftige Änderungen.
