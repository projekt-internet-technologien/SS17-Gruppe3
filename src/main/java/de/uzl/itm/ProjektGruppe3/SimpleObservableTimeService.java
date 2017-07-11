/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * <p>
 * - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * <p>
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * <p>
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uzl.itm.ProjektGruppe3;

import com.google.common.primitives.Longs;
import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.options.ContentFormat;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.*;

/**
 * This {@link de.uzl.itm.ncoap.application.server.resource.Webresource} updates on a regular basis and provides
 * the current UTC-time.
 *
 * @author Oliver Kleine
 */
@SuppressWarnings("Since15")
public class SimpleObservableTimeService extends ObservableBaseService<Long> {
    private static Logger LOG = Logger.getLogger(SimpleObservableTimeService.class.getName());
    private static HashMap<Long, String> payloadTemplates = new HashMap<Long, String>();

    static {
        //Add template for plaintext UTF-8 payload
        payloadTemplates.put(
                ContentFormat.TEXT_PLAIN_UTF8,
                "The current time is %02d:%02d:%02d"
        );

        //Add template for XML payload
        payloadTemplates.put(
                ContentFormat.APP_XML,
                "<time>\n" + "\t<hour>%02d</hour>\n" + "\t<minute>%02d</minute>\n" + "\t<second>%02d</second>\n</time>"
        );

        payloadTemplates.put(
                ContentFormat.APP_TURTLE,
                "@prefix itm: <http://gruppe03.pit.itm.uni-luebeck.de/>\n" +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "\n" +
                        "itm:time1 itm:hour \"%02d\"^^xsd:integer .\n" +
                        "itm:time1 itm:minute \"%02d\"^^xsd:integer .\n" +
                        "itm:time1 itm:seconds \"%02d\"^^xsd:integer ."
        );
    }

    /**
     * Creates a new instance of {@link SimpleObservableTimeService}.
     *
     * @param path the path of this {@link SimpleObservableTimeService} (e.g. /utc-time)
     * @param updateInterval the interval (in seconds) for resource status updates (e.g. 5 for every 5 seconds).
     */
    public SimpleObservableTimeService(String path, int updateInterval, ScheduledExecutorService executor) {
        super(path, System.currentTimeMillis(), updateInterval, executor);

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
        String title = "\"UTC time (updated every " + updateInterval + " seconds)\"";
        this.setLinkParam(LinkParam.createLinkParam(TITLE, title));

        //Sets the link attribute for the resource type ('rt')
        String rtValue = "\"time\"";
        this.setLinkParam(LinkParam.createLinkParam(RT, rtValue));

        //Sets the link attribute for max-size estimation ('sz')
        this.setLinkParam(LinkParam.createLinkParam(SZ, "" + 100L));

        //Sets the link attribute for interface description ('if')
        String ifValue = "\"GET only\"";
        this.setLinkParam(LinkParam.createLinkParam(IF, ifValue));
    }

    public byte[] getSerializedResourceStatus(long contentFormat) {
        LOG.debug("Try to create payload (content format: " + contentFormat + ")");

        String template = payloadTemplates.get(contentFormat);
        if (template == null) {
            return null;
        } else {
            long time = getResourceStatus() % 86400000;
            long hours = time / 3600000;
            long remainder = time % 3600000;
            long minutes = remainder / 60000;
            long seconds = (remainder % 60000) / 1000;
            return String.format(template, hours, minutes, seconds).getBytes(CoapMessage.CHARSET);
        }
    }

    public byte[] getEtag(long contentFormat) {
        return Longs.toByteArray(getResourceStatus() | (contentFormat << 56));
    }

    protected Logger getLogger() {
        return LOG;
    }

    protected Long getResourceValue() {
        return System.currentTimeMillis();
    }
}
