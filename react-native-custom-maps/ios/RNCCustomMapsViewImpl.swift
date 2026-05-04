import Foundation
import UIKit

#if RNC_PROVIDER_GOOGLE
import GoogleMaps
#else
import MapKit
#endif

/// Bridge protocol exposed back to Objective-C++ so the Swift impl
/// can emit Fabric events without importing C++ headers.
@objc public protocol RNCCustomMapsEventDelegate: AnyObject {
  func emitRegionChange(latitude: Double,
                        longitude: Double,
                        latitudeDelta: Double,
                        longitudeDelta: Double,
                        isComplete: Bool)
  func emitMarkerPress(identifier: String, latitude: Double, longitude: Double)
  func emitMapPress(latitude: Double, longitude: Double)
}

@objc public final class RNCCustomMapsViewImpl: UIView {

  @objc public weak var eventDelegate: RNCCustomMapsEventDelegate?

  private var initialRegionApplied = false
  private var clusteringEnabled = false
  private var clusterRadius: Int32 = 60
  private var providerHint: String = "default"
  private var markersData: [[String: Any]] = []

  // MARK: - Provider-specific surfaces
  #if RNC_PROVIDER_GOOGLE
  private let mapView: GMSMapView = {
    let camera = GMSCameraPosition.camera(withLatitude: 0, longitude: 0, zoom: 2)
    return GMSMapView(frame: .zero, camera: camera)
  }()
  private var gmsMarkers: [String: GMSMarker] = [:]
  #else
  private let mapView = MKMapView(frame: .zero)
  private let markerTapRecognizer = UITapGestureRecognizer()
  private let mapTapRecognizer = UITapGestureRecognizer()
  #endif

  override init(frame: CGRect) {
    super.init(frame: frame)
    setupMap()
  }

  required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

  private func setupMap() {
    mapView.frame = bounds
    mapView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    addSubview(mapView)

    #if RNC_PROVIDER_GOOGLE
    mapView.delegate = self
    #else
    mapView.delegate = self
    let tap = UITapGestureRecognizer(target: self, action: #selector(handleMapTap(_:)))
    tap.cancelsTouchesInView = false
    mapView.addGestureRecognizer(tap)
    #endif
  }

  // MARK: - Props bridge (called from RNCCustomMapsView.mm)

  @objc public func setZoomEnabled(_ enabled: Bool) {
    #if RNC_PROVIDER_GOOGLE
    mapView.settings.zoomGestures = enabled
    #else
    mapView.isZoomEnabled = enabled
    #endif
  }

  @objc public func setScrollEnabled(_ enabled: Bool) {
    #if RNC_PROVIDER_GOOGLE
    mapView.settings.scrollGestures = enabled
    #else
    mapView.isScrollEnabled = enabled
    #endif
  }

  @objc public func setShowsUserLocation(_ enabled: Bool) {
    #if RNC_PROVIDER_GOOGLE
    mapView.isMyLocationEnabled = enabled
    #else
    mapView.showsUserLocation = enabled
    #endif
  }

  @objc public func setClusteringEnabled(_ enabled: Bool, radius: Int32) {
    clusteringEnabled = enabled
    clusterRadius = radius
    rerenderMarkers()
  }

  @objc public func setProviderHint(_ hint: String) {
    providerHint = hint
  }

  @objc public func applyInitialRegion(latitude: Double,
                                       longitude: Double,
                                       latitudeDelta: Double,
                                       longitudeDelta: Double) {
    guard !initialRegionApplied,
          latitudeDelta > 0, longitudeDelta > 0 else { return }
    initialRegionApplied = true
    setRegion(lat: latitude, lon: longitude, latD: latitudeDelta, lonD: longitudeDelta, animated: false)
  }

  @objc public func applyControlledRegion(latitude: Double,
                                          longitude: Double,
                                          latitudeDelta: Double,
                                          longitudeDelta: Double) {
    guard latitudeDelta > 0, longitudeDelta > 0 else { return }
    setRegion(lat: latitude, lon: longitude, latD: latitudeDelta, lonD: longitudeDelta, animated: true)
  }

  @objc public func applyMarkers(_ markers: [[String: Any]]) {
    markersData = markers
    rerenderMarkers()
  }

  // MARK: - Internals

  private func setRegion(lat: Double, lon: Double, latD: Double, lonD: Double, animated: Bool) {
    #if RNC_PROVIDER_GOOGLE
    // Convert delta → zoom approximation.
    let zoom = Float(log2(360.0 / latD))
    let camera = GMSCameraPosition.camera(withLatitude: lat, longitude: lon, zoom: max(0, min(20, zoom)))
    if animated { mapView.animate(to: camera) } else { mapView.camera = camera }
    #else
    let region = MKCoordinateRegion(
      center: CLLocationCoordinate2D(latitude: lat, longitude: lon),
      span: MKCoordinateSpan(latitudeDelta: latD, longitudeDelta: lonD))
    mapView.setRegion(region, animated: animated)
    #endif
  }

  private func rerenderMarkers() {
    let visible = clusteringEnabled
      ? Self.cluster(markers: markersData, radiusDeg: max(0.001, Double(clusterRadius) / 1000.0))
      : markersData

    #if RNC_PROVIDER_GOOGLE
    for (_, m) in gmsMarkers { m.map = nil }
    gmsMarkers.removeAll(keepingCapacity: true)
    for raw in visible {
      guard let lat = raw["latitude"] as? Double,
            let lon = raw["longitude"] as? Double else { continue }
      let marker = GMSMarker(position: CLLocationCoordinate2D(latitude: lat, longitude: lon))
      marker.title = raw["title"] as? String
      marker.snippet = raw["description"] as? String
      let id = raw["identifier"] as? String ?? UUID().uuidString
      marker.userData = id
      if let b64 = raw["imageBase64"] as? String,
         !b64.isEmpty,
         let data = Data(base64Encoded: b64),
         let img = UIImage(data: data) {
        marker.icon = img
      }
      marker.map = mapView
      gmsMarkers[id] = marker
    }
    #else
    let existing = mapView.annotations.filter { !($0 is MKUserLocation) }
    mapView.removeAnnotations(existing)
    for raw in visible {
      guard let lat = raw["latitude"] as? Double,
            let lon = raw["longitude"] as? Double else { continue }
      let pin = RNCCustomAnnotation()
      pin.coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lon)
      pin.title = raw["title"] as? String
      pin.subtitle = raw["description"] as? String
      pin.identifier = raw["identifier"] as? String ?? UUID().uuidString
      if let b64 = raw["imageBase64"] as? String,
         !b64.isEmpty,
         let data = Data(base64Encoded: b64),
         let img = UIImage(data: data) {
        pin.image = img
      }
      mapView.addAnnotation(pin)
    }
    #endif
  }

