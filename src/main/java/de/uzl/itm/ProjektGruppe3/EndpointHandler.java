package de.uzl.itm.ProjektGruppe3;

import de.uzl.itm.SimpleCallback;
import de.uzl.itm.ncoap.application.endpoint.CoapEndpoint;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.communication.blockwise.BlockSize;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Marco Buchholz on 15.05.17.
 */
class EndpointHandler extends CoapEndpoint {
    private String SSP_HOST;
    private int SSP_PORT;

    /**
     * @param SSP_HOST host of the SSP
     * @param SSP_PORT port of the SSP
     */
    public EndpointHandler(String SSP_HOST, int SSP_PORT) throws URISyntaxException {
        super(BlockSize.SIZE_64, BlockSize.SIZE_64);
        this.SSP_HOST = SSP_HOST;
        this.SSP_PORT = SSP_PORT;
        registerAtSSP();
    }

    private void registerSimpleObservableResource(ObservableWebresource resssource) {
        // register resource at server
        this.registerWebresource(resssource);
    }

    private void registerAtSSP() throws URISyntaxException {

        URI resourceURI = new URI ("coap", null, SSP_HOST, SSP_PORT, "/registry", null, null);
        System.out.println(resourceURI.toString());
        CoapRequest coapRequest = new CoapRequest(MessageType.CON, MessageCode.POST, resourceURI);
        InetSocketAddress remoteSocket = new InetSocketAddress(SSP_HOST, SSP_PORT);

        SimpleCallback callback = new SimpleCallback();
        this.sendCoapRequest(coapRequest, remoteSocket, callback);
    }
}
