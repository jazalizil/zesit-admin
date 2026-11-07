import UIKit

// Point d'entrée UIKit pur : la fenêtre plein écran est créée dans AppDelegate
// (rootViewController = MainViewController Compose), donc la vue Compose couvre
// tout l'écran (status bar + home indicator inclus).
UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil, NSStringFromClass(AppDelegate.self))
