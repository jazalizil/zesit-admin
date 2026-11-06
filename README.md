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
- JDK 17 (`/usr/bin/java` suffit)
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

La phase appelle `iosApp/iosApp/gradle-build.sh`, qui résout un JDK dans l'ordre :
1. `java` déjà dans le PATH ;
2. `/usr/libexec/java_home` (mécanisme Apple) ;
3. préfixes Homebrew (`/opt/homebrew/opt/openjdk@17`, …) ;
4. `/Library/Java/JavaVirtualMachines/*/Contents/Home` (JDK installés par .dmg).

Si toujours rien (« Unable to locate Java Runtime » = aucun JDK enregistré sur
la machine), installez un JDK 17+ **enregistré** :

```bash
# Option A — Homebrew (Apple Silicon : /opt/homebrew, Intel : /usr/local)
brew install openjdk@17
sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk
/usr/libexec/java_home        # doit répondre .../openjdk-17.jdk/Contents/Home

# Option B — .dmg Adoptium Temurin 17 (s'installe direct dans
# /Library/Java/JavaVirtualMachines) : https://adoptium.net/temurin/releases/
```

Repli manuel : `org.gradle.java.home=/Library/Java/JavaVirtualMachines/<jdk>.jdk/Contents/Home`
dans `zesit-admin/gradle.properties` (lu aussi par `iosApp/iosApp/gradle-build.sh`).
`gradlew` applique en outre le fallback `java_home` quand on l'appelle à la main.

## Configurer l'app (premier lancement)

1. Ouvrir les réglages (⚙) :
   - **URL** : `https://ntfy.zesit.fr` (défaut, = `ntfy.$ZESIT_DOMAIN`)
   - **Topic** : le device id, ex. `zesit-3f9a1c7e04b2d8e5f6a0` (un par device,
     listé dans `DEVICE_TOPICS` du `.env` racine)
   - **Token** : `tk_…` (bearer ntfy, obligatoire car `deny-all`)
2. « Tester la connexion » doit répondre ✓.
3. Sur iOS : autoriser les notifications à l'invite ; le device token APNs est alors
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
