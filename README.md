# Black Wallet

Wizualny, w pełni offline portfel z personalizowanymi kartami (Kotlin, Jetpack Compose, Material 3).
Bez roota, bez uprawnień, bez internetu. NIE wykonuje płatności NFC – tym nadal zajmuje się Google Wallet.

## Kompilacja (Android Studio)
1. Zainstaluj Android Studio (Ladybug 2024.2 lub nowsze, JDK 17 jest w zestawie).
2. File > Open > wskaż folder `BlackWallet`. Poczekaj na Gradle Sync (pobiera zależności – wymaga internetu).
3. Telefon: Ustawienia > Informacje o telefonie > Informacje o oprogramowaniu > 7x „Numer kompilacji”,
   potem Opcje programisty > Debugowanie USB. Podłącz kabel i zaakceptuj klucz.
4. Wybierz telefon na pasku u góry i kliknij Run (zielony trójkąt) – aplikacja zainstaluje się bezpośrednio.

## Plik APK
Build > Build Bundle(s) / APK(s) > Build APK(s). Wynik: `app/build/outputs/apk/debug/app-debug.apk`
Skopiuj na telefon i zainstaluj (zezwól menedżerowi plików na instalację z nieznanych źródeł).

## Dane
Prywatny katalog aplikacji (`wallet.json` + `images/`). Kopie zapasowe Androida wyłączone.
Brak uprawnień INTERNET/NFC. Nie ma pól na numer karty, CVV, PIN ani datę ważności.

## Wersja release / Google Play (AAB)
1. Wygeneruj klucz (raz) w katalogu projektu:
   `keytool -genkeypair -v -keystore blackwallet-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias blackwallet`
   Zapisz hasła i plik .jks w bezpiecznym miejscu (kopia poza komputerem).
2. Skopiuj `keystore.properties.example` do `keystore.properties` i uzupełnij hasła.
3. Build > Generate Signed Bundle / APK > Android App Bundle (albo `./gradlew bundleRelease`).
   Wynik: `app/build/outputs/bundle/release/app-release.aab`
4. Przed każdą kolejną wersją zwiększ `versionCode` w `app/build.gradle.kts`.
5. W Play Console: utwórz aplikację, wgraj AAB (włącz Play App Signing), uzupełnij opis, ikonę 512x512,
   baner 1024x500, min. 2 zrzuty ekranu, ankietę zawartości, formularz Bezpieczeństwo danych
   (brak zbieranych danych) i link do polityki prywatności (szablon: PRIVACY_POLICY.md – opublikuj ją
   pod publicznym adresem URL, np. GitHub Pages).
6. Uwaga: nowe konta osobiste wymagają zwykle testu zamkniętego (min. 12 testerów przez 14 dni)
   przed publikacją produkcyjną – sprawdź aktualne zasady w Play Console.
7. Zmień nazwę/opis tak, aby nie sugerowały powiązania z Google Wallet ani z bankami.

## APK bez instalowania Android Studio (GitHub Actions)
1. Załóż darmowe konto na github.com i utwórz nowe, prywatne repozytorium.
2. Wgraj do niego wszystkie pliki projektu (Add file > Upload files), razem z folderem `.github`.
   Jeśli folder `.github` się nie wgra: Add file > Create new file, wpisz nazwę `.github/workflows/build.yml`
   i wklej zawartość tego pliku z ZIP-a.
3. Zakładka Actions > „Build APK” > Run workflow.
4. Po ok. 5 min wejdź w zakończone uruchomienie i pobierz artefakt `BlackWallet-debug-apk` (ZIP z app-debug.apk).
