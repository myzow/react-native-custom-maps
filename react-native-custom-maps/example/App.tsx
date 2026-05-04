/**
 * Example app for react-native-custom-maps.
 *
 * Demonstrates:
 *   - Initial region
 *   - Multiple markers (15 random pins around San Francisco)
 *   - Marker press handler (updates UI counter)
 *   - Region change reporting
 *   - Toggleable clustering
 *   - Toggleable provider hint (informational on iOS;
 *     real provider is selected at install time)
 */

import React, { useCallback, useMemo, useState } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { MapView, Marker, type Region, type MarkerPressEvent } from 'react-native-custom-maps';

const SF: Region = {
  latitude: 37.7749,
  longitude: -122.4194,
  latitudeDelta: 0.2,
  longitudeDelta: 0.2,
};

function buildSeed(n: number) {
  const out: { id: string; latitude: number; longitude: number; title: string }[] = [];
  for (let i = 0; i < n; i++) {
    const jitterLat = (Math.sin(i * 12.9898) * 43758.5453) % 1;
    const jitterLon = (Math.cos(i * 78.233)  * 43758.5453) % 1;
    out.push({
      id: `pin-${i}`,
      latitude: SF.latitude  + jitterLat * 0.18,
      longitude: SF.longitude + jitterLon * 0.18,
      title: `Stop #${i + 1}`,
    });
  }
  return out;
}

export default function App() {
  const seed = useMemo(() => buildSeed(15), []);
  const [pressed, setPressed] = useState<string>('—');
  const [region, setRegion] = useState<Region>(SF);
  const [clustered, setClustered] = useState(false);

  const onMarkerPress = useCallback((e: MarkerPressEvent) => {
    setPressed(`${e.identifier} @ ${e.coordinate.latitude.toFixed(3)}, ${e.coordinate.longitude.toFixed(3)}`);
  }, []);

  return (
    <SafeAreaView style={styles.root}>
      <View style={styles.headerBar}>
        <Text style={styles.title} testID="example-title">react-native-custom-maps</Text>
        <Text style={styles.subtitle}>
          {region.latitude.toFixed(3)}, {region.longitude.toFixed(3)} · Δ{region.latitudeDelta.toFixed(2)}
        </Text>
      </View>

      <MapView
        testID="map-view"
        style={styles.map}
        initialRegion={SF}
        onRegionChangeComplete={setRegion}
        onMarkerPress={onMarkerPress}
        clusteringEnabled={clustered}
        showsUserLocation={false}
      >
        {seed.map((p) => (
          <Marker
            key={p.id}
            identifier={p.id}
            coordinate={{ latitude: p.latitude, longitude: p.longitude }}
            title={p.title}
            description="Tap to select"
          />
        ))}
      </MapView>

      <View style={styles.footer}>
        <Text style={styles.tapped} testID="last-pressed">Last tapped: {pressed}</Text>
        <TouchableOpacity
          testID="toggle-clustering-btn"
          style={[styles.btn, clustered && styles.btnActive]}
          onPress={() => setClustered((v) => !v)}
        >
          <Text style={styles.btnText}>{clustered ? 'Clustering: ON' : 'Clustering: OFF'}</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#0b0d10' },
  headerBar: { paddingHorizontal: 20, paddingVertical: 14 },
  title: { color: '#e6e8eb', fontSize: 20, fontWeight: '700', letterSpacing: -0.3 },
  subtitle: { color: '#7b8794', marginTop: 4, fontSize: 12 },
  map: { flex: 1, marginHorizontal: 12, borderRadius: 16, overflow: 'hidden' },
  footer: { padding: 16, gap: 12 },
  tapped: { color: '#cbd2d9', fontSize: 13 },
  btn: {
    backgroundColor: '#1f2937',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
  },
  btnActive: { backgroundColor: '#2563eb' },
  btnText: { color: '#fff', fontWeight: '600' },
});
