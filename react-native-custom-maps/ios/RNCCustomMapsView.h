#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Fabric component view for `RNCCustomMapsView`.
 *
 * The Swift implementation lives in `RNCCustomMapsViewImpl.swift`; this
 * Objective-C++ class is the entry point recognised by the Fabric
 * renderer (codegen looks up a class named `RNCCustomMapsView`).
 *
 * It:
 *   - declares itself as the component view for "RNCCustomMapsView"
 *   - decodes the codegen-generated props struct
 *   - forwards calls to the Swift implementation
 *   - emits direct events (onRegionChange, onMarkerPress, onMapPress)
 */
@interface RNCCustomMapsView : RCTViewComponentView
@end

NS_ASSUME_NONNULL_END
