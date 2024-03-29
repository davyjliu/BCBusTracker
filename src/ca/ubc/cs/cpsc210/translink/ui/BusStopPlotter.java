package ca.ubc.cs.cpsc210.translink.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.R;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.Stop;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.HashMap;
import java.util.Map;

import static ca.ubc.cs.cpsc210.translink.util.Geometry.rectangleContainsPoint;

// A plotter for bus stop locations
public class BusStopPlotter extends MapViewOverlay {
    /**
     * clusterer
     */
    private RadiusMarkerClusterer stopClusterer;
    /**
     * maps each stop to corresponding marker on map
     */
    private Map<Stop, Marker> stopMarkerMap = new HashMap<>();
    /**
     * marker for stop that is nearest to user (null if no such stop)
     */
    private Marker nearestStnMarker;
    private Activity activity;
    private StopInfoWindow stopInfoWindow;

    /**
     * Constructor
     *
     * @param activity the application context
     * @param mapView  the map view on which buses are to be plotted
     */
    public BusStopPlotter(Activity activity, MapView mapView) {
        super(activity.getApplicationContext(), mapView);
        this.activity = activity;
        nearestStnMarker = null;
        stopInfoWindow = new StopInfoWindow((StopSelectionListener) activity, mapView);
        newStopClusterer();
    }

    public RadiusMarkerClusterer getStopClusterer() {
        return stopClusterer;
    }

    /**
     * Mark all visible stops in stop manager onto map.
     *  reason for the if null statement is because we don't want to replace
        the nearest marker with a new one every time the screen is moved.
        With if null, it will only produce new markers that did not exist before.
        TLDR: updateMarkerOfNearest is the only method that changes the marker icon.
     */
    public void markStops(Location currentLocation) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        updateAreaAndCreateStopCluster();

        for (Stop s : StopManager.getInstance()) {
            if (rectangleContainsPoint(northWest, southEast, s.getLocn())) {
                Marker newmarker = getMarker(s);
                if (newmarker == null) {
                    newmarker = new Marker(mapView);
                    StringBuilder newlineroutes = new StringBuilder();


                    setMarkerAndStop(newmarker, s);
                    parseRoutesAndAddToTitleAlongWithStopInfo(s, newmarker, newlineroutes);
                    newmarker.setIcon(stopIconDrawable);
                }
                stopClusterer.add(newmarker);
            }
        }
    }



    /**
     * Create a new stop cluster object used to group stops that are close by to reduce screen clutter
     */
    private void newStopClusterer() {
        stopClusterer = new RadiusMarkerClusterer(activity);
        stopClusterer.getTextPaint().setTextSize(20.0F * BusesAreUs.dpiFactor());
        int zoom = mapView == null ? 16 : mapView.getZoomLevel();
        if (zoom == 0) {
            zoom = MapDisplayFragment.DEFAULT_ZOOM;
        }
        int radius = 1000 / zoom;

        stopClusterer.setRadius(radius);
        Drawable clusterIconD = activity.getResources().getDrawable(R.drawable.stop_cluster);
        Bitmap clusterIcon = ((BitmapDrawable) clusterIconD).getBitmap();
        stopClusterer.setIcon(clusterIcon);
    }

    /**
     * Update marker of nearest stop (called when user's location has changed).  If nearest is null,
     * no stop is marked as the nearest stop.
     *
     * @param nearest stop nearest to user's location (null if no stop within StopManager.RADIUS metres)
     */
    public void updateMarkerOfNearest(Stop nearest) {
        Drawable stopIconDrawable = activity.getResources().getDrawable(R.drawable.stop_icon);
        Drawable closestStopIconDrawable = activity.getResources().getDrawable(R.drawable.closest_stop_icon);

        if (nearest == null) {
            resetNearestMarkerToNullAndOrgIcon(stopIconDrawable);
        } else {
            replaceNearestMarkerWithNewNearestStop(stopIconDrawable, nearest);
            if (nearestStnMarker == null) {
                nearestStnMarker = new Marker(mapView);
                StringBuilder newlineroutes = new StringBuilder();


                setMarkerAndStop(nearestStnMarker, nearest);
                parseRoutesAndAddToTitleAlongWithStopInfo(nearest, nearestStnMarker, newlineroutes);
                stopClusterer.add(nearestStnMarker);
            }
            nearestStnMarker.setIcon(closestStopIconDrawable);
        }
    }



    /**
     * Manage mapping from stops to markers using a map from stops to markers.
     * The mapping in the other direction is done using the Marker.setRelatedObject() and
     * Marker.getRelatedObject() methods.
     */
    private Marker getMarker(Stop stop) {
        return stopMarkerMap.get(stop);
    }

    private void setMarker(Stop stop, Marker marker) {
        stopMarkerMap.put(stop, marker);
    }

    private void clearMarker(Stop stop) {
        stopMarkerMap.remove(stop);
    }

    private void clearMarkers() {
        stopMarkerMap.clear();
    }


// HELPER METHODS for markStops


    //MODIFIES: This
    // EFFECTS: Parsing all routes to have it's own individual line
    private void parseRoutesToOneLine(Stop s, StringBuilder newstring) {
        for (Route r : s.getRoutes()) {
            newstring.append(r.getNumber());
            newstring.append("\n");
        }
    }

    //MODIFIES: This
    // EFFECTS: Adding stop number and name,along with routes to the marker to the title
    private void addComponenentsToTitle(Marker m, Stop s, StringBuilder newstring) {
        newstring.deleteCharAt(newstring.length() - 1);
        m.setTitle(s.getNumber() + " " + s.getName() + "\n" + newstring);
    }


    //MODIFIES: This
    // EFFECTS: Parsing and adding components to title
    private void parseRoutesAndAddToTitleAlongWithStopInfo(Stop s, Marker m, StringBuilder newstring) {
        parseRoutesToOneLine(s, newstring);
        addComponenentsToTitle(m, s, newstring);
    }

    // MODIFIES: This
    // EFFECTS: update Visible Area and Create new stop cluster
    private void updateAreaAndCreateStopCluster() {
        updateVisibleArea();
        newStopClusterer();
    }


    // MODIFIES: This
    // EFFECTS: Set marker and stop
    private void setMarkerAndStop(Marker m, Stop s) {
        setMarker(s, m);
        m.setRelatedObject(s);
        m.setInfoWindow(stopInfoWindow);
        m.setPosition(new GeoPoint(s.getLocn().getLatitude(), s.getLocn().getLongitude()));

    }

    // MODIFIES: This
    // EFFECTS: If nearest stop is not null, reset it to the normal stop icon,
    //         and make nearest stop null.
    public void resetNearestMarkerToNullAndOrgIcon(Drawable d) {
        if (nearestStnMarker != null) {
            nearestStnMarker.setIcon(d);
            nearestStnMarker = null;
        }
    }


    // MODIFIES: This
    // EFFECTS: If nearest stop is not null, reset it to the normal stop icon,
    //         and make nearest marker have nearest stop characteristics.

    public void replaceNearestMarkerWithNewNearestStop(Drawable d, Stop s) {
        if (nearestStnMarker != null) {
            nearestStnMarker.setIcon(d);
            nearestStnMarker = getMarker(s);
        }
    }


}
