/**
 * Fabric Native Component spec for the custom maps view.
 *
 * This file is consumed by React Native codegen at build time
 * to generate the native interfaces (C++/ObjC++/Java) for the
 * Fabric component. Do NOT import this file directly in product
 * code — use the wrappers in `src/MapView.tsx` and `src/Marker.tsx`.
 */

import type { HostComponent, ViewProps } from 'react-native';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type {
  DirectEventHandler,
  Double,
  WithDefault,
  Int32,
} from 'react-native/Libraries/Types/CodegenTypes';

type RegionType = Readonly<{
  latitude: Double;
  longitude: Double;
  latitudeDelta: Double;
  longitudeDelta: Double;
}>;

type MarkerType = Readonly<{
  identifier: string;
  latitude: Double;
  longitude: Double;
  title?: string;
  description?: string;
  /**
   * If provided, the native side will rasterize a React subtree
   * delivered via this base64 PNG and use it as marker icon.
   * (Set by the high-level <Marker> component when children exist.)
   */
  imageBase64?: string;
}>;

type OnRegionChangeEvent = Readonly<{
  latitude: Double;
  longitude: Double;
  latitudeDelta: Double;
  longitudeDelta: Double;
  isComplete: boolean;
}>;

type OnMarkerPressEvent = Readonly<{
  identifier: string;
  latitude: Double;
  longitude: Double;
}>;

type OnMapPressEvent = Readonly<{
  latitude: Double;
  longitude: Double;
}>;

export interface NativeProps extends ViewProps {
  initialRegion?: RegionType;
  region?: RegionType;
  markers?: ReadonlyArray<MarkerType>;
  provider?: WithDefault<string, 'default'>;
  zoomEnabled?: WithDefault<boolean, true>;
  scrollEnabled?: WithDefault<boolean, true>;
  showsUserLocation?: WithDefault<boolean, false>;
  /** Enable native marker clustering. */
  clusteringEnabled?: WithDefault<boolean, false>;
  /** Minimum cluster size (only used when clustering is enabled). */
  clusterRadius?: WithDefault<Int32, 60>;

  onRegionChange?: DirectEventHandler<OnRegionChangeEvent>;
  onMarkerPress?: DirectEventHandler<OnMarkerPressEvent>;
  onMapPress?: DirectEventHandler<OnMapPressEvent>;
}

export default codegenNativeComponent<NativeProps>(
  'RNCCustomMapsView',
) as HostComponent<NativeProps>;
