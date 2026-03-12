Pod::Spec.new do |s|
  s.name         = 'EncoreKMPBridge'
  s.version      = '0.1.0'
  s.summary      = 'Obj-C bridge for EncoreKit Swift SDK — used by KMP cinterop'
  s.homepage     = 'https://encorekit.com'
  s.license      = { :type => 'MIT' }
  s.author       = 'Encore'
  s.source       = { :git => '', :tag => s.version.to_s }

  s.ios.deployment_target = '16.0'
  s.swift_version = '5.9'

  s.source_files = '*.swift'

  s.dependency 'EncoreKit', '~> 1.4'
end
