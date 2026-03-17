import SwiftUI
import EncoreKMP

@main
struct EncoreKMPDemoApp: App {
    init() {
        // KMP Encore object exposed as Encore.shared in Swift
        Encore.shared.configure(apiKey: "pk_test_yr93u3skt5fgy99ame1pq3wx", logLevel: .debug)

        Encore.shared.identify(
            userId: "demo_user_kmp_ios_001",
            attributes: UserAttributes(
                email: "demo-ios@example.com",
                firstName: "Demo",
                lastName: "User",
                phoneNumber: nil,
                postalCode: nil,
                city: nil,
                state: nil,
                countryCode: "US",
                latitude: nil,
                longitude: nil,
                dateOfBirth: nil,
                gender: nil,
                language: nil,
                subscriptionTier: "free",
                monthsSubscribed: nil,
                billingCycle: nil,
                lastPaymentAmount: nil,
                lastActiveDate: nil,
                totalSessions: nil,
                custom: [:]
            )
        )

        Encore.shared.onPassthrough { placementId in
            print("[EncoreKMPDemo] Passthrough for placement: \(placementId ?? "nil")")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
