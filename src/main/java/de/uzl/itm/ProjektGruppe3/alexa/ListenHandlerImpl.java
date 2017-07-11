/**
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.uzl.itm.ProjektGruppe3.alexa;

import com.amazon.alexa.avs.AVSController;
import com.amazon.alexa.avs.ListenHandler;
import com.amazon.alexa.avs.RecordingRMSListener;
import com.amazon.alexa.avs.RequestListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class ListenHandlerImpl implements ListenHandler {
    private static final Logger log = LoggerFactory.getLogger(ListenHandlerImpl.class);

    private volatile ListeningState listeningState;
    private AVSController controller;

    public ListenHandlerImpl(AVSController controller) {
        super();
        ClassLoader resLoader = Thread.currentThread().getContextClassLoader();
        this.controller = controller;
        listeningState = ListeningState.START;
    }

    private void startOrStopListening() {
        controller.onUserActivity();

        if (listeningState == ListeningState.START) { // if in idle mode
            listeningState = ListeningState.STOP;
            controller.startRecording(null, new SpeechRequestListener());
        } else { // else we must already be in listening
            listeningState = ListeningState.PROCESSING;
            // stop the recording so the request can complete
            controller.stopRecording();
        }
    }

    private class SpeechRequestListener extends RequestListener {

        @Override
        public void onRequestFinished() {
            // In case we get a response from the server without
            // terminating the stream ourselves.
            if (listeningState == ListeningState.STOP) {
                startOrStopListening();
            }
            finishProcessing();
        }

        @Override
        public void onRequestError(Throwable e) {
            log.error("An error occured creating speech request", e);
            startOrStopListening();
            finishProcessing();
        }
    }

    /**
     * Handles functional logic to wrap up a speech request
     */
    private void finishProcessing() {
        listeningState = ListeningState.START;
        controller.processingFinished();
    }

    @Override
    public void onStopCaptureDirective() {
        if (listeningState == ListeningState.STOP) {
            startOrStopListening();
        }
    }

    @Override
    public void onExpectSpeechDirective() {
        Thread thread =
                new Thread(() -> {
                    while (listeningState != ListeningState.START
                            || controller.isSpeaking()) {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                        }
                    }
                });
        thread.start();
    }

    @Override
    public synchronized void onWakeWordDetected() {
        if (listeningState == ListeningState.START) { // if in idle mode
            log.info("Wake Word was detected");
            startOrStopListening();
        }
    }

    private enum ListeningState {
        START,
        STOP,
        PROCESSING;
    }
}
