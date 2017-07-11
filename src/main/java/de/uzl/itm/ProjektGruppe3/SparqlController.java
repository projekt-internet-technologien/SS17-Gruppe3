package de.uzl.itm.ProjektGruppe3;

import de.dennisboldt.RestClient;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Marco Buchholz on 22.05.17.
 */
public class SparqlController extends Observable {
    private static Logger LOG = Logger.getLogger(SparqlController.class.getName());
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final String host;
    private final String port;
    private final long updateInterval;
    private final String query;
    private String accept = "application/xml";

    public SparqlController(String host, String port, int updateInterval, String query) {
        this.host = host;
        this.port = port;
        this.updateInterval = updateInterval;
        this.query = query;
    }

    public void schedulePeriodicRequest() {
        scheduler.scheduleAtFixedRate(() -> {
            List<String> result = request(query);
            setChanged();
            LOG.debug(StringEscapeUtils.unescapeJava(result.toString()));

            notifyObservers(result);
        }, updateInterval, updateInterval, TimeUnit.SECONDS);
    }

    private LinkedList<String> request(String sparql) {
        RestClient client = new RestClient(host, port, sparql, accept);

        try {
            return client.getResult();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }
}
