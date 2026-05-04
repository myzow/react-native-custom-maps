# react-native-custom-maps

A production-ready maps library for React Native built on top of the
**New Architecture** (Fabric + TurboModules + codegen). Drop-in API
inspired by `react-native-maps`, with a single JS surface and a
**developer-selectable native renderer**:

| Platform | Renderer A (default) | Renderer B (opt-in) |
|----------|----------------------|----------------------|
| Android  | OpenStreetMap (osmdroid) — no API key | Google Maps SDK |
| iOS      | Apple MapKit — no API key | Google Maps SDK |

> Selection happens **at install time** (gradle property / Pod ENV var) so
> only one provider is linked into your app binary.

---

## ✨ Features

- ✅ **Fabric component** — codegen-generated bindings, no legacy bridge
- ✅ **MapView** + **Marker** declarative API
- ✅ Initial + controlled regions, gestures, user-location
- ✅ `onRegionChange` (live) + `onRegionChangeComplete`, `onMarkerPress`, `onPress`
- ✅ **Native clustering** for large marker sets (`clusteringEnabled`)
- ✅ Custom marker icons via `imageBase64`
- ✅ Strict TypeScript types
- ✅ Autolinking (RN ≥ 0.74)

---

## 📦 Installation

```bash
yarn add react-native-custom-maps
# or
npm install react-native-custom-maps
```

> Requires React Native **≥ 0.74** and the **New Architecture enabled**
> (`newArchEnabled=true` on Android, `RCT_NEW_ARCH_ENABLED=1` for `pod install`).
> Tested against RN **0.83.4**.

### Android — pick a provider

In your app's `android/gradle.properties`:

```properties
# OpenStreetMap (default — no key required)
customMapsProvider=osm

# OR: Google Maps
customMapsProvider=google
```

If you choose **Google Maps**, also add your API key to
`android/app/src/main/AndroidManifest.xml`:

```xml
<application>
  <meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY"/>
  ...
</application>
```

Add the permissions you need (the library declares none on its own):

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

### iOS — pick a provider

Set the env var **before** `pod install`:

```bash
# Apple MapKit (default — no key required)
RNC_CUSTOM_MAPS_PROVIDER=mapkit bundle exec pod install

# OR: Google Maps
RNC_CUSTOM_MAPS_PROVIDER=google bundle exec pod install
```

If you choose **Google Maps**, register your key in `AppDelegate.swift`:

```swift
import GoogleMaps

func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
  GMSServices.provideAPIKey("YOUR_IOS_GOOGLE_MAPS_API_KEY")
  ...
}
```

For MapKit, add to `Info.plist` if you use `showsUserLocation`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>Show my location on the map</string>
```

---

## 🚀 Usage

```tsx
import { MapView, Marker } from 'react-native-custom-maps';

