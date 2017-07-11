package de.uzl.itm.ProjektGruppe3.iMirror;

import de.uzl.itm.ProjektGruppe3.hue.HueController;
import de.uzl.itm.ProjektGruppe3.hue.HueSystem;
import org.apache.commons.lang3.StringEscapeUtils;
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
import java.util.*;

/**
 * Class for controlling the hue system by receiving data from a intelligent mirror.
 * The mirror sends the name and an uuid to the ssp host so that the IMirrorResponseObserver
 * can switch the hue light depending on the name
 */
public class IMirrorResponseObserver implements Observer{
    /**
     * log4j
     */
    static Logger LOG = LoggerFactory.getLogger(IMirrorResponseObserver.class.getName());
    private static final int defaultBulbValue = 12750;
    private static final Map<String, Integer> persons = new HashMap<>();
    static {
        persons.put("Sebastian", 25500);
        persons.put("Daniel", 46920);
        persons.put("Fritzi", 56100);
        persons.put("Raphael", 30000);
    }
    /**
     * philips hue
     */
    HueSystem hueSystem;

    /**
     * creates a new rfidResponseObserver object
     * @param hueSystem
     */
    public IMirrorResponseObserver(HueSystem hueSystem) {
        this.hueSystem = hueSystem;
    }

    @Override
    public void update(Observable o, Object arg) {
        String name = "";
        if (arg instanceof List) {
            LinkedList responses = (LinkedList) arg;
            LOG.debug(StringEscapeUtils.unescapeJava(responses.toString()));

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
                    LOG.debug(resultList.toString());
//                    if (HueController.HueConnected && !resultList.isEmpty()) {
                        for(int i = 0; i < resultList.size(); i++) {
                            Element result = resultList.get(i);
                            for(Element binding : result.getChildren("binding", namespace)) {
                                if(binding.getAttribute("name").getValue().equals("value")){
                                    Element literal = binding.getChild("literal", namespace);
                                    name = literal.getText();

                                    if(!name.equals("unknown") && persons.get(name) != null){
                                        LOG.debug("Name: {}", name);
                                        LOG.debug("Hue value: {}", persons.get(name));
                                        hueSystem.getController().updateLight(persons.get(name));
                                    }else{
                                        hueSystem.getController().updateLight(defaultBulbValue);
                                    }
                                }
                            }
                        }
//                    }

                } catch (Exception e) {
                    LOG.error(e.getMessage(), e.getStackTrace());
                }
            }
        }
    }
}
