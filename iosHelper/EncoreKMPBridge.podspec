Pod::Spec.new do |s|
  s.name         = 'EncoreKMPBridge'
  s.version      = '0.1.0'
  s.summary      = 'Obj-C bridge for EncoreKit Swift SDK — used by KMP cinterop'
  s.homepage     = 'https://encorekit.com'
  s.license      = { :type => 'MIT' }
  s.author       = 'Encore'
  s.source       = { :git => '', :tag => s.version.to_s }

  s.ios.deployment_target = '15.0'
  s.swift_version = '5.9'

  s.source_files = '*.swift'

  # Read iOS SDK version from the Gradle version catalog (single source of truth).
  # Same pattern as RN (JSON.parse package.json) and Flutter (YAML.safe_load pubspec.yaml).
  toml = File.read(File.join(__dir__, '..', 'gradle', 'libs.versions.toml'))
  match = toml.match(/encore-ios\s*=\s*"([^"]+)"/)
  raise "Could not parse encore-ios version from gradle/libs.versions.toml" unless match
  ios_version = match[1]

  s.dependency 'EncoreKit', ios_version
end