export default function Screen() {
  return (
    <MapView
      style={{ flex: 1 }}
      initialRegion={{
        latitude: 37.7749,
        longitude: -122.4194,
        latitudeDelta: 0.2,
        longitudeDelta: 0.2,
      }}
      onRegionChangeComplete={(r) => console.log('settled', r)}
      onMarkerPress={(e) => console.log('tapped', e.identifier)}
      clusteringEnabled
    >
      <Marker
        identifier="ferry-bldg"
        coordinate={{ latitude: 37.7955, longitude: -122.3937 }}
        title="Ferry Building"
        description="Historic terminal at the foot of Market St."
      />
      <Marker
        coordinate={{ latitude: 37.8077, longitude: -122.4750 }}
        title="Golden Gate"
      />
    </MapView>
  );
}
```

---

## 📚 API reference

### `<MapView />`

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `initialRegion` | `Region` | — | Camera region applied once on mount. |
| `region` | `Region` | — | Controlled camera region. Pass to drive the map from state. |
| `provider` | `'google' \| 'osm' \| 'apple'` | `'default'` | Informational hint. The actual renderer is chosen at install time. |
| `zoomEnabled` | `boolean` | `true` | Toggles pinch-to-zoom. |
| `scrollEnabled` | `boolean` | `true` | Toggles pan gesture. |
| `showsUserLocation` | `boolean` | `false` | Shows the blue dot. **Caller is responsible for runtime permissions.** |
| `clusteringEnabled` | `boolean` | `false` | Enables native grid clustering for high marker counts. |
| `clusterRadius` | `number` | `60` | Cluster radius in pixels. |
| `onRegionChange` | `(region: Region) => void` | — | Fires continuously while the camera moves. |
| `onRegionChangeComplete` | `(region: Region) => void` | — | Fires once gestures finish. |
| `onPress` | `(event: MapPressEvent) => void` | — | User tapped on the map (not on a marker). |
| `onMarkerPress` | `(event: MarkerPressEvent) => void` | — | User tapped a marker. |
| `style` | `StyleProp<ViewStyle>` | — | Standard RN style. |
| children | `<Marker />` | — | Markers are flattened to a serialised native prop. |

### `<Marker />`

| Prop | Type | Description |
|------|------|-------------|
| `coordinate` | `LatLng` | Marker position. **Required.** |
| `identifier` | `string` | Stable id surfaced on `onMarkerPress`. Auto-generated if omitted. |
| `title` | `string` | Callout title. |
| `description` | `string` | Callout subtitle. |
| `imageBase64` | `string` | Base64-encoded PNG used as the marker icon. |
| `onPress` | `() => void` | Reserved (use `MapView.onMarkerPress`). |

### Types

```ts
interface LatLng {
  latitude: number;
  longitude: number;
}
interface Region extends LatLng {
  latitudeDelta: number;
  longitudeDelta: number;
}
interface MarkerPressEvent { identifier: string; coordinate: LatLng }
interface MapPressEvent    { coordinate: LatLng }
```

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────┐
│ JS / TS                                                │
│   src/MapView.tsx        ← high-level component        │
│   src/Marker.tsx         ← virtual; serialised to prop │
│   src/specs/CustomMapsViewNativeComponent.ts ← codegen │
└────────────────────────────────────────────────────────┘
                         │  codegen
                         ▼
┌────────────────────────────────────────────────────────┐
│ Fabric Component (provider-agnostic)                   │
│   Android: RNCCustomMapsViewManager (Kotlin)           │
│   iOS    : RNCCustomMapsView (Obj-C++ / Swift)         │
└────────────────────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                                  ▼
  Renderer A                        Renderer B
  ───────────                       ───────────
  Android: osmdroid                 Android: Google Maps
  iOS    : MapKit                   iOS    : Google Maps
```

**Why a single `markers` prop instead of native managed children?**
Fabric does not allow arbitrary React subtrees as children of a native
view (a known limitation as of RN 0.83). Flattening `<Marker />`
children into a serialisable array prop keeps the JSX ergonomic *and*
plays well with the renderer.

**Custom marker views** are supported by passing a pre-rasterised
`imageBase64` PNG (you can capture any RN view with `react-native-view-shot`).

---

## 🧪 Example app

Run the bundled demo (RN CLI):

```bash
cd example
yarn install
# Android (OSM by default):
yarn android
# iOS (MapKit by default):
yarn pods && yarn ios
```

The demo seeds 15 markers around San Francisco and toggles clustering.

---

## ⚠️ Limitations

- The `provider` JS prop is informational; the native renderer is chosen
  at install time (gradle / Pod ENV) for binary-size reasons.
- `<Marker>` `children` is reserved but rendered via base64 PNG (not a
  live React subtree). This keeps the surface compatible with Fabric.
- `showsUserLocation=true` requires the host app to request runtime
  permissions (`ACCESS_FINE_LOCATION` on Android, `NSLocationWhen…` on iOS).
- Clustering is grid-based (O(n)). For very large datasets (>50k pins)
  you should pre-cluster off-thread using the exported `clusterMarkers`
  helper.

---

## 📄 License

MIT © Emergent Labs
