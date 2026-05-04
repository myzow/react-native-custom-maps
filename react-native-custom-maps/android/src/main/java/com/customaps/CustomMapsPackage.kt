package com.customaps

import android.view.View
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.uimanager.ViewManager
import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule

class CustomMapsPackage : TurboReactPackage() {

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    // No turbo modules in this library — only a Fabric component.
    return null
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider { emptyMap<String, ReactModuleInfo>() }
  }

  override fun createViewManagers(
    reactContext: ReactApplicationContext
  ): MutableList<ViewManager<out View, *>> {
    return mutableListOf(RNCCustomMapsViewManager())
  }
}
