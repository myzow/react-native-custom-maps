# Architecture deep-dive

This document complements `README.md` for engineers who need to extend
the library or reason about its internals.

## 1. Codegen pipeline

`src/specs/CustomMapsViewNativeComponent.ts` declares the Fabric
component spec. At build time React Native's codegen produces:

```
build/generated/source/codegen/...                    (Android)
  ├─ ReactCommon/RNCustomMapsSpec/Props.h
  ├─ ReactCommon/RNCustomMapsSpec/EventEmitters.h
  ├─ ReactCommon/RNCustomMapsSpec/RCTComponentViewHelpers.h
  └─ java/com/facebook/react/viewmanagers/
        RNCCustomMapsViewManagerInterface.kt
        RNCCustomMapsViewManagerDelegate.kt

ios/build/generated/.../RNCustomMapsSpec/             (iOS)
  ├─ ComponentDescriptors.h
  ├─ EventEmitters.h
  ├─ Props.h
  └─ RCTComponentViewHelpers.h
```

The library code never imports these files directly during development —
they are generated when consumers build their apps. The Kotlin
`RNCCustomMapsViewManager` and the Obj-C++ `RNCCustomMapsView.mm`
implement the generated protocols/interfaces.

## 2. Provider selection

| Layer    | Mechanism                       |
|----------|---------------------------------|
| Android  | `customMapsProvider` Gradle prop drives the source-set choice in `android/build.gradle`; only one of `src/google/java` or `src/osm/java` is compiled. |
| iOS      | `RNC_CUSTOM_MAPS_PROVIDER` env var read by the Podspec, which sets `RNC_PROVIDER_GOOGLE` or `RNC_PROVIDER_MAPKIT` as a Swift compilation condition. The Swift impl gates renderer-specific code with `#if RNC_PROVIDER_GOOGLE`. |

Both Android implementations expose an **identical Kotlin class**
(`RNCCustomMapsView`) with the same public methods, so the ViewManager
remains provider-agnostic.

## 3. Event flow

```
JS  ──onRegionChange──►  NativeCustomMapsView (codegen)
                          │
                          ▼
              EventEmitter (Fabric)
                          │
                          ▼
        Android: dispatchEvent("topRegionChange")
        iOS    : RNCCustomMapsViewEventEmitter::onRegionChange()
```

The Android side uses the legacy `EventDispatcher` API which is fully
supported under Fabric — Fabric routes those events through the new
event pipeline transparently.

## 4. Marker rendering strategy

We flatten `<Marker>` children into a `markers` prop because:

1. Managed children of native components on Fabric require
   `RCTViewComponentView` mounting hooks that we can't safely rely on
   for arbitrary subtrees.
2. Map markers are rendered by the underlying SDK (osmdroid / MapKit /
   GoogleMaps), not by the React renderer — a 1:1 mapping with React
   tree nodes would be unnecessarily expensive.
3. Custom appearances are still supported via `imageBase64`, allowing
   apps to capture any RN component (with `react-native-view-shot`)
   into a pin icon.

## 5. Clustering

A naive O(n) grid clusterer lives in three places (so we can run it on
whichever side has the freshest data):

- `src/clustering.ts`           ← optional JS pre-clustering
- `android/.../CustomMapsEvents.kt` ← native clusterer used by both
                                       Android impls
- `ios/.../RNCCustomMapsViewImpl.swift` ← static `cluster(...)`

Cell size is derived from `clusterRadius`. The centre of each cluster
is the mean of its members; clusters with >1 member surface a synthetic
`identifier` of the form `cluster-<first-id>-<count>`.
