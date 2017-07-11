package de.uzl.itm.ProjektGruppe3.hue;

import com.philips.lighting.hue.sdk.*;
import com.philips.lighting.model.*;

import java.util.List;
import java.util.Random;

/**
 * Philips Hue System
 *
 * Initiate a new hue controller which controls a hue light
 * @author Florian Winzek
 * @version 1.0.0
 */
public class HueSystem {
    /**
     * hue controller
     */
    private final HueController controller;

    /**
     * getter method
     * @return hue controller object
     */
    public HueController getController() {
        return controller;
    }

    /**
     * Creates a new Philips Hue
     */
    public HueSystem(){
        PHHueSDK phHueSDK = PHHueSDK.create();

        HueProperties.loadProperties();  // Load in HueProperties, if first time use a properties file is created.
        // Bind the Model and View
        controller = new HueController();

        phHueSDK.getNotificationManager().registerSDKListener(controller.getListener());
        controller.connectToLastKnownAccessPoint();
        if(phHueSDK.getSelectedBridge() == null) {
            controller.findBridges();
        }
    }

}