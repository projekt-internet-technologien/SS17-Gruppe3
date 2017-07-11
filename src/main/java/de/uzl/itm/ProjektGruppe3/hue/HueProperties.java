package de.uzl.itm.ProjektGruppe3.hue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Class for storing user information of the philips hue system
 * @author Florian Winzek
 * @version 1.0.0
 */
public final class HueProperties {

    /**
     * ip address of the hue bridge
     */
    private static final String LAST_CONNECTED_IP   = "192.168.178.123";
    /**
     * whitelisted user of the hue bridge
     */
    private static final String USER_NAME           = "nrHdBi9OL7Sjh-6o2nX3UYEgDaIiaRsU03TgDyyy";
    /**
     * filename to store all information on the local system
     */
    private static final String PROPS_FILE_NAME     = "MyHue.properties";
    private static Properties props=null;

    /**
     * storing the current ip address
     * @param ipAddress ip address of the hue bridge
     */
    public static void storeLastIPAddress(String ipAddress) {
        props.setProperty(LAST_CONNECTED_IP, ipAddress);
        saveProperties();
    }

    /**
     * Stores the Username (for Whitelist usage). This is generated as a random 16 character string.
     */
    public static void storeUsername(String username) {
        props.setProperty(USER_NAME, username);
        saveProperties();
    }

    /**
     * Returns the stored Whitelist username.  If it doesn't exist we generate a 16 character random string and store this in the properties file.
     */
    public static String getUsername() {
        String username = props.getProperty(USER_NAME);
        return username;
    }

    /**
     * getter method of ip address field
     * @return current ip address
     */
    public static String getLastConnectedIP() {
        return props.getProperty(LAST_CONNECTED_IP);
    }

    /**
     * loads the current property file for the hue system
     */
    public static void loadProperties() {
        if (props==null) {
            props=new Properties();
            FileInputStream in;

            try {
                in = new FileInputStream(PROPS_FILE_NAME);
                props.load(in);
                in.close();
            } catch (FileNotFoundException ex) {
                saveProperties();
            } catch (IOException e) {
                // Handle the IOException.
            }
        }
    }

    /**
     * stores the current hue system properties in a file
     */
    public static void saveProperties() {
        try {
            FileOutputStream out = new FileOutputStream(PROPS_FILE_NAME);
            props.store(out, null);
            out.close();
        } catch (FileNotFoundException e) {
            // Handle the FileNotFoundException.
        } catch (IOException e) {
            // Handle the IOException.
        }
    }
}