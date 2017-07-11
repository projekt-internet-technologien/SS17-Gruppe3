package de.uzl.itm.ProjektGruppe3;

import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.*;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Marco Buchholz on 16.05.17.
 */
@SuppressWarnings("Since15")
public abstract class ObservableBaseService<T> extends ObservableWebresource<T> {
    public static long DEFAULT_CONTENT_FORMAT = ContentFormat.APP_TURTLE;

    private ScheduledFuture periodicUpdateFuture;
    private int updateInterval;

    // This is to handle whether update requests are confirmable or not (remoteSocket -> MessageType)
    private HashMap<InetSocketAddress, Integer> observations = new HashMap<InetSocketAddress, Integer>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ObservableBaseService(String uriPath, T initialStatus, int updateInterval, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);

        //Set the update interval, i.e. the frequency of resource updates
        this.updateInterval = updateInterval;
        schedulePeriodicResourceUpdate();
    }

    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteAddress) {
        try {
            this.lock.readLock().lock();
            if (!this.observations.containsKey(remoteAddress)) {
                getLogger().error("This should never happen (no observation found for \"" + remoteAddress + "\")!");
                return false;
            } else {
                return this.observations.get(remoteAddress) == MessageType.CON;
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void removeObserver(InetSocketAddress remoteAddress) {
        try {
            this.lock.writeLock().lock();
            if (this.observations.remove(remoteAddress) != null) {
                getLogger().info("Observation canceled for remote socket \"" + remoteAddress + "\".");
            } else {
                getLogger().warn("No observation found to be canceled for remote socket \"remoteAddress\".");
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void updateEtag(T resourceStatus) {
        //nothing to do here as the ETAG is constructed on demand in the getEtag(long contentFormat) method
    }

    private void schedulePeriodicResourceUpdate() {
        this.periodicUpdateFuture = this.getExecutor().scheduleAtFixedRate(new Runnable() {

            public void run() {
                try {
                    setResourceStatus(getResourceValue(), updateInterval);
                    getLogger().info("New status of resource " + getUriPath() + ": " + getResourceStatus());
                } catch (Exception ex) {
                    getLogger().error("Exception while updating actual time...", ex);
                }
            }
        }, updateInterval, updateInterval, TimeUnit.SECONDS);
    }

    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) {
        try {
            if (coapRequest.getMessageCode() == MessageCode.GET) {
                processGet(responseFuture, coapRequest, remoteAddress);
            } else {
                CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
                        MessageCode.METHOD_NOT_ALLOWED_405);
                String message = "Service does not allow " + coapRequest.getMessageCodeName() + " requests.";
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
                responseFuture.set(coapResponse);
            }
        } catch (Exception ex) {
            responseFuture.setException(ex);
        }
    }


    private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                            InetSocketAddress remoteAddress) throws Exception {

        //create resource status
        WrappedResourceStatus resourceStatus;
        if (coapRequest.getAcceptedContentFormats().isEmpty()) {
            resourceStatus = getWrappedResourceStatus(DEFAULT_CONTENT_FORMAT);
        } else {
            resourceStatus = getWrappedResourceStatus(coapRequest.getAcceptedContentFormats());
        }

//        //Retrieve the accepted content formats from the request
//        Set<Long> contentFormats = coapRequest.getAcceptedContentFormats();
//
//        //If accept option is not set in the request, use the default (TEXT_PLAIN_UTF8)
//        if (contentFormats.isEmpty()) {
//            contentFormats.add(DEFAULT_CONTENT_FORMAT);
//        }
//
//        //Generate the payload of the response (depends on the accepted content formats, resp. the default
//        WrappedResourceStatus resourceStatus = null;
//        Iterator<Long> iterator = contentFormats.iterator();
//        long contentFormat = DEFAULT_CONTENT_FORMAT;
//
//        while(resourceStatus == null && iterator.hasNext()) {
//            contentFormat = iterator.next();
//            resourceStatus = getWrappedResourceStatus(contentFormat);
//        }

        CoapResponse coapResponse;

        if (resourceStatus != null) {
            //if the payload could be generated, i.e. at least one of the accepted content formats (according to the
            //requests accept option(s)) is offered by the Webservice then set payload and content format option
            //accordingly
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), resourceStatus.getContentFormat());

            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());

            // this is to accept the client as an observer
            if (coapRequest.getObserve() == 0) {
                coapResponse.setObserve();
                try {
                    this.lock.writeLock().lock();
                    this.observations.put(remoteAddress, coapRequest.getMessageType());
                } catch (Exception ex) {
                    getLogger().error("This should never happen!");
                } finally {
                    this.lock.writeLock().unlock();
                }
            }
        } else {
            //if no payload could be generated, i.e. none of the accepted content formats (according to the
            //requests accept option(s)) is offered by the Webservice then set the code of the response to
            //400 BAD REQUEST and set a payload with a proper explanation
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);

            StringBuilder payload = new StringBuilder();
            payload.append("Requested content format(s) (from requests ACCEPT option) not available: ");
            for (long acceptedContentFormat : coapRequest.getAcceptedContentFormats())
                payload.append("[").append(acceptedContentFormat).append("]");

            coapResponse.setContent(payload.toString().getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        }

        //Set the response future with the previously generated CoAP response
        responseFuture.set(coapResponse);
    }

    @Override
    public void shutdown() {
        // cancel the periodic update task
        getLogger().info("Shutdown service " + getUriPath() + ".");
        boolean futureCanceled = this.periodicUpdateFuture.cancel(true);
        getLogger().info("Future canceled: " + futureCanceled);
    }

    protected abstract Logger getLogger();

    protected abstract T getResourceValue();
}
