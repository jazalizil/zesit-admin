# Zesit Admin — app iOS/Android (Kotlin Multiplatform)

App mobile **Kotlin Multiplatform + Compose Multiplatform** qui remplace l'app
ntfy officielle sur les devices des admins : elle **s'abonne à un topic** du
serveur ntfy self-hosté du VPS (`https://ntfy.$ZESIT_DOMAIN`, voir
`../ntfy/README.md`), **liste les notifications** reçues (historique local) et
les reçoit **en temps réel** (WebSocket) ou **en push natif iOS via APNs**.

Dévs orientés **iOS d'abord** : la cible iOS (device + simulateur) est complète,
Android est fourni (WebSocket/poll ; pas de push FCM pour l'instant).

## Architecture

```
zesit-admin/
├── shared/          module KMP : client ntfy (Ktor), modèles, persistance, pont APNs
│   └── commonMain   NtfyApi (WS + /json + POST /v1/apns), AppSettings, repositories
│   └── iosMain      moteur Ktor Darwin, apnsEnvironment() = sandbox en Debug
│   └── androidMain  moteur Ktor OkHttp
├── composeApp/      UI Compose Multiplatform + ViewModel + point d'entrée Android/iOS
└── iosApp/          coquille SwiftUI + AppDelegate (enregistrement APNs) + projet Xcode
```

- **Réception** : `GET /<topic>/json?poll=1[&since=<id>]` (historique, NDJSON) puis
  WebSocket `wss://<host>/<topic>/ws` en continu (reconnexion auto 5 s). Les notifs
  sont mises en cache localement (500 max).
- **Auth ntfy** : le serveur est en `auth-default-access: deny-all`
  (`../ntfy/server.yml`) → toutes les requêtes partent avec
  `Authorization: Bearer <tk_…>` (réglage in-app, stocké chiffré : Keychain iOS /
  EncryptedSharedPreferences Android via multiplatform-settings).
  Provisionner un token côté VPS :
  `docker compose exec ntfy ntfy token add <user>` puis
  `ntfy access <user> 'zesit-*' read-write`.
- **Push natif iOS (APNs)** : `AppDelegate` appelle
  `UIApplication.registerForRemoteNotifications()`, récupère le device token et
  le pousse à `ApnsBridge.shared.registerDeviceToken(...)` (KMP), qui fait
  `POST {baseUrl}/v1/apns` avec `{"token","environment","topics":[...]}` (+ Bearer).
  C'est l'endpoint exposé par le **fork linusr/ntfy** (image
  `ghcr.io/linusr/ntfy:apns` dans `../docker-compose.yml`) quand le bloc
  `apns-*` (clé .p8, key id, team id, `apns-bundle-id`) est actif dans
  `../ntfy/server.yml` : le VPS pousse alors directement vers APNs, sans passer
  par ntfy.sh/Firebase. **Le `apns-bundle-id` du serveur doit être le bundle id
  de cette app : `fr.zesit.admin`** (déjà en place dans `ntfy/server.yml`).
  L'image officielle `binwiederhier/ntfy` n'a ni `/v1/apns` ni les options
  `apns-*`. Sans APNs, l'app fonctionne quand même (WS au premier plan,
  poll manuel).

## Prérequis

- macOS + Xcode 26.x (clang/Kotlin-Native toolchain iOS)
- JDK 17+ : SDKMAN (`sdk install java` — voir `.sdkmanrc` : `java=17.0.14-jbr`),
  le JBR d'Android Studio, ou n'importe quel JDK (`/usr/bin/java` suffit)
- Android SDK (`ANDROID_HOME` renseigné) pour la cible Android
- Gradle : le wrapper est fourni (`gradle/wrapper/gradle-wrapper.jar` +
  `gradle-wrapper.properties`, Gradle 8.14.3). Si le jar est absent :
  `brew install gradle && gradle wrapper --gradle-version 8.14.3`

## Build & run

```bash
cd zesit-admin

# Compile toutes les cibles (common, Android, iOS x64/Arm64/SimulatorArm64) + tests unitaires
./gradlew :shared:check

# Android
./gradlew :composeApp:assembleDebug        # APK dans composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug         # sur un device/émulateur connecté

# iOS : ouvrir le projet Xcode (la phase "Gradle build" appelle automatiquement
# ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode avant chaque build)
open iosApp/iosApp.xcodeproj
# Xcode : sélectionner l'équipe (Signing & Capabilities → Team), cible = iPhone perso,
# puis Run (⌘R). Le device token APNs n'est dispo que sur device physique.
```

## Dépannage Xcode : « java: command not found »

Xcode exécute les Run Script (dont notre phase « Gradle build ») avec un **PATH
minimal** (`/usr/bin:/bin:/usr/sbin:/sbin`), sans ton `.zshrc` : un JDK installé
via Homebrew, JetBrains Toolbox ou SDKMAN est alors invisible pour `./gradlew`.

