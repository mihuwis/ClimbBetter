# Lokalny Auth Emulator

Projekt `demo-climbbetter`, Auth na `127.0.0.1:9099`. Działa bez prawdziwych e-maili i poświadczeń Google; nie służy do logowania Mihu do projektu `climbbetter-dev`.

Z katalogu repozytorium:

```powershell
npm.cmd --prefix infra/firebase install
npm.cmd --prefix infra/firebase start
```

Start zachowuje UID i konta przez import/eksport `.firebase/auth-state`, ignorowany przez Git. Do modeli używamy osobnej bazy `climbbetter_models`, API18081 i frontendu5175. Pełne kroki, dane przykładowe i testy: [Firebase Authentication local](../../.ai/documentation/firebase-authentication-local.md).
