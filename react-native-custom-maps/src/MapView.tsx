import React, {
  Children,
  isValidElement,
  useCallback,
  useMemo,
  useRef,
  useState,
} from 'react';
import { StyleSheet, View, type ViewStyle, type StyleProp } from 'react-native';
import NativeCustomMapsView from './specs/CustomMapsViewNativeComponent';
import type {
  Region,
  MapPressEvent,
  RegionChangeEvent,
  MarkerPressEvent,
  MapProvider,
} from './types';
import type { MarkerProps } from './Marker';
import { Marker } from './Marker';

export interface MapViewProps {
  style?: StyleProp<ViewStyle>;
  /** Initial camera region (uncontrolled). */
  initialRegion?: Region;
  /** Controlled camera region. If supplied, native view follows this prop. */
  region?: Region;
  /** Renderer used on the underlying platform.
   *  - "google" : Google Maps (Android & iOS)
   *  - "osm"    : OpenStreetMap (Android only)
   *  - "apple"  : Apple MapKit (iOS only, default)
   *  When omitted, each platform falls back to its native default. */
  provider?: MapProvider;
  zoomEnabled?: boolean;
  scrollEnabled?: boolean;
  showsUserLocation?: boolean;

  /** Enables native point clustering for large marker sets. */
  clusteringEnabled?: boolean;
  /** Pixel radius used by the native clusterer (default 60). */
  clusterRadius?: number;

  onRegionChange?: (region: Region) => void;
  onRegionChangeComplete?: (region: Region) => void;
  onPress?: (event: MapPressEvent) => void;
  onMarkerPress?: (event: MarkerPressEvent) => void;

  children?: React.ReactNode;
  testID?: string;
}

/**
 * MapView — Fabric native component wrapper.
 *
 * Children are expected to be <Marker /> elements; they are flattened
 * to a serializable `markers` prop and passed to the native view, so
 * the entire scene re-renders atomically on the native thread.
 */
export const MapView: React.FC<MapViewProps> = ({
  style,
  initialRegion,
  region,
  provider,
  zoomEnabled = true,
  scrollEnabled = true,
  showsUserLocation = false,
  clusteringEnabled = false,
  clusterRadius = 60,
  onRegionChange,
  onRegionChangeComplete,
  onPress,
  onMarkerPress,
  children,
  testID,
}) => {
  // Track the latest region so consumers can use the component as
  // either controlled or uncontrolled.
  const lastRegionRef = useRef<Region | undefined>(initialRegion ?? region);
  const [, forceTick] = useState(0);

  const markers = useMemo(() => {
    const out: Array<{
      identifier: string;
      latitude: number;
      longitude: number;
      title?: string;
      description?: string;
      imageBase64?: string;
    }> = [];
    let autoIndex = 0;
    Children.forEach(children, (child) => {
      if (!isValidElement<MarkerProps>(child)) return;
      if (child.type !== Marker) return;
      const p = child.props;
      out.push({
        identifier:
          p.identifier ?? `marker-${autoIndex++}-${p.coordinate.latitude}-${p.coordinate.longitude}`,
        latitude: p.coordinate.latitude,
        longitude: p.coordinate.longitude,
        title: p.title,
        description: p.description,
        imageBase64: p.imageBase64,
      });
    });
    return out;
  }, [children]);

  const handleRegionChange = useCallback(
    (e: { nativeEvent: { latitude: number; longitude: number; latitudeDelta: number; longitudeDelta: number; isComplete: boolean } }) => {
      const r: Region = {
        latitude: e.nativeEvent.latitude,
        longitude: e.nativeEvent.longitude,
        latitudeDelta: e.nativeEvent.latitudeDelta,
        longitudeDelta: e.nativeEvent.longitudeDelta,
      };
      lastRegionRef.current = r;
      if (e.nativeEvent.isComplete) {
        onRegionChangeComplete?.(r);
      } else {
        onRegionChange?.(r);
      }
      forceTick((n) => n + 1);
    },
    [onRegionChange, onRegionChangeComplete],
  );

  const handleMarkerPress = useCallback(
    (e: { nativeEvent: { identifier: string; latitude: number; longitude: number } }) => {
      onMarkerPress?.({
        identifier: e.nativeEvent.identifier,
        coordinate: {
          latitude: e.nativeEvent.latitude,
          longitude: e.nativeEvent.longitude,
        },
      });
    },
    [onMarkerPress],
  );

  const handleMapPress = useCallback(
    (e: { nativeEvent: { latitude: number; longitude: number } }) => {
      onPress?.({
        coordinate: {
          latitude: e.nativeEvent.latitude,
          longitude: e.nativeEvent.longitude,
        },
      });
    },
    [onPress],
  );

  return (
    <View style={[styles.container, style]} testID={testID}>
      <NativeCustomMapsView
        style={StyleSheet.absoluteFill}
        initialRegion={initialRegion}
        region={region}
        provider={provider ?? 'default'}
        zoomEnabled={zoomEnabled}
        scrollEnabled={scrollEnabled}
        showsUserLocation={showsUserLocation}
        clusteringEnabled={clusteringEnabled}
        clusterRadius={clusterRadius}
        markers={markers}
        onRegionChange={handleRegionChange}
        onMarkerPress={handleMarkerPress}
        onMapPress={handleMapPress}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    overflow: 'hidden',
  },
});

export default MapView;
