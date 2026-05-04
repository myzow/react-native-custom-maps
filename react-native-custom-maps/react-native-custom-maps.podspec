require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

# Provider selection at install time. Consumers configure this in their Podfile:
#
#   ENV['RNC_CUSTOM_MAPS_PROVIDER'] = 'mapkit'   # or 'google'
#
# Defaults to MapKit (zero-config, no API key required).
provider = (ENV["RNC_CUSTOM_MAPS_PROVIDER"] || "mapkit").downcase

Pod::Spec.new do |s|
  s.name         = "react-native-custom-maps"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = "https://github.com/emergent-labs/react-native-custom-maps"
  s.license      = "MIT"
  s.authors      = { "Emergent Labs" => "support@emergent.sh" }
  s.platforms    = { :ios => "13.4" }
  s.source       = { :git => "https://github.com/emergent-labs/react-native-custom-maps.git", :tag => "v#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.requires_arc = true
  s.swift_version = "5.0"

  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES",
    "SWIFT_OBJC_INTEROP_MODE" => "objcxx",
    "OTHER_SWIFT_FLAGS" => "-DRNC_PROVIDER_#{provider.upcase}",
    "GCC_PREPROCESSOR_DEFINITIONS" => "RNC_PROVIDER_#{provider.upcase}=1"
  }

  if provider == "google"
    s.dependency "GoogleMaps", "~> 8.4"
    s.dependency "Google-Maps-iOS-Utils", "~> 5.0"
  end

  # Pull in the React Native New Architecture pods. install_modules_dependencies
  # is the standard helper exposed by RN for libraries shipping codegen specs.
  install_modules_dependencies(s)
end
