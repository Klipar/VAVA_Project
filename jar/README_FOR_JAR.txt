== SPUSTENIE APLIKÁCIE (SERVER + KLIENT) ==

1. Nainštaluj Java 21 (alebo vyššiu, kompatibilnú s JavaFX)
   https://adoptium.net/

2. Stiahni JavaFX SDK 21 (rovnaká verzia ako Java)
   https://gluonhq.com/products/javafx/
   Rozbaľ do priečinka vedľa .jar súborov

3. Spusti SERVER (v prvom termináli):

java --module-path ./javafx-sdk-21/lib --add-modules javafx.controls,javafx.fxml -jar server.jar

4. Spusti KLIENTA (v druhom termináli):

java --module-path ./javafx-sdk-21/lib --add-modules javafx.controls,javafx.fxml -jar client.jar


== POZNÁMKY ==

- Server musí bežať PRED spustením klienta
- Oba .jar súbory musia byť v rovnakom priečinku ako javafx-sdk-21
- Ak sa názov SDK líši (napr. javafx-sdk-21.0.5), uprav cestu

== TESTOVANIE ==

Po spustení klienta by ste mali vidieť prihlasovacie okno aplikácie.