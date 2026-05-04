/**
 * Public type definitions for react-native-custom-maps.
 */

export interface LatLng {
  latitude: number;
  longitude: number;
}

export interface Region {
  latitude: number;
  longitude: number;
  latitudeDelta: number;
  longitudeDelta: number;
}

export type MapProvider = 'google' | 'osm' | 'apple';

export interface MarkerPressEvent {
  /** Identifier of the pressed marker (auto-generated or user-supplied). */
  identifier: string;
  coordinate: LatLng;
}

export interface RegionChangeEvent {
  region: Region;
  /** True when the gesture has finished and the camera is settled. */
  isComplete: boolean;
}

export interface MapPressEvent {
  coordinate: LatLng;
}
