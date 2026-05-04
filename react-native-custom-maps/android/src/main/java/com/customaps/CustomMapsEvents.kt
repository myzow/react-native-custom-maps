package com.customaps

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.Event
import com.facebook.react.uimanager.ThemedReactContext

/**
 * Provider-agnostic helpers for emitting Fabric direct events back to JS.
 * Both Google and OSM implementations of `RNCCustomMapsView` use these.
 */
internal object CustomMapsEvents {

  fun emitRegionChange(
    context: ThemedReactContext,
    viewId: Int,
    latitude: Double,
    longitude: Double,
    latitudeDelta: Double,
    longitudeDelta: Double,
    isComplete: Boolean,
  ) {
    val payload = Arguments.createMap().apply {
      putDouble("latitude", latitude)
      putDouble("longitude", longitude)
      putDouble("latitudeDelta", latitudeDelta)
      putDouble("longitudeDelta", longitudeDelta)
      putBoolean("isComplete", isComplete)
    }
    dispatch(context, viewId, "topRegionChange", payload)
  }

  fun emitMarkerPress(
    context: ThemedReactContext,
    viewId: Int,
    identifier: String,
    latitude: Double,
    longitude: Double,
  ) {
    val payload = Arguments.createMap().apply {
      putString("identifier", identifier)
      putDouble("latitude", latitude)
      putDouble("longitude", longitude)
    }
    dispatch(context, viewId, "topMarkerPress", payload)
  }

  fun emitMapPress(
    context: ThemedReactContext,
    viewId: Int,
    latitude: Double,
    longitude: Double,
  ) {
    val payload = Arguments.createMap().apply {
      putDouble("latitude", latitude)
      putDouble("longitude", longitude)
    }
    dispatch(context, viewId, "topMapPress", payload)
  }

  private fun dispatch(
    context: ThemedReactContext,
    viewId: Int,
    eventName: String,
    payload: WritableMap,
  ) {
    val surfaceId = UIManagerHelper.getSurfaceId(context)
    val dispatcher = UIManagerHelper.getEventDispatcherForReactTag(context, viewId)
    dispatcher?.dispatchEvent(CustomMapsDirectEvent(surfaceId, viewId, eventName, payload))
  }
}

/**
 * Concrete, named Event subclass.
 *
 * Kotlin's `Event<T : Event<T>>` uses F-bounded polymorphism, which means
 * `object : Event<Event<*>>(...)` (an anonymous class parameterized by a
 * star-projected Event) does NOT satisfy the bound and fails to compile.
 * A concrete self-referencing subclass is the idiomatic fix.
 */
private class CustomMapsDirectEvent(
  surfaceId: Int,
  viewId: Int,
  private val name: String,
  private val data: WritableMap,
) : Event<CustomMapsDirectEvent>(surfaceId, viewId) {
  override fun getEventName(): String = name
  override fun getEventData(): WritableMap = data
}

/**
 * Holds the parsed marker list shared by both provider impls.
 */
internal data class ParsedMarker(
  val identifier: String,
  val latitude: Double,
  val longitude: Double,
  val title: String?,
  val description: String?,
  val imageBase64: String?,
)

internal fun parseMarkers(arr: ReadableArray?): List<ParsedMarker> {
  arr ?: return emptyList()
  val out = ArrayList<ParsedMarker>(arr.size())
  for (i in 0 until arr.size()) {
    val m = arr.getMap(i) ?: continue
    out.add(
      ParsedMarker(
        identifier = if (m.hasKey("identifier")) m.getString("identifier") ?: "" else "marker-$i",
        latitude = m.getDouble("latitude"),
        longitude = m.getDouble("longitude"),
        title = if (m.hasKey("title")) m.getString("title") else null,
        description = if (m.hasKey("description")) m.getString("description") else null,
        imageBase64 = if (m.hasKey("imageBase64")) m.getString("imageBase64") else null,
      ),
    )
  }
  return out
}

/**
 * Naive grid clustering (mirrors the JS helper).
 */
internal fun clusterMarkers(
  markers: List<ParsedMarker>,
  cellSizeDeg: Double,
): List<ParsedMarker> {
  if (markers.isEmpty()) return markers
  val buckets = HashMap<String, MutableList<ParsedMarker>>()
  for (m in markers) {
    val key = "${(m.latitude / cellSizeDeg).toInt()}:${(m.longitude / cellSizeDeg).toInt()}"
    buckets.getOrPut(key) { ArrayList() }.add(m)
  }
  val out = ArrayList<ParsedMarker>(buckets.size)
  for ((_, members) in buckets) {
    if (members.size == 1) {
      out.add(members[0])
    } else {
      var lat = 0.0
      var lon = 0.0
      for (m in members) { lat += m.latitude; lon += m.longitude }
      out.add(
        ParsedMarker(
          identifier = "cluster-${members[0].identifier}-${members.size}",
          latitude = lat / members.size,
          longitude = lon / members.size,
          title = "${members.size} markers",
          description = null,
          imageBase64 = null,
        ),
      )
    }
  }
  return out
}
