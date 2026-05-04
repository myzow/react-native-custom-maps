package com.customaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.View
import android.widget.FrameLayout
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.ThemedReactContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.max
import kotlin.math.min

/**
 * Google Maps-backed implementation of the Fabric custom map view.
 *
 * The class name and public API match the OpenStreetMap variant in
 * `src/osm/java/...`; only ONE of the two source sets is compiled,
 * decided by the `customMapsProvider` Gradle property.
 */
class RNCCustomMapsView(private val themedContext: ThemedReactContext) :
  FrameLayout(themedContext.reactApplicationContext), OnMapReadyCallback {

  private val mapView: MapView = MapView(themedContext)
  private var map: GoogleMap? = null
  private val markers = HashMap<String, Marker>()
  private var pendingMarkers: List<ParsedMarker> = emptyList()
  private var initialRegionApplied = false
  private var pendingInitialRegion: DoubleArray? = null
  private var pendingControlledRegion: DoubleArray? = null
  private var clusteringEnabled = false
  private var clusterRadius = 60
  private var zoomEnabled = true
  private var scrollEnabled = true
  private var showsUserLocation = false
  var providerHint: String = "default"

  init {
    addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    mapView.onCreate(null)
    mapView.onStart()
    mapView.onResume()
    mapView.getMapAsync(this)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    map = googleMap
    googleMap.uiSettings.isZoomGesturesEnabled = zoomEnabled
    googleMap.uiSettings.isScrollGesturesEnabled = scrollEnabled
    if (showsUserLocation) {
      try { googleMap.isMyLocationEnabled = true } catch (_: SecurityException) { /* missing permission */ }
    }

    pendingInitialRegion?.let {
      moveCamera(it[0], it[1], it[2], it[3], animate = false)
      initialRegionApplied = true
      pendingInitialRegion = null
    }
    pendingControlledRegion?.let {
      moveCamera(it[0], it[1], it[2], it[3], animate = true)
      pendingControlledRegion = null
    }

    googleMap.setOnCameraMoveStartedListener {
      val tgt = googleMap.cameraPosition.target
      val span = visibleSpan(googleMap)
      CustomMapsEvents.emitRegionChange(themedContext, id, tgt.latitude, tgt.longitude, span.first, span.second, false)
    }
    googleMap.setOnCameraIdleListener {
      val tgt = googleMap.cameraPosition.target
      val span = visibleSpan(googleMap)
      CustomMapsEvents.emitRegionChange(themedContext, id, tgt.latitude, tgt.longitude, span.first, span.second, true)
    }
    googleMap.setOnMarkerClickListener { m ->
      val identifier = (m.tag as? String) ?: ""
      CustomMapsEvents.emitMarkerPress(themedContext, id, identifier, m.position.latitude, m.position.longitude)
      false
    }
    googleMap.setOnMapClickListener { ll ->
      CustomMapsEvents.emitMapPress(themedContext, id, ll.latitude, ll.longitude)
    }

    if (pendingMarkers.isNotEmpty()) renderMarkers(pendingMarkers)
  }

  private fun visibleSpan(googleMap: GoogleMap): Pair<Double, Double> {
    val bounds: LatLngBounds = googleMap.projection.visibleRegion.latLngBounds
    val latD = bounds.northeast.latitude - bounds.southwest.latitude
    val lonD = bounds.northeast.longitude - bounds.southwest.longitude
    return latD to lonD
  }

  // ---- Public API used by ViewManager ----

  fun applyInitialRegion(lat: Double, lon: Double, latD: Double, lonD: Double) {
    if (initialRegionApplied) return
    if (map == null) {
      pendingInitialRegion = doubleArrayOf(lat, lon, latD, lonD)
      return
    }
    moveCamera(lat, lon, latD, lonD, animate = false)
    initialRegionApplied = true
  }

  fun applyControlledRegion(lat: Double, lon: Double, latD: Double, lonD: Double) {
    if (map == null) {
      pendingControlledRegion = doubleArrayOf(lat, lon, latD, lonD)
      return
    }
    moveCamera(lat, lon, latD, lonD, animate = true)
  }

  fun applyMarkers(arr: ReadableArray?) {
    val parsed = parseMarkers(arr)
    pendingMarkers = parsed
    if (map != null) renderMarkers(parsed)
  }

  fun setZoomEnabledImpl(v: Boolean) { zoomEnabled = v; map?.uiSettings?.isZoomGesturesEnabled = v }
  fun setScrollEnabledImpl(v: Boolean) { scrollEnabled = v; map?.uiSettings?.isScrollGesturesEnabled = v }
  fun setShowsUserLocationImpl(v: Boolean) {
    showsUserLocation = v
    try { map?.isMyLocationEnabled = v } catch (_: SecurityException) { /* missing permission */ }
  }
  fun setClusteringEnabledImpl(v: Boolean) { clusteringEnabled = v; if (map != null) renderMarkers(pendingMarkers) }
  fun setClusterRadiusImpl(v: Int) { clusterRadius = v; if (map != null) renderMarkers(pendingMarkers) }

  fun cleanup() {
    try {
      mapView.onPause()
      mapView.onStop()
      mapView.onDestroy()
    } catch (_: Throwable) { /* lifecycle out of order — safe to ignore */ }
    markers.clear()
    map = null
  }

  // ---- Internals ----

  private fun moveCamera(lat: Double, lon: Double, latD: Double, lonD: Double, animate: Boolean) {
    val m = map ?: return
    val north = lat + latD / 2
    val south = lat - latD / 2
    val east  = lon + lonD / 2
    val west  = lon - lonD / 2
    val bounds = LatLngBounds(LatLng(min(south, north), min(west, east)), LatLng(max(south, north), max(west, east)))
    val update = CameraUpdateFactory.newLatLngBounds(bounds, 0)
    if (animate) m.animateCamera(update) else m.moveCamera(update)
  }

  private fun renderMarkers(list: List<ParsedMarker>) {
    val m = map ?: return
    for ((_, mk) in markers) mk.remove()
    markers.clear()

    val toShow = if (clusteringEnabled)
      clusterMarkers(list, max(0.001, clusterRadius / 1000.0))
    else list

    for (parsed in toShow) {
      val opts = MarkerOptions()
        .position(LatLng(parsed.latitude, parsed.longitude))
        .title(parsed.title)
        .snippet(parsed.description)
      val bmp = decodeImage(parsed.imageBase64)
      if (bmp != null) opts.icon(BitmapDescriptorFactory.fromBitmap(bmp))
      val mk = m.addMarker(opts) ?: continue
      mk.tag = parsed.identifier
      markers[parsed.identifier] = mk
    }
  }

  private fun decodeImage(b64: String?): Bitmap? {
    if (b64.isNullOrEmpty()) return null
    return try {
      val bytes = Base64.decode(b64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Throwable) {
      null
    }
  }

  override fun requestLayout() {
    super.requestLayout()
    // Fabric workaround: react-native does not call onMeasure/onLayout on
    // descendants when a forced layout is requested asynchronously. Schedule
    // a layout pass on the main looper so the MapView resizes properly.
    post(measureAndLayout)
  }

  private val measureAndLayout = Runnable {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
    )
    layout(left, top, right, bottom)
  }
}
