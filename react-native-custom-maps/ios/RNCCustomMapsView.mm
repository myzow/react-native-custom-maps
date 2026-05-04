#import "RNCCustomMapsView.h"

#import <React/RCTConversions.h>
#import <React/RCTFabricComponentsPlugins.h>
#import <react/renderer/components/RNCustomMapsSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNCustomMapsSpec/EventEmitters.h>
#import <react/renderer/components/RNCustomMapsSpec/Props.h>
#import <react/renderer/components/RNCustomMapsSpec/RCTComponentViewHelpers.h>

#import "react_native_custom_maps-Swift.h"

using namespace facebook::react;

@interface RNCCustomMapsView () <RCTRNCCustomMapsViewViewProtocol>
@end

@implementation RNCCustomMapsView {
  RNCCustomMapsViewImpl *_impl;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<RNCCustomMapsViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RNCCustomMapsViewProps>();
    _props = defaultProps;
    _impl = [[RNCCustomMapsViewImpl alloc] initWithFrame:frame];
    _impl.eventDelegate = self;
    self.contentView = _impl;
  }
  return self;
}

#pragma mark - Props

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &newP = *std::static_pointer_cast<RNCCustomMapsViewProps const>(props);
  const auto &oldP = oldProps ? *std::static_pointer_cast<RNCCustomMapsViewProps const>(oldProps)
                              : RNCCustomMapsViewProps{};

  // Boolean flags
  if (newP.zoomEnabled != oldP.zoomEnabled) {
    [_impl setZoomEnabled:newP.zoomEnabled];
  }
  if (newP.scrollEnabled != oldP.scrollEnabled) {
    [_impl setScrollEnabled:newP.scrollEnabled];
  }
  if (newP.showsUserLocation != oldP.showsUserLocation) {
    [_impl setShowsUserLocation:newP.showsUserLocation];
  }
  if (newP.clusteringEnabled != oldP.clusteringEnabled ||
      newP.clusterRadius != oldP.clusterRadius) {
    [_impl setClusteringEnabled:newP.clusteringEnabled radius:newP.clusterRadius];
  }

  // Provider hint (informational on iOS — actual provider is decided at compile time).
  if (newP.provider != oldP.provider) {
    [_impl setProviderHint:[NSString stringWithUTF8String:newP.provider.c_str()]];
  }

  // initialRegion: only applied once.
  if (oldProps == nullptr) {
    [_impl applyInitialRegionWithLatitude:newP.initialRegion.latitude
                                longitude:newP.initialRegion.longitude
                            latitudeDelta:newP.initialRegion.latitudeDelta
                           longitudeDelta:newP.initialRegion.longitudeDelta];
  }

  // region (controlled).
  if (newP.region.latitude != oldP.region.latitude ||
      newP.region.longitude != oldP.region.longitude ||
      newP.region.latitudeDelta != oldP.region.latitudeDelta ||
      newP.region.longitudeDelta != oldP.region.longitudeDelta) {
    [_impl applyControlledRegionWithLatitude:newP.region.latitude
                                   longitude:newP.region.longitude
                               latitudeDelta:newP.region.latitudeDelta
                              longitudeDelta:newP.region.longitudeDelta];
  }

  // markers: copy the C++ vector into NSArray<NSDictionary *>.
  NSMutableArray<NSDictionary *> *markers = [NSMutableArray arrayWithCapacity:newP.markers.size()];
  for (const auto &m : newP.markers) {
    [markers addObject:@{
      @"identifier": [NSString stringWithUTF8String:m.identifier.c_str()],
      @"latitude":  @(m.latitude),
      @"longitude": @(m.longitude),
      @"title":       m.title.empty()       ? @"" : [NSString stringWithUTF8String:m.title.c_str()],
      @"description": m.description.empty() ? @"" : [NSString stringWithUTF8String:m.description.c_str()],
      @"imageBase64": m.imageBase64.empty() ? @"" : [NSString stringWithUTF8String:m.imageBase64.c_str()],
    }];
  }
  [_impl applyMarkers:markers];

  [super updateProps:props oldProps:oldProps];
}

#pragma mark - Event forwarding (called from Swift impl)

- (void)emitRegionChangeWithLatitude:(double)lat
                           longitude:(double)lon
                       latitudeDelta:(double)latD
                      longitudeDelta:(double)lonD
                          isComplete:(BOOL)isComplete
{
  if (!_eventEmitter) return;
  std::static_pointer_cast<const RNCCustomMapsViewEventEmitter>(_eventEmitter)
    ->onRegionChange({
      .latitude = lat,
      .longitude = lon,
      .latitudeDelta = latD,
      .longitudeDelta = lonD,
      .isComplete = (bool)isComplete,
    });
}

- (void)emitMarkerPressWithIdentifier:(NSString *)ident
                             latitude:(double)lat
                            longitude:(double)lon
{
  if (!_eventEmitter) return;
  std::static_pointer_cast<const RNCCustomMapsViewEventEmitter>(_eventEmitter)
    ->onMarkerPress({
      .identifier = std::string(ident.UTF8String ?: ""),
      .latitude = lat,
      .longitude = lon,
    });
}

- (void)emitMapPressWithLatitude:(double)lat longitude:(double)lon
{
  if (!_eventEmitter) return;
  std::static_pointer_cast<const RNCCustomMapsViewEventEmitter>(_eventEmitter)
    ->onMapPress({ .latitude = lat, .longitude = lon });
}

@end

Class<RCTComponentViewProtocol> RNCCustomMapsViewCls(void)
{
  return RNCCustomMapsView.class;
}
