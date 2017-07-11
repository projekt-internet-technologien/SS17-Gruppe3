package de.uzl.itm.ProjektGruppe3;

import de.uzl.itm.LoggingConfiguration;
import de.uzl.itm.ProjektGruppe3.alexa.Connector;
import de.uzl.itm.ProjektGruppe3.alexa.skill.Launcher;
import de.uzl.itm.ProjektGruppe3.hue.HueSystem;
import de.uzl.itm.ProjektGruppe3.iMirror.IMirrorResponseObserver;
import de.uzl.itm.ProjektGruppe3.rfid.ObservableRfidService;
import de.uzl.itm.ProjektGruppe3.rfid.RfidObserver;
import de.uzl.itm.ProjektGruppe3.rfid.RfidResponseObserver;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application
 *
 * @author Florian Winzek, Max Golubew, Marco Buchholz
 */
public class App {
    @Option(name = "--ports", usage = "Set USB ports")
    private String ports = null;

    @Option(name = "--rxtxlib", usage = "Set RXTX lib")
    private String rxtxlib = "/usr/lib/jni";

    @Option(name = "--baud", usage = "Set baud rate")
    private int baud = 115200;

    @Option(name = "--host", usage = "Host of the SSP (ip or domain)")
    private String SSP_HOST = "141.83.151.196";

    @Option(name = "--port", usage = "Port of the SSP")
    private int SSP_PORT = 5683;

    @Option(name = "--http-port", usage = "HTTP port of the SSP")
    private int SSP_HTTP_PORT = 8080;

    private static Logger LOG = LoggerFactory.getLogger(App.class.getName());

    /**
     * Initilizes the RxTx library and pin shutdown options
     *
     * @param args
     * @throws InterruptedException
     */
    public App(String[] args) throws InterruptedException {

        HueSystem hue = new HueSystem();
        EndpointHandler handler;
        SerialCommunicator serialCommunicator;
        CmdLineParser parser = new CmdLineParser(this);

        String queryRfid = "PREFIX pit: <https://pit.itm.uni-luebeck.de/>\n" +
                "PREFIX itm: <http://gruppe03.pit.itm.uni-luebeck.de/>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "\n" +
                "SELECT * WHERE{\n" +
                "itm:Rfid pit:hasStatus ?status.\n" +
                "}";

        String queryIMirror = "PREFIX pit: <https://pit.itm.uni-luebeck.de/>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "\n" +
                "SELECT * WHERE {\n" +
                " ?component pit:isType \"Camera\"^^xsd:string .\n" +
                " ?component pit:hasStatus ?status .\n" +
                " ?status pit:hasValue ?value .\n" +
                " ?status pit:hasScaleUnit \"Name\"^^xsd:string .\n" +
                "}";

        // start sparqlCtrl for RFID
        SparqlController sparqlRfidController = new SparqlController(SSP_HOST, "" + SSP_HTTP_PORT, 1,
                queryRfid);
        sparqlRfidController.schedulePeriodicRequest();
        sparqlRfidController.addObserver(new RfidResponseObserver(hue));

        // start sparqlCtrl for IMIRROR
        SparqlController sparqlIMirrorController = new SparqlController(SSP_HOST, "" + SSP_HTTP_PORT, 1,
                queryIMirror);
        sparqlIMirrorController.schedulePeriodicRequest();
        sparqlIMirrorController.addObserver(new IMirrorResponseObserver(hue));

        try {
            parser.parseArgument(args);
            handler = new EndpointHandler(SSP_HOST, SSP_PORT);

            Runnable skill = () -> {
                try {
                    new Launcher(SSP_HOST, SSP_HTTP_PORT); // starts the alexa skill jetty server
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            new Thread(skill).start();

            ObservableRfidService rfidService = new ObservableRfidService("/rfid", 4, handler.getExecutor());
            handler.registerWebresource(rfidService);

            serialCommunicator = new SerialCommunicator(baud, ports, rxtxlib);

            boolean success = serialCommunicator.registerObserver("Rfid", new RfidObserver(rfidService));
            if (success) {
                serialCommunicator.startAll();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            parser.printUsage(System.err);
        }

        // keep program running until user aborts (CTRL-C)
        while (true) {
            Thread.sleep(500);
        }
    }

    public static void main(String[] args) throws Exception {
        // configure logging
        LoggingConfiguration.configureDefaultLogging();

        // start AVS
        new Connector();

        new App(args);

    }
}
