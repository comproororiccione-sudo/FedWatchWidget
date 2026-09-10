# FedWatch Free Widget v1.1

Widget Android gratuito per seguire le probabilità della prossima riunione FOMC.

## Strategia dati
1. **CME pubblico (priorità):** prova a leggere la tabella pubblica FedWatch/QuikStrike usando la pagina CME come referrer. Se riesce, il widget mostra `DATO CME PUBBLICO` e i valori della tabella, ad es. 30,2% / 69,8%.
2. **Fallback gratuito:** se QuikStrike blocca la richiesta o cambia HTML, calcola una stima dai futures 30-Day Fed Funds (ZQ) e la marca esplicitamente `⚠ STIMA FALLBACK`.

Non usa la FedWatch API a pagamento e non contiene API key.

## Aggiornamento
- Automatico ogni 30 minuti tramite WorkManager.
- Manuale con il pulsante ↻.
- Ultimo dato valido conservato in cache.

## Nota importante
CME può modificare in qualsiasi momento controlli anti-bot, referrer o struttura HTML. Per questo la lettura gratuita diretta non può avere la stessa garanzia di stabilità dell'API ufficiale a pagamento. Il widget non presenta mai il fallback come dato ufficiale.

## Build
Aprire la cartella in Android Studio (JDK 17), lasciare completare la sincronizzazione Gradle, quindi Build > Build APK(s).
