package com.customaps

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.RNCCustomMapsViewManagerDelegate
import com.facebook.react.viewmanagers.RNCCustomMapsViewManagerInterface

/**
 * Fabric ViewManager.
 *
 * The actual View class (`RNCCustomMapsView`) is provided by ONE of the
 * provider-specific source sets (`src/google/java/...` or `src/osm/java/...`)
 * which are wired in via `build.gradle` based on the `customMapsProvider`
 * property. Both implementations expose the same public API so this manager
 * can stay provider-agnostic.
 */
@ReactModule(name = RNCCustomMapsViewManager.NAME)
class RNCCustomMapsViewManager : SimpleViewManager<RNCCustomMapsView>(),
  RNCCustomMapsViewManagerInterface<RNCCustomMapsView> {

  private val mDelegate: ViewManagerDelegate<RNCCustomMapsView> =
    RNCCustomMapsViewManagerDelegate(this)

  override fun getDelegate(): ViewManagerDelegate<RNCCustomMapsView> = mDelegate

  override fun getName(): String = NAME

  override fun createViewInstance(reactContext: ThemedReactContext): RNCCustomMapsView {
    return RNCCustomMapsView(reactContext)
  }

  override fun onDropViewInstance(view: RNCCustomMapsView) {
    view.cleanup()
    super.onDropViewInstance(view)
  }

  // ----- Props (each method is invoked by codegen-generated delegate) -----

  @ReactProp(name = "initialRegion")
  override fun setInitialRegion(view: RNCCustomMapsView, value: ReadableMap?) {
    value ?: return
    view.applyInitialRegion(
      value.getDouble("latitude"),
      value.getDouble("longitude"),
      value.getDouble("latitudeDelta"),
      value.getDouble("longitudeDelta"),
    )
  }

  @ReactProp(name = "region")
  override fun setRegion(view: RNCCustomMapsView, value: ReadableMap?) {
    value ?: return
    view.applyControlledRegion(
      value.getDouble("latitude"),
      value.getDouble("longitude"),
      value.getDouble("latitudeDelta"),
      value.getDouble("longitudeDelta"),
    )
  }

  @ReactProp(name = "markers")
  override fun setMarkers(view: RNCCustomMapsView, value: ReadableArray?) {
    view.applyMarkers(value)
  }

  @ReactProp(name = "provider")
  override fun setProvider(view: RNCCustomMapsView, value: String?) {
    view.providerHint = value ?: "default"
  }

  @ReactProp(name = "zoomEnabled", defaultBoolean = true)
  override fun setZoomEnabled(view: RNCCustomMapsView, value: Boolean) {
    view.setZoomEnabledImpl(value)
  }

  @ReactProp(name = "scrollEnabled", defaultBoolean = true)
  override fun setScrollEnabled(view: RNCCustomMapsView, value: Boolean) {
    view.setScrollEnabledImpl(value)
  }

  @ReactProp(name = "showsUserLocation", defaultBoolean = false)
  override fun setShowsUserLocation(view: RNCCustomMapsView, value: Boolean) {
    view.setShowsUserLocationImpl(value)
  }

  @ReactProp(name = "clusteringEnabled", defaultBoolean = false)
  override fun setClusteringEnabled(view: RNCCustomMapsView, value: Boolean) {
    view.setClusteringEnabledImpl(value)
  }

  @ReactProp(name = "clusterRadius", defaultInt = 60)
  override fun setClusterRadius(view: RNCCustomMapsView, value: Int) {
    view.setClusterRadiusImpl(value)
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
    return mutableMapOf(
      "topRegionChange" to mutableMapOf("registrationName" to "onRegionChange"),
      "topMarkerPress"  to mutableMapOf("registrationName" to "onMarkerPress"),
      "topMapPress"     to mutableMapOf("registrationName" to "onMapPress"),
    )
  }

  companion object {
    const val NAME = "RNCCustomMapsView"
  }
}
