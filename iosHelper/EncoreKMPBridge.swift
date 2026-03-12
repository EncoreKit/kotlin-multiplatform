// EncoreKMPBridge.swift
// @objc wrapper around the Swift-only EncoreKit SDK for Kotlin/Native cinterop.
// Modeled on flutter-sdk/ios/Classes/EncoreFlutterPlugin.swift and
// encore-react-native/ios/EncoreReactSDK.swift.

import Foundation
import Encore

@objc public class EncoreKMPBridge: NSObject {

    private var currentContinuation: CheckedContinuation<Void, Error>?
    private let lock = NSLock()

    @objc public static let shared = EncoreKMPBridge()

    // MARK: - Configuration

    @objc public func configure(apiKey: String, logLevel: String) {
        let level: Encore.LogLevel
        switch logLevel.uppercased() {
        case "DEBUG": level = .debug
        case "INFO": level = .info
        case "WARN": level = .warn
        case "ERROR": level = .error
        default: level = .none
        }
        Encore.shared.configure(apiKey: apiKey, logLevel: level)
    }

    // MARK: - User Identity

    @objc public func identify(userId: String, attributes: NSDictionary?) {
        let attrs = Self.parseAttributes(attributes)
        Encore.shared.identify(userId: userId, attributes: attrs)
    }

    @objc public func setUserAttributes(_ attributes: NSDictionary) {
        let attrs = Self.parseAttributes(attributes)
        if let attrs {
            Encore.shared.setUserAttributes(attrs)
        }
    }

    @objc public func reset() {
        cancelStalePurchase()
        Encore.shared.reset()
    }

    // MARK: - Placements

    @objc public func setClaimEnabled(_ enabled: Bool) {
        Encore.shared.placements.isClaimEnabled = enabled
    }

    /// Shows placement and calls completion on main thread.
    /// completion receives: (status: String, reason: String?, error: String?)
    @objc public func showPlacement(
        _ placementId: String?,
        completion: @escaping (String, String?, String?) -> Void
    ) {
        cancelStalePurchase()
        Task { @MainActor in
            do {
                let result = try await Encore.shared.placement(placementId).show()
                switch result {
                case .granted:
                    completion("granted", nil, nil)
                case .notGranted(let reason):
                    completion("notGranted", reason.rawValue, nil)
                @unknown default:
                    completion("notGranted", "unknown", nil)
                }
            } catch {
                completion("notGranted", nil, error.localizedDescription)
            }
        }
    }

    // MARK: - Purchase Handler Registration

    /// Registers onPurchaseRequest handler. When native SDK fires, calls
    /// `onRequest(productId, placementId, promoOfferId)`. Call `completePurchaseRequest`
    /// to resume the native flow.
    @objc public func registerOnPurchaseRequest(
        onRequest: @escaping (String, String?, String?) -> Void
    ) {
        Encore.shared.onPurchaseRequest { [weak self] request in
            guard let self = self else { return }

            self.cancelStalePurchase()

            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                self.lock.lock()
                self.currentContinuation = continuation
                self.lock.unlock()

                DispatchQueue.main.async {
                    onRequest(
                        request.productId,
                        request.placementId,
                        request.promoOfferId
                    )
                }
            }
        }
    }

    /// Resumes the pending purchase continuation.
    @objc public func completePurchaseRequest(success: Bool) {
        lock.lock()
        let continuation = currentContinuation
        currentContinuation = nil
        lock.unlock()

        if let continuation {
            if success {
                continuation.resume()
            } else {
                continuation.resume(throwing: NSError(
                    domain: "EncoreKMP",
                    code: -2,
                    userInfo: [NSLocalizedDescriptionKey: "Purchase failed by KMP handler"]
                ))
            }
        }
    }

    /// Registers onPurchaseComplete handler. Fires after native StoreKit purchase.
    @objc public func registerOnPurchaseComplete(
        onComplete: @escaping (String, String) -> Void
    ) {
        Encore.shared.onPurchaseComplete { [weak self] transaction, productId in
            guard self != nil else { return }
            DispatchQueue.main.async {
                onComplete(String(transaction.id), productId)
            }
        }
    }

    /// Registers onPassthrough handler. Fires for all not-granted outcomes.
    @objc public func registerOnPassthrough(
        onPassthrough: @escaping (String?) -> Void
    ) {
        Encore.shared.onPassthrough { [weak self] placementId in
            guard self != nil else { return }
            DispatchQueue.main.async {
                onPassthrough(placementId)
            }
        }
    }

    // MARK: - Internal

    private func cancelStalePurchase() {
        lock.lock()
        let stale = currentContinuation
        currentContinuation = nil
        lock.unlock()

        stale?.resume(throwing: NSError(
            domain: "EncoreKMP",
            code: -3,
            userInfo: [NSLocalizedDescriptionKey: "Superseded by new SDK action"]
        ))
    }

    // MARK: - Helpers

    private static func parseAttributes(_ dict: NSDictionary?) -> UserAttributes? {
        guard let dict = dict as? [String: Any] else { return nil }
        return UserAttributes(
            email: dict["email"] as? String,
            firstName: dict["firstName"] as? String,
            lastName: dict["lastName"] as? String,
            phoneNumber: dict["phoneNumber"] as? String,
            postalCode: dict["postalCode"] as? String,
            city: dict["city"] as? String,
            state: dict["state"] as? String,
            countryCode: dict["countryCode"] as? String,
            latitude: dict["latitude"] as? String,
            longitude: dict["longitude"] as? String,
            dateOfBirth: dict["dateOfBirth"] as? String,
            gender: dict["gender"] as? String,
            language: dict["language"] as? String,
            subscriptionTier: dict["subscriptionTier"] as? String,
            monthsSubscribed: dict["monthsSubscribed"] as? String,
            billingCycle: dict["billingCycle"] as? String,
            lastPaymentAmount: dict["lastPaymentAmount"] as? String,
            lastActiveDate: dict["lastActiveDate"] as? String,
            totalSessions: dict["totalSessions"] as? String,
            custom: dict["custom"] as? [String: String] ?? [:]
        )
    }
}
