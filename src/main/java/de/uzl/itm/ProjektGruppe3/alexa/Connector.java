package de.uzl.itm.ProjektGruppe3.alexa;

import com.amazon.alexa.avs.*;
import com.amazon.alexa.avs.auth.AuthSetup;
import com.amazon.alexa.avs.config.DeviceConfig;
import com.amazon.alexa.avs.config.DeviceConfigUtils;
import com.amazon.alexa.avs.http.AVSClientFactory;
import com.amazon.alexa.avs.wakeword.WakeWordIPCFactory;
import de.uzl.itm.ProjektGruppe3.alexa.auth.companionservice.RegCodeDisplayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Marco Buchholz on 06.06.17.
 */
public class Connector {
    private static final Logger log = LoggerFactory.getLogger(Connector.class);

    private AVSController controller;
    private AuthSetup authSetup;
    private ListenHandler listenHandler;

    public Connector() throws Exception {
        this(DeviceConfigUtils.readConfigFile());
    }

    public Connector(String configName) throws Exception {
        this(DeviceConfigUtils.readConfigFile(configName));
    }

    public Connector(DeviceConfig config) throws Exception {
        authSetup = new AuthSetup(config);
        controller = new AVSController(new AVSAudioPlayerFactory(), new AlertManagerFactory(),
            getAVSClientFactory(config), DialogRequestIdAuthority.getInstance(),
            new WakeWordIPCFactory(), config);
        listenHandler = new ListenHandlerImpl(controller);

        addListeners();
        startAuthentication(config);
        start();
    }

    protected AVSClientFactory getAVSClientFactory(DeviceConfig config) {
        return new AVSClientFactory(config);
    }


    private void startAuthentication(DeviceConfig config) {
        try {
            RegCodeDisplayHandler regCodeDisplayHandler =
                    new RegCodeDisplayHandler(config);
            authSetup.startProvisioningThread(regCodeDisplayHandler);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void addListeners() {
        authSetup.addAccessTokenListener(controller);
    }

    private void start() {
        controller.init(listenHandler);
        controller.startHandlingDirectives();
    }
}
