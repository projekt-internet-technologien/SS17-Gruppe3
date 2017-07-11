package de.uzl.itm.ProjektGruppe3.rfid;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;

/**
 * Class for rfid data transmission - receives rfid sensor values
 * @author Florian Winzek
 * @version 1.0.0
 */
public class RfidObserver implements Observer{
    /**
     * log4j
     */
    private static Logger LOG = LoggerFactory.getLogger(RfidObserver.class.getName());
    /**
     * rfidService
     */
    private final ObservableRfidService rfidService;
    /**
     * current rfid data value
     */
    private String tmpValue = "";

    /**
     * creates a new RfidObserver object
     * @param rfidService rfidService
     */
    public RfidObserver(ObservableRfidService rfidService){
        this.rfidService = rfidService;
    }

    @Override
    public void update(Observable o, Object arg) {
        if(arg instanceof byte[]){
            byte[] bytes = (byte[]) arg;
            String current = new String(bytes);
            if (current.contains("#")) {
                int pos = current.indexOf('#');
                tmpValue += current.substring(0, pos).trim();
                try {
                    if(!rfidService.getCurrentEntries().contains(tmpValue)){
                        rfidService.getCurrentEntries().add(tmpValue);
                    }else {
                        rfidService.getCurrentEntries().remove(tmpValue);
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }

                tmpValue = current.substring(pos + 1);
            } else {
                tmpValue += current;
            }
        }
    }
}
