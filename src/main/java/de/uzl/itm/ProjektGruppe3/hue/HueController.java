package de.uzl.itm.ProjektGruppe3.hue;

import java.util.List;
import java.util.Random;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for controlling the Philips Hue System
 * @author Florian Winzek
 * @version 1.0.0
 */
public class HueController {
    /**
     * hue sdk
     */
    private PHHueSDK phHueSDK;
    /**
     * max light value for the hue bulb
     */
    private static final int MAX_HUE=65535;
    /**
     * log4j
     */
    private static Logger LOG = LoggerFactory.getLogger(HueController.class);
    /**
     * status of the hue connection
     */
    public static boolean HueConnected = false;

    /**
     * creates a new hue controller
     */
    public HueController() {
        this.phHueSDK = PHHueSDK.getInstance();
    }

    /**
     * search for an available hue bridge
     */
    public void findBridges() {
        phHueSDK = PHHueSDK.getInstance();
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        sm.search(true, true);
    }

    /**
     * implementation of the hue sdk listener
     */
    private PHSDKListener listener = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPointsList) {
            if(!accessPointsList.isEmpty()){
                phHueSDK.connect(accessPointsList.get(0));
            }
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            // Start the Pushlink Authentication.
            phHueSDK.startPushlinkAuthentication(accessPoint);
        }

        @Override
        public void onBridgeConnected(PHBridge bridge, String username) {
            phHueSDK.setSelectedBridge(bridge);
            phHueSDK.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);
            String lastIpAddress =  bridge.getResourceCache().getBridgeConfiguration().getIpAddress();
            HueProperties.storeUsername(username);
            HueProperties.storeLastIPAddress(lastIpAddress);
            HueProperties.saveProperties();
            HueConnected = true;
            LOG.info("Bridge connected");
        }

        @Override
        public void onCacheUpdated(List<Integer> arg0, PHBridge arg1) {
        }

        @Override
        public void onConnectionLost(PHAccessPoint arg0) {
        }

        @Override
        public void onConnectionResumed(PHBridge arg0) {
        }

        @Override
        public void onError(int code, final String message) {

            if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
               LOG.error("Bridge not responding");
            }
            else if (code == PHMessageType.PUSHLINK_BUTTON_NOT_PRESSED) {
                LOG.info("Press Pushlink Button");
            }
            else if (code == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {
                LOG.error(message);
            }
            else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
                LOG.error("Bridge not found");
            }
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
            for (PHHueParsingError parsingError: parsingErrorsList) {
                LOG.error("ParsingError : " + parsingError.getMessage());
            }
        }
    };

    /**
     * getter method of hue listener
     * @return hue listener object
     */
    public PHSDKListener getListener() {
        return listener;
    }

    /**
     * updates all available lights from the connected hue bridge
     * @param on on/off parameter
     */
    public void updateLight(boolean on) {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        PHBridgeResourcesCache cache = bridge.getResourceCache();

        List<PHLight> allLights = cache.getAllLights();

        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setOn(on);
            bridge.updateLightState(light, lightState); // If no bridge response is required then use this simpler form.
        }
    }
    /**
     * updates all available lights from the connected hue bridge
     */
    public void updateLight(int bulbValue) {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        PHBridgeResourcesCache cache = bridge.getResourceCache();

        List<PHLight> allLights = cache.getAllLights();

        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(bulbValue);
            bridge.updateLightState(light, lightState); // If no bridge response is required then use this simpler form.
        }
    }

    /**
     * Connect to the last known access point.
     * This method is triggered by the Connect to Bridge button but it can equally be used to automatically connect to a bridge.
     *
     */
    public boolean connectToLastKnownAccessPoint() {
        String username = HueProperties.getUsername();
        String lastIpAddress =  HueProperties.getLastConnectedIP();

        if (username==null || lastIpAddress == null) {
            LOG.info("Missing Last Username or Last IP.  Last known connection not found.");
            return false;
        }
        PHAccessPoint accessPoint = new PHAccessPoint();
        accessPoint.setIpAddress(lastIpAddress);
        accessPoint.setUsername(username);
        phHueSDK.connect(accessPoint);
        return true;
    }
}