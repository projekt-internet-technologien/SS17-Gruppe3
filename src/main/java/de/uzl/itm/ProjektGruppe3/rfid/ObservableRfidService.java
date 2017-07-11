package de.uzl.itm.ProjektGruppe3.rfid;

import com.google.common.primitives.Longs;
import de.uzl.itm.ProjektGruppe3.ObservableBaseService;
import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.*;

/**
 * Class for sending Rfid information to the ssp host
 * @author Florian Winzek
 * @version 1.0.0
 */
public class ObservableRfidService extends ObservableBaseService<List<String>> {
    /**
     * log4j
     */
    private static Logger LOG = Logger.getLogger(ObservableRfidService.class.getName());
    /**
     * template for the sparql query to send data to the ssp host
     */
    private static HashMap<Long, String> payloadTemplates = new HashMap<>();
    /**
     * data storage of the rfid transmission
     */
    private List<String> currentEntries = new ArrayList<>();

    static {
        payloadTemplates.put(
                ContentFormat.APP_TURTLE,
                    "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                    "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                    "@prefix pit: <https://pit.itm.uni-luebeck.de/> .\n" +
                    "@prefix itm: <http://gruppe03.pit.itm.uni-luebeck.de/> .\n" +
                    "itm:Pi rdf:type pit:Device ;\n" +
                    "\t pit:hasIp \"141.83.175.234\"^^xsd:string ;\n" +
                    "\t pit:hasGroup \"PIT_03-SS17\"^^xsd:string ;\n" +
                    "\t pit:hasComponent itm:Rfid .\n" +
                    "itm:Rfid rdf:type pit:Component ;\n" +
                    "\t pit:isType \"Rfid\"^^xsd:string ;\n" +
                    "\t pit:lastModified \"%s\"^^xsd:dateTime ;\n" +
                    "\t pit:isActor \"false\"^^xsd:boolean ;\n" +
                    "\t pit:hasURL \"coap://141.83.175.234:5683/rfid\"^^xsd:anyURI ;\n" +
                    "\t pit:hasDescription \"Radio-frequency identification\"^^xsd:string .\n"+
                    "%s"
        );
    }

    /**
     * creates a new ObservableService object for rfid data
     */
    public ObservableRfidService(String uriPath, int updateInterval, ScheduledExecutorService executor){
        super(uriPath, new ArrayList<>(), updateInterval, executor);

        Set<Long> keys = payloadTemplates.keySet();
        Long[] array = keys.toArray(new Long[keys.size()]);

        // Convert to "1 3 45"
        String[] values = new String[keys.size()];
        for (int i = 0; i < array.length; i++) {
            values[i] = array[i].toString();
        }

        //Sets the link attributes for supported content types ('ct')
        String ctValue = "\"" + String.join(" ", values) + "\"";
        this.setLinkParam(LinkParam.createLinkParam(CT, ctValue));

        //Sets the link attribute to give the resource a title
        String title = "\"Rfid (updated every " + updateInterval + " seconds)\"";
        this.setLinkParam(LinkParam.createLinkParam(TITLE, title));

        //Sets the link attribute for the resource type ('rt')
        String rtValue = "\"rfid\"";
        this.setLinkParam(LinkParam.createLinkParam(RT, rtValue));

        //Sets the link attribute for max-size estimation ('sz')
        this.setLinkParam(LinkParam.createLinkParam(SZ, "" + 100L));

        //Sets the link attribute for interface description ('if')
        String ifValue = "\"GET only\"";
        this.setLinkParam(LinkParam.createLinkParam(IF, ifValue));
    }
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    protected List<String> getResourceValue() {
        return currentEntries;
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return Longs.toByteArray(getResourceStatus().hashCode() | (contentFormat << 56));
    }

    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        LOG.debug("Try to create payload (content format: " + contentFormat + ")");
        StringBuilder sb = new StringBuilder();
        for(String name : getCurrentEntries()){
            sb.append("itm:Rfid pit:hasStatus itm:"+ name + " .\n");
        }
        for(String name : getCurrentEntries()){
            sb.append("itm:" + name + " rdf:type pit:Status;\n" +
                    "\t pit:hasScaleUnit \"Name\" ;\n" +
                    "\t pit:hasValue \""+ name +"\"^^xsd:string .\n");
        }
        String template = payloadTemplates.get(contentFormat);
        if (template == null)
            return null;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime date = LocalDateTime.now();
        String text = date.format(formatter);
        LocalDateTime parsedDate = LocalDateTime.parse(text, formatter);

        return String.format(template, parsedDate, sb.toString()).getBytes(CoapMessage.CHARSET);
    }

    /**
     * getter method for field currentEntries
     * @return list with current entries of the rfid data transmission
     */
    public List<String> getCurrentEntries() {
        return currentEntries;
    }
}
