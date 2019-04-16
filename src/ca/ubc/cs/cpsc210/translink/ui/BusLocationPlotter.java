package ca.ubc.cs.cpsc210.translink.ui;

import android.content.Context;
import ca.ubc.cs.cpsc210.translink.R;
import ca.ubc.cs.cpsc210.translink.model.Bus;
import ca.ubc.cs.cpsc210.translink.model.Stop;
import ca.ubc.cs.cpsc210.translink.model.StopManager;
import ca.ubc.cs.cpsc210.translink.parsers.BusParser;
import ca.ubc.cs.cpsc210.translink.providers.HttpBusLocationDataProvider;
import org.json.JSONException;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.IOException;
import java.util.ArrayList;

// A plotter for bus locations
public class BusLocationPlotter extends MapViewOverlay {
    /**
     * overlay used to display bus locations
     */
    private ItemizedIconOverlay<OverlayItem> busLocationsOverlay;

    private HttpBusLocationDataProvider hbldp;

    /**
     * Constructor
     *
     * @param context the application context
     * @param mapView the map view
     */

    String data;
    Stop selected;

    public BusLocationPlotter(Context context, MapView mapView) {
        super(context, mapView);
        busLocationsOverlay = createBusLocnOverlay();


    }

    public ItemizedIconOverlay<OverlayItem> getBusLocationsOverlay() {
        return busLocationsOverlay;
    }

    /**
     * Plot buses serving selected stop
     */
    public void plotBuses() {
        selected = StopManager.getInstance().getSelected();
        hbldp = new HttpBusLocationDataProvider(selected);

        if (selected != null) {
            busLocationsOverlay.removeAllItems();

            try {
                data = hbldp.dataSourceToString();
                BusParser.parseBuses(selected, data);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException("Can't read the arrivals data");
            }
            plotBusesOnMap();
        }
    }


    /**
     * Create the overlay for bus markers.
     */
    private ItemizedIconOverlay<OverlayItem> createBusLocnOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(context);

        return new ItemizedIconOverlay<OverlayItem>(
                new ArrayList<OverlayItem>(),
                context.getResources().getDrawable(R.drawable.bus),
                null, rp);
    }

    // HELPER METHODS:

    // MODIFIES: This
    // EFFECTS: Plot buses onto map regarding selected stop

    private void plotBusesOnMap() {
        for (Bus b : selected.getBuses()) {
            GeoPoint geoPoint = new GeoPoint(b.getLatLon().getLatitude(), b.getLatLon().getLongitude());
            OverlayItem newbus = new OverlayItem(b.getRoute().getNumber(), "", geoPoint);
            busLocationsOverlay.addItem(newbus);
        }
    }
}
