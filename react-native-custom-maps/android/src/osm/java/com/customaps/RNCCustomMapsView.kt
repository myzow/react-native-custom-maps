package com.customaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.view.MotionEvent
import android.widget.FrameLayout
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.ThemedReactContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import kotlin.math.max
import kotlin.math.pow

/**
 * OpenStreetMap (osmdroid) implementation of the Fabric custom map view.
 * Identical public API to the Google Maps variant — see google/java/...
 */
class RNCCustomMapsView(private val themedContext: ThemedReactContext) :
  FrameLayout(themedContext.reactApplicationContext) {

  private val mapView: MapView = MapView(themedContext)
  private val markerOverlays = HashMap<String, Marker>()
  private var pendingMarkers: List<ParsedMarker> = emptyList()
  private var initialRegionApplied = false
  private var clusteringEnabled = false
  private var clusterRadius = 60
  var providerHint: String = "default"

  init {
    Configuration.getInstance().userAgentValue = themedContext.packageName
    mapView.setTileSource(TileSourceFactory.MAPNIK)
    mapView.setMultiTouchControls(true)
    addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

    mapView.addMapListener(object : MapListener {
      override fun onScroll(event: ScrollEvent?): Boolean { dispatchRegion(false); return false }
      override fun onZoom(event: ZoomEvent?): Boolean     { dispatchRegion(true); return false }
    })

    mapView.overlays.add(object : Overlay() {
      override fun onSingleTapConfirmed(e: MotionEvent, mv: MapView): Boolean {
        val proj = mv.projection
        val gp = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
        CustomMapsEvents.emitMapPress(themedContext, id, gp.latitude, gp.longitude)
        return false
      }
    })
  }

  private fun dispatchRegion(isComplete: Boolean) {
    val center = mapView.mapCenter
    // osmdroid uses zoom levels (Web Mercator); convert to delta degrees.
    val zoom = mapView.zoomLevelDouble
    val span = 360.0 / 2.0.pow(zoom)
    CustomMapsEvents.emitRegionChange(
      themedContext, id,
      center.latitude, center.longitude,
      span, span, isComplete,
    )
  }

  // ---- Public API used by ViewManager ----

  fun applyInitialRegion(lat: Double, lon: Double, latD: Double, lonD: Double) {
    if (initialRegionApplied) return
    initialRegionApplied = true
    setRegion(lat, lon, latD, lonD)
  }

  fun applyControlledRegion(lat: Double, lon: Double, latD: Double, lonD: Double) {
    setRegion(lat, lon, latD, lonD)
  }

  private fun setRegion(lat: Double, lon: Double, latD: Double, @Suppress("UNUSED_PARAMETER") lonD: Double) {
    val controller = mapView.controller
    val targetZoom = (Math.log(360.0 / latD) / Math.log(2.0)).coerceIn(1.0, 20.0)
    controller.setZoom(targetZoom)
    controller.setCenter(GeoPoint(lat, lon))
  }

  fun applyMarkers(arr: ReadableArray?) {
    val parsed = parseMarkers(arr)
    pendingMarkers = parsed
    renderMarkers(parsed)
  }

  fun setZoomEnabledImpl(v: Boolean) { mapView.setMultiTouchControls(v) }
  fun setScrollEnabledImpl(v: Boolean) {
    // osmdroid lacks a direct scroll toggle; intercept touches when disabled.
    mapView.isClickable = v
    mapView.isFocusable = v
  }
  fun setShowsUserLocationImpl(@Suppress("UNUSED_PARAMETER") v: Boolean) {
    // Implementing the user-location overlay requires runtime permission;
    // documented as a host-app responsibility (see README).
  }
  fun setClusteringEnabledImpl(v: Boolean) { clusteringEnabled = v; renderMarkers(pendingMarkers) }
  fun setClusterRadiusImpl(v: Int) { clusterRadius = v; renderMarkers(pendingMarkers) }

  fun cleanup() {
    mapView.overlays.clear()
    markerOverlays.clear()
    mapView.onDetach()
  }

  private fun renderMarkers(list: List<ParsedMarker>) {
    for ((_, mk) in markerOverlays) mapView.overlays.remove(mk)
    markerOverlays.clear()

    val toShow = if (clusteringEnabled)
      clusterMarkers(list, max(0.001, clusterRadius / 1000.0))
    else list

    for (parsed in toShow) {
      val mk = Marker(mapView).apply {
        position = GeoPoint(parsed.latitude, parsed.longitude)
        title = parsed.title
        subDescription = parsed.description
        id = parsed.identifier
      }
      decodeImage(parsed.imageBase64)?.let { bmp ->
        mk.icon = BitmapDrawable(resources, bmp)
      }
      mk.setOnMarkerClickListener { m, _ ->
        CustomMapsEvents.emitMarkerPress(themedContext, id, m.id ?: "", m.position.latitude, m.position.longitude)
        true
      }
      mapView.overlays.add(mk)
      markerOverlays[parsed.identifier] = mk
    }
    mapView.invalidate()
  }

  private fun decodeImage(b64: String?): Bitmap? {
    if (b64.isNullOrEmpty()) return null
    return try {
      val bytes = Base64.decode(b64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Throwable) { null }
  }

  override fun requestLayout() {
    super.requestLayout()
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
