/**
 * JS-side helpers for clustering. The native side performs grid-based
 * clustering when `clusteringEnabled` is true; this utility is exposed
 * for cases where users want to pre-cluster off the UI thread.
 */

import type { LatLng } from './types';

export interface ClusterableMarker {
  identifier: string;
  coordinate: LatLng;
  weight?: number;
}

export interface Cluster {
  center: LatLng;
  count: number;
  members: ClusterableMarker[];
}

/**
 * Simple grid-based clustering. O(n).
 * Buckets markers by integer (lat/cell, lon/cell) keys.
 */
export function clusterMarkers(
  markers: ClusterableMarker[],
  cellSizeDeg = 0.05,
): Cluster[] {
  const buckets = new Map<string, ClusterableMarker[]>();
  for (const m of markers) {
    const kx = Math.floor(m.coordinate.latitude / cellSizeDeg);
    const ky = Math.floor(m.coordinate.longitude / cellSizeDeg);
    const key = `${kx}:${ky}`;
    const arr = buckets.get(key);
    if (arr) arr.push(m);
    else buckets.set(key, [m]);
  }
  const out: Cluster[] = [];
  for (const members of buckets.values()) {
    let lat = 0;
    let lon = 0;
    let total = 0;
    for (const m of members) {
      const w = m.weight ?? 1;
      lat += m.coordinate.latitude * w;
      lon += m.coordinate.longitude * w;
      total += w;
    }
    out.push({
      center: { latitude: lat / total, longitude: lon / total },
      count: members.length,
      members,
    });
  }
  return out;
}
