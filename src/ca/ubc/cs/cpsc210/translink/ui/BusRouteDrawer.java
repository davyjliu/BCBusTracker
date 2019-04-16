package ca.ubc.cs.cpsc210.translink.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import ca.ubc.cs.cpsc210.translink.BusesAreUs;
import ca.ubc.cs.cpsc210.translink.model.Route;
import ca.ubc.cs.cpsc210.translink.model.RouteManager;
import ca.ubc.cs.cpsc210.translink.model.RoutePattern;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.util.Geometry;
import ca.ubc.cs.cpsc210.translink.util.LatLon;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static ca.ubc.cs.cpsc210.translink.util.Geometry.gpFromLatLon;
import static ca.ubc.cs.cpsc210.translink.util.Geometry.rectangleContainsPoint;
import static ca.ubc.cs.cpsc210.translink.util.Geometry.rectangleIntersectsLine;

// A bus route drawer
public class BusRouteDrawer extends MapViewOverlay {
    /**
     * overlay used to display bus route legend text on a layer above the map
     */
    private BusRouteLegendOverlay busRouteLegendOverlay;
    /**
     * overlays used to plot bus routes
     */
    private List<Polyline> busRouteOverlays;

    private List<GeoPoint> geoPoints = new ArrayList<>();

    /**
     * Constructor
     *
     * @param context the application context
     * @param mapView the map view
     */
    public BusRouteDrawer(Context context, MapView mapView) {
        super(context, mapView);
        busRouteLegendOverlay = createBusRouteLegendOverlay();
        busRouteOverlays = new ArrayList<>();
    }

    /**
     * Plot each visible segment of each route pattern of each route going through the selected stop.
     *
     *
     *
     * We first initialize Area and clear routes to get ready for new selected stop.
     *
     * Then we find the routes regarding the selected stop and add all the route numbers that passes the selected stop.
     *
     * For each route, we acquire the path it takes through each RoutePattern.
     *
     * For each path, we find the latlon pts that exist within the rectangle using setWidthColorPointsAndAddPolyLine.
     *
     * The line is then drawn from the aggregation of polylines in busRouteOverlays.
     *
     */
    public void plotRoutes(int zoomLevel) {
        initializeAreaAndClearRouteLegend();
        if (StopManager.getInstance().getSelected() != null) {
            busRouteOverlays.clear();
            for (Route r : StopManager.getInstance().getSelected().getRoutes()) {
                busRouteLegendOverlay.add(r.getNumber());
                for (RoutePattern p : r.getPatterns()) {
                    for (int i = 0; i < p.getPath().size() - 1; i++) {
                        if (rectangleIntersectsLine(northWest, southEast, p.getPath().get(i), p.getPath().get(i + 1))) {
                            Polyline py = new Polyline(context);
                            setWidthColorPointsAndAddPolyLine(py, zoomLevel, r, p.getPath(), i);
                        }
                    }
                }
            }
        }
    }



    public List<Polyline> getBusRouteOverlays() {
        return Collections.unmodifiableList(busRouteOverlays);
    }

    public BusRouteLegendOverlay getBusRouteLegendOverlay() {
        return busRouteLegendOverlay;
    }


    /**
     * Create text overlay to display bus route colours
     */
    private BusRouteLegendOverlay createBusRouteLegendOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(context);
        return new BusRouteLegendOverlay(rp, BusesAreUs.dpiFactor());
    }

    /**
     * Get width of line used to plot bus route based on zoom level
     *
     * @param zoomLevel the zoom level of the map
     * @return width of line used to plot bus route
     */
    private float getLineWidth(int zoomLevel) {
        if (zoomLevel > 14) {
            return 7.0f * BusesAreUs.dpiFactor();
        } else if (zoomLevel > 10) {
            return 5.0f * BusesAreUs.dpiFactor();
        } else {
            return 2.0f * BusesAreUs.dpiFactor();
        }
    }


    // HELPER METHODS



    // MODIFIES: This
    // EFFECTS: Update visible area and clear Route Legend

    private void initializeAreaAndClearRouteLegend() {
        updateVisibleArea();
        busRouteLegendOverlay.clear();
    }

    // MODIFIES: This
    // EFFECTS: Sets the width, color and points of a polyline
    //     **Geopoints are set from A(0) ---> B(1), B(1)---->C(2),
    //     * if the recentangle intersects the line, such as C(2)--|-D(3) then C(2) to D(3) is not added
    //     * These points are then added to busRouteOverlays.
    //     *


    private void setWidthColorPointsAndAddPolyLine(Polyline py, int zoomLevel, Route r, List<LatLon> latlons, int idx) {
        py.setWidth(getLineWidth(zoomLevel));
        py.setColor(busRouteLegendOverlay.getColor(r.getNumber()));
        geoPoints.add(gpFromLatLon(latlons.get(idx)));
        geoPoints.add(gpFromLatLon((latlons.get(idx + 1))));
        py.setPoints(geoPoints);
        busRouteOverlays.add(py);
        geoPoints.clear();
    }



}
