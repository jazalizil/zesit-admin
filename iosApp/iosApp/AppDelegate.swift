import UIKit
import UserNotifications
import ComposeApp

/// Gère l'enregistrement APNs : quand Apple renvoie le device token, on le pousse
/// au module Kotlin (ApnsBridge) qui l'enregistre auprès du serveur ntfy (POST /v1/apns)
/// pour activer le push natif iOS (clé .p8 configurée côté serveur, bloc apns-* de
/// ntfy/server.yml). Le bundle id de cette app (fr.zesit.admin) doit correspondre
/// au `apns-topic` du serveur.
class AppDelegate: NSObject, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Fenêtre plein écran, root = vue Compose (MainViewController) :
        // couvre tout l'écran, y compris sous la status bar et le home indicator.
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = MainViewControllerKt.MainViewController()
        window.makeKeyAndVisible()
        self.window = window



        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error = error {
                print("APNs: autorisation refusée: \(error.localizedDescription)")
            }
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            }
        }
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenHex = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("APNs: device token reçu (\(tokenHex.prefix(12))…) → envoi à Zesit Admin (KMP)")
        SharedApnsBridge.shared.registerDeviceToken(tokenHex: tokenHex)
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("APNs: échec d'enregistrement: \(error.localizedDescription)")
    }
}

/// Affiche aussi les notifications quand l'app est au premier plan.
extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}
