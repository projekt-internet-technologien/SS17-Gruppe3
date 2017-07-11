package de.uzl.itm.ProjektGruppe3.rfid;

import de.uzl.itm.ProjektGruppe3.hue.HueController;
import de.uzl.itm.ProjektGruppe3.hue.HueSystem;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Rfid Response Observer - receives available values from the ssp host by a given sparql query
 */
public class RfidResponseObserver implements Observer {
    /**
     * log4j
     */
    static Logger LOG = LoggerFactory.getLogger(RfidResponseObserver.class.getName());
    /**
     * philips hue
     */
    private HueSystem hueSystem;

    /**
     * creates a new rfidResponseObserver object
     * @param hueSystem
     */
    public RfidResponseObserver(HueSystem hueSystem) {
        this.hueSystem = hueSystem;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof List) { // generic type is String. Cannot be tested because of type erasure
            LinkedList responses = (LinkedList) arg;
            for (Object response :
                    responses) {
                String strJson = (String) response;
                JsonReader jsonReader = Json.createReader(new StringReader(strJson));
                JsonObject json = jsonReader.readObject();

                String xml = json.getString("results");

                try {
                    InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                    Document doc = new SAXBuilder().build( stream );
                    Element root = doc.getRootElement();
                    Namespace namespace = root.getNamespace();

                    Element results = root.getChild("results", namespace);
                    List<Element> resultList = results.getChildren();
                    if (HueController.HueConnected) {
                        hueSystem.getController().updateLight(!resultList.isEmpty());
                    };
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e.getStackTrace());
                }
            }
        }
    }
}