La phase « Gradle build » du projet Xcode résout donc elle-même un JDK et
exporte `JAVA_HOME` (même approche que l'app ToyotaObd), dans l'ordre :
1. `$JAVA_HOME` déjà défini dans l'environnement ;
2. `$HOME/.sdkman/candidates/java/current` (SDKMAN — `.sdkmanrc` du projet) ;
3. le JBR d'Android Studio (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`) ;
4. `/usr/libexec/java_home` en dernier recours (JVMs enregistrées par macOS).

`gradlew` est le wrapper Gradle standard : il lance `$JAVA_HOME/bin/java`
(inutile de modifier le PATH). Test manuel de la phase en conditions Xcode :

```bash
env -i PATH=/usr/bin:/bin:/usr/sbin:/sbin HOME="$HOME" \
  SRCROOT="$PWD/iosApp" sh -c 'cd "$SRCROOT/.." && ./gradlew --version'
```

Si aucun des 4 chemins ne fournit de JDK, installez-en un : SDKMAN
(`sdk install java 17.0.14-jbr`), le JBR d'Android Studio, ou un JDK 17+
enregistré (`/usr/libexec/java_home` doit répondre, ex. .dmg Temurin ou
`brew install openjdk@17` + lien dans `/Library/Java/JavaVirtualMachines`).

## Variables d'environnement (iOS & desktop)

L'app lit ses secrets/URLs (token TI de traffic-inspector, token SC de
security-scanner, URLs par défaut des panneaux) dans l'environnement du
process **en priorité**, sinon dans des fichiers d'env locaux — **jamais
commités**.

### Générateur : `./gradlew generateEnv`

Source unique **`env.json`** (racine, ignoré par Git ; modèle
`env.json.example`) — les clés de `common` s'appliquent aux trois plateformes,
surchargeables clé par clé dans chaque section :

```json
{ "common": { "TI_API_TOKEN": "…", "SC_API_TOKEN": "…" }, "android": {}, "desktop": {}, "ios": {} }
```

`./gradlew generateEnv` régénère les fichiers d'env de toutes les plateformes
(valeur vide ou absente = réglage par défaut du code) :

| Sortie | Consommateur |
|---|---|
| `desktop.env` (racine) | lu au lancement par `DesktopEnvFile`, sans exports du shell (IDEA, `.app`) ; cherché aussi via `$ZESIT_DESKTOP_ENV` puis `~/.zesit-admin/desktop.env` |
| `iosApp/iosApp/Env.local.xcconfig` | chargé par `Env.xcconfig` (attaché à la cible Xcode) → clés figées dans l'Info.plist du `.app` ; le générateur échappe les URLs `//` → `$(SLASH)` (« // » ouvre un commentaire en xcconfig) |
| `composeApp/env-android.properties` | valeurs du BuildConfig Android (priorité : propriété Gradle > variable d'env > ce fichier) |

### Fichiers Env générés (table des rôles)

La **table des rôles** (clé d'env → champ `AppEnv` surchargé) vit dans le
`build.gradle.kts` racine et est la source de vérité de la consommation
applicative : `generateEnv` en dérive les fichiers **commités, marqués
« GÉNÉRÉ — NE PAS ÉDITER »** :

- `composeApp/src/commonMain/.../di/EnvOverrides.kt` — `withEnvOverrides(lookup)` :
  les trois `EnvFactory.*.kt` (fins et stables) ne font que fournir leur
  résolution (process > Info.plist sur iOS, process > desktop.env sur desktop,
  BuildConfig sur Android) ;
- `composeApp/src/androidMain/.../di/AndroidEnv.kt` — mapping clé → champ
  BuildConfig (lui-même généré depuis la même table par
  `composeApp/build.gradle.kts`).

Ajouter une variable d'env à l'app = **une ligne dans la table des rôles + une
entrée dans `env.json` + `./gradlew generateEnv`** — aucun des fichiers
`EnvFactory`, Info.plist ni buildConfigField à retoucher à la main.

Clés : `TI_API_TOKEN`, `SC_API_TOKEN`, `NTFY_TOKEN`, `KPI_BASE_URL`,
`NTFY_BASE_URL`, `TRAFFIC_BASE_URL`, `SECURITY_BASE_URL` (partout) et
`ZESIT_DEBUG=1` (desktop uniquement). L'environnement du process (variables du
schéma de run Xcode, exports du shell) reste prioritaire sur tous ces
fichiers ; la variable `TI_API_TOKEN` du schéma Xcode peut donc en être retirée.

## Configurer l'app (premier lancement)

Les panneaux de connexion ne demandent presque plus rien : les **URLs et les
tokens (TI, SC, ntfy) viennent de l'environnement** (`env.json` +
`./gradlew generateEnv`, voir plus haut) — plus aucun champ URL ni token à saisir.

1. Page KPI / Réel / Prévisionnel : identifiants seulement (l'URL vient de l'env).
2. Page Notifications : le **topic** (device id) seulement, ex.
   `zesit-3f9a1c7e04b2d8e5f6a0` (un par device, listé dans `DEVICE_TOPICS` du
   `.env` racine) ; le bearer ntfy (`NTFY_TOKEN`, obligatoire car `deny-all`)
   vient de l'env. Sans lui, le bouton est désactivé et le panneau l'explique.
3. Pages Traffic / Sécurité : aucun champ — tout vient de l'env.
4. Sur iOS : autoriser les notifications à l'invite ; le device token APNs est alors
   enregistré automatiquement (`POST /v1/apns`, visible dans le bandeau de statut).

## Envoyer une notif de test

```bash
cd .. && ./notify.sh "Test Zesit Admin" "hello depuis le VPS" max
```

## Notes iOS / APNs

- Bundle id : `fr.zesit.admin` — doit matcher `apns-bundle-id` dans `ntfy/server.yml`
  (fork `ghcr.io/linusr/ntfy:apns`, seul à exposer `/v1/apns`).
- Entitlements : `iosApp/iosApp.entitlements` (`aps-environment: development`).
  Pour TestFlight/App Store : passer à `production` et archiver en Release.
- Le push APNs ne fonctionne que sur **device physique** (pas le simulateur).
- Sans enregistrement APNs (serveur sans bloc `apns-*`), iOS retombe en
  polling : les notifs arrivent avec le délai décidé par iOS.