  /// Naive grid clustering identical to the JS helper. Keeps the
  /// "first" marker in each cell as the representative; aggregates
  /// counts in the title.
  private static func cluster(markers: [[String: Any]], radiusDeg: Double) -> [[String: Any]] {
    var buckets: [String: [[String: Any]]] = [:]
    for m in markers {
      guard let lat = m["latitude"] as? Double,
            let lon = m["longitude"] as? Double else { continue }
      let key = "\(Int(lat / radiusDeg)):\(Int(lon / radiusDeg))"
      buckets[key, default: []].append(m)
    }
    var out: [[String: Any]] = []
    for (_, members) in buckets {
      if members.count == 1 {
        out.append(members[0])
      } else {
        var rep = members[0]
        rep["title"] = "\(members.count) markers"
        out.append(rep)
      }
    }
    return out
  }

  #if !RNC_PROVIDER_GOOGLE
  @objc private func handleMapTap(_ gr: UITapGestureRecognizer) {
    let pt = gr.location(in: mapView)
    let coord = mapView.convert(pt, toCoordinateFrom: mapView)
    eventDelegate?.emitMapPress(latitude: coord.latitude, longitude: coord.longitude)
  }
  #endif
}

// MARK: - Provider delegates

#if RNC_PROVIDER_GOOGLE
extension RNCCustomMapsViewImpl: GMSMapViewDelegate {
  public func mapView(_ mapView: GMSMapView, idleAt position: GMSCameraPosition) {
    eventDelegate?.emitRegionChange(latitude: position.target.latitude,
                                    longitude: position.target.longitude,
                                    latitudeDelta: 360.0 / pow(2.0, Double(position.zoom)),
                                    longitudeDelta: 360.0 / pow(2.0, Double(position.zoom)),
                                    isComplete: true)
  }
  public func mapView(_ mapView: GMSMapView, willMove gesture: Bool) {
    let p = mapView.camera.target
    eventDelegate?.emitRegionChange(latitude: p.latitude,
                                    longitude: p.longitude,
                                    latitudeDelta: 360.0 / pow(2.0, Double(mapView.camera.zoom)),
                                    longitudeDelta: 360.0 / pow(2.0, Double(mapView.camera.zoom)),
                                    isComplete: false)
  }
  public func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
    let id = (marker.userData as? String) ?? ""
    eventDelegate?.emitMarkerPress(identifier: id,
                                   latitude: marker.position.latitude,
                                   longitude: marker.position.longitude)
    return false
  }
  public func mapView(_ mapView: GMSMapView, didTapAt coordinate: CLLocationCoordinate2D) {
    eventDelegate?.emitMapPress(latitude: coordinate.latitude, longitude: coordinate.longitude)
  }
}
#else
final class RNCCustomAnnotation: NSObject, MKAnnotation {
  dynamic var coordinate: CLLocationCoordinate2D = .init(latitude: 0, longitude: 0)
  var title: String?
  var subtitle: String?
  var identifier: String = ""
  var image: UIImage?
}

extension RNCCustomMapsViewImpl: MKMapViewDelegate {
  public func mapView(_ mapView: MKMapView, regionWillChangeAnimated animated: Bool) {
    let r = mapView.region
    eventDelegate?.emitRegionChange(latitude: r.center.latitude,
                                    longitude: r.center.longitude,
                                    latitudeDelta: r.span.latitudeDelta,
                                    longitudeDelta: r.span.longitudeDelta,
                                    isComplete: false)
  }
  public func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
    let r = mapView.region
    eventDelegate?.emitRegionChange(latitude: r.center.latitude,
                                    longitude: r.center.longitude,
                                    latitudeDelta: r.span.latitudeDelta,
                                    longitudeDelta: r.span.longitudeDelta,
                                    isComplete: true)
  }
  public func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
    guard let pin = annotation as? RNCCustomAnnotation else { return nil }
    let id = "rnc-pin"
    let view = mapView.dequeueReusableAnnotationView(withIdentifier: id) as? MKMarkerAnnotationView
      ?? MKMarkerAnnotationView(annotation: pin, reuseIdentifier: id)
    view.annotation = pin
    view.canShowCallout = true
    if let img = pin.image {
      view.image = img
    }
    return view
  }
  public func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
    guard let pin = view.annotation as? RNCCustomAnnotation else { return }
    eventDelegate?.emitMarkerPress(identifier: pin.identifier,
                                   latitude: pin.coordinate.latitude,
                                   longitude: pin.coordinate.longitude)
  }
}
#endif
