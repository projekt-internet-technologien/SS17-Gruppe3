/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package de.uzl.itm.ProjektGruppe3.alexa.auth.companionservice;

import com.amazon.alexa.avs.config.DeviceConfig;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.Scanner;

public class RegCodeDisplayHandler {

    private final DeviceConfig deviceConfig;

    public RegCodeDisplayHandler(DeviceConfig deviceConfig) {
        this.deviceConfig = deviceConfig;
    }

    public void displayRegCode(String regCode) {
        String regUrl =
            deviceConfig.getCompanionServiceInfo().getServiceUrl() + "/provision/" + regCode;
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            boolean selected = yesOrNo(
                "Please register your device by visiting the following URL in "
                    + "a web browser and follow the instructions:\n" + regUrl
                    + "\n\n Would you like to open the URL automatically in your default browser?");
            if (selected) {
                try {
                    Desktop.getDesktop().browse(new URI(regUrl));
                } catch (Exception e) {
                    // Ignore and proceed
                }

                showInformation("If a browser window did not open, please copy and paste the below URL into a "
                        + "web browser, and follow the instructions:\n" + regUrl
                        + "\n\n Press enter to continue.");
            } else {
                handleAuthenticationCopyToClipboard(regUrl);
            }
        } else {
            handleAuthenticationCopyToClipboard(regUrl);
        }
    }

    private void handleAuthenticationCopyToClipboard(String regUrl) {
        boolean selected = yesOrNo("Please register your device by visiting the following URL in "
                                + "a web browser and follow the instructions:\n" + regUrl
                                + "\n\n Would you like the URL copied to your clipboard?");
        if (selected) {
            copyToClipboard(regUrl);
        }
        showInformation("Press Enter once you've authenticated with AVS");
    }

    private void copyToClipboard(String text) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Clipboard systemClipboard = defaultToolkit.getSystemClipboard();
        systemClipboard.setContents(new StringSelection(text), null);
    }

    private boolean yesOrNo(String message) throws IllegalArgumentException {
        System.out.println(message);
        Scanner scanner = new Scanner(System.in);

        if(scanner.next().equalsIgnoreCase("y")||scanner.next().equalsIgnoreCase("yes")) {
            return true;
        } else if(scanner.next().equalsIgnoreCase("n")||scanner.next().equalsIgnoreCase("no")) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid character");
        }
    }

    private void showInformation(String message) {
        System.out.println(message);
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }
}
