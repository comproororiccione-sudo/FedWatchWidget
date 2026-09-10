# Generare l’APK

## GitHub Actions (senza Android Studio)
1. Crea un repository GitHub vuoto.
2. Carica tutto il contenuto di questa cartella nella root del repository.
3. Apri la scheda **Actions** e seleziona **Build Android APK**.
4. Premi **Run workflow**.
5. A build completata, scarica l’artifact **FedWatchFreeWidget-v1.1-debug**.
6. Dentro lo ZIP dell’artifact trovi `app-debug.apk`, già firmato con la chiave debug e installabile su Android.

## Android Studio
Apri la cartella del progetto e usa **Build > Build APK(s)**.
Il file verrà creato in `app/build/outputs/apk/debug/app-debug.apk`.
