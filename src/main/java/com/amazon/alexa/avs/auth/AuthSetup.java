/**
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Amazon Software License (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/asl/
 * <p>
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.amazon.alexa.avs.auth;

import com.amazon.alexa.avs.auth.companionservice.CompanionServiceAuthManager;
import com.amazon.alexa.avs.auth.companionservice.CompanionServiceClient;
import de.uzl.itm.ProjektGruppe3.alexa.auth.companionservice.RegCodeDisplayHandler;
import com.amazon.alexa.avs.config.DeviceConfig;
import com.amazon.alexa.avs.config.DeviceConfig.ProvisioningMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Initializes and owns the two ways to provision this device: via a companion service where this
 * device acts as a client, and via a companion application where this device acts as a server.
 */
public class AuthSetup implements AccessTokenListener {

    private static final Logger log = LoggerFactory.getLogger(AuthSetup.class);

    private final DeviceConfig deviceConfig;
    private final Set<AccessTokenListener> accessTokenListeners = new HashSet<>();

    /**
     * Creates an {@link AuthSetup} object.
     *
     * @param deviceConfig
     *            Information about this device.
     */
    public AuthSetup(final DeviceConfig deviceConfig) {
        this.deviceConfig = deviceConfig;
    }

    public void addAccessTokenListener(AccessTokenListener accessTokenListener) {
        accessTokenListeners.add(accessTokenListener);
    }

    /**
     * Initializes threads for the {@link CompanionServiceClient}.
     */
    public void startProvisioningThread(RegCodeDisplayHandler regCodeDisplayHandler) throws Exception {
        if (deviceConfig.getProvisioningMethod() == ProvisioningMethod.COMPANION_APP) {
            throw new Exception(String.format("Invalid configuration: Provisioning method %s is not supported by this app.",
                    ProvisioningMethod.COMPANION_APP));
        }

        CompanionServiceClient remoteProvisioningClient =
                new CompanionServiceClient(deviceConfig);
        final CompanionServiceAuthManager authManager = new CompanionServiceAuthManager(
                deviceConfig, remoteProvisioningClient, regCodeDisplayHandler, this);

        Thread provisioningThread = new Thread() {
            @Override
            public void run() {
                try {
                    authManager.startRemoteProvisioning();
                } catch (Exception e) {
                    if (e.getMessage().startsWith("InvalidSessionId")) {
                        log.error(
                                "Could not authenticate. Did you sign into Amazon before clicking ok?");
                    }
                    log.error("Failed to start companion service client", e);
                }
            }
        };
        provisioningThread.start();
    }

    @Override
    public void onAccessTokenReceived(String accessToken) {
        log.debug("Access token: " + accessToken);
        accessTokenListeners
                .stream()
                .forEach(listener -> listener.onAccessTokenReceived(accessToken));
    }
}
