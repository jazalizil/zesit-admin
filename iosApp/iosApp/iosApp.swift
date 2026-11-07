import SwiftUI

@main
struct ZesitAdminApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Plein écran : la vue Compose couvre aussi la status bar et le home indicator
                // (Compose reçoit quand même les safe area insets et padde le contenu).
                .ignoresSafeArea()
        }
    }
}
