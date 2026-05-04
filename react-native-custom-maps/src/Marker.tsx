import React from 'react';
import type { LatLng } from './types';

export interface MarkerProps {
  /** Stable identifier used by `onMarkerPress`. Auto-generated if omitted. */
  identifier?: string;
  coordinate: LatLng;
  title?: string;
  description?: string;
  /**
   * Optional base64-encoded PNG used as the marker icon on the native side.
   * Pass either this or `children` (children render is processed by the
   * host app via captureRef → base64 before being sent to native).
   */
  imageBase64?: string;
  onPress?: () => void;
  children?: React.ReactNode;
}

/**
 * Marker — declarative child of <MapView />.
 *
 * This component never renders to the React tree; <MapView /> introspects
 * its children, serializes them to the native `markers` prop, and the
 * Fabric component is responsible for the actual marker drawing. This
 * mirrors the API shape of react-native-maps while staying compatible
 * with the New Architecture, where arbitrary React subtrees as
 * "managed children" of native views are not supported.
 */
export const Marker: React.FC<MarkerProps> = () => null;

export default Marker;
