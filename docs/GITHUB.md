# GitHub-Repository

Das Projekt verwendet den Hauptzweig `main`. Quellcode, Gradle Wrapper, Dokumentation und Workflow gehören in Git; APKs, Caches, private Datenbanken, lokale SDK-Pfade und Signierschlüssel werden ausgeschlossen.

## Änderungen hochladen

Das Remote `origin` verweist nach Einrichtung auf das GitHub-Repository. Änderungen vor jedem Upload prüfen:

```sh
git status
git diff
git add <GEPRUEFTE-DATEIEN>
git diff --cached
git commit -m "Beschreibung der Änderung"
git push origin main
```

Eine GitHub-Noreply-Adresse als Commit-E-Mail verwenden, wenn keine persönliche E-Mail veröffentlicht werden soll. Zugangsdaten gehören nicht in Remote-URLs oder Git-Dateien.

## Releases

Mit dem vorhandenen privaten Schlüssel `./build.sh release` ausführen. Für eine App-Version können ein Git-Tag und ein GitHub Release erstellt und `APK/Karteikarten.apk` als Asset hochgeladen werden. Signierte APKs sind keine Git-Dateien. Der CI-Workflow stellt nur Debug-APKs für Entwicklung bereit, keine Updates für eine bereits installierte Release-App.

Für den eigenen App-Quellcode wurde noch keine Projektlizenz festgelegt. Drittanbieter-Lizenzen sind in NOTICE.md dokumentiert.
