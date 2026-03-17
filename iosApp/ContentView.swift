import SwiftUI
import EncoreKMP

struct ContentView: View {
    @State private var resultText = "No result yet"
    @State private var isLoading = false

    var body: some View {
        VStack(spacing: 24) {
            Text("Encore KMP Demo (iOS)")
                .font(.title)

            Text("User: demo_user_kmp_ios_001")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Button("Show Placement") {
                isLoading = true
                let builder = Encore.shared.placement(id: "demo")
                builder.show { result, error in
                    DispatchQueue.main.async {
                        if let error = error {
                            resultText = "Error: \(error.localizedDescription)"
                        } else if let granted = result as? PresentationResultGranted {
                            resultText = "Granted (offer: \(granted.offerId ?? "nil"))"
                        } else if let notGranted = result as? PresentationResultNotGranted {
                            resultText = "Not granted (\(notGranted.reason.value))"
                        } else {
                            resultText = "Unknown result"
                        }
                        isLoading = false
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isLoading)

            Text(resultText)
                .font(.body)

            Button("Reset SDK") {
                Encore.shared.reset()
                resultText = "SDK reset"
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }
}
