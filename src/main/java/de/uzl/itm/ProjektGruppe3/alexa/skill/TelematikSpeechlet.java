/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package de.uzl.itm.ProjektGruppe3.alexa.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

/**
 * This Alexa Skill queries the Smart Service Proxie from the itm at uni Lübeck
 * and answers with the status of a given component.
 */
public class TelematikSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(TelematikSpeechlet.class);
    private final Status status;

    /**
     * The key to get the item from the intent.
     */
    private static final String TYPE_SLOT = "Type";

    public TelematikSpeechlet(String host, int port) {
        status = new Status(host, port);
    }

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        String speechOutput =
                "Willkommen beim ITM Lübeck skill... Was möchtest du wissen?";
        // If the user either does not reply to the welcome message or says
        // something that is not understood, they will be prompted again with this text.
        String repromptText = "Für Informationen darüber, was du sagen kannst, sage hilfe.";

        // Here we are prompting the user for input
        return newAskResponse(speechOutput, repromptText);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        log.debug("IntentName: {}", intentName);

        if (intentName != null) {
            switch (intentName) {
                case "StatusIntent":
                    return getStatus(intent);
                case "AMAZON.HelpIntent":
                    return getHelp();
                case "AMAZON.StopIntent":
                case "AMAZON.CancelIntent":
                    PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
                    outputSpeech.setText("Auf wiedersehen");

                    return SpeechletResponse.newTellResponse(outputSpeech);
            }
        }

        throw new SpeechletException("Invalid Intent");
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any cleanup logic goes here
    }

    /**
     * Creates a {@code SpeechletResponse} for the StatusIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getStatus(Intent intent) {
        Slot itemSlot = intent.getSlot(TYPE_SLOT);

        if (itemSlot != null && itemSlot.getValue() != null) {
            String itemName = itemSlot.getValue();
            log.debug("ItemSlot: {}", itemName);

            // Get the recipe for the item
            String status = this.status.get(itemName);

            if (status != null) {
                // If we have the status, return it to the user.
                PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
                outputSpeech.setText(status);

                SimpleCard card = new SimpleCard();
                card.setTitle("Staus für " + itemName);
                card.setContent(status);

                return SpeechletResponse.newTellResponse(outputSpeech, card);
            } else {
                // We don't have a status, so keep the session open and ask the user for another
                // item.
                String speechOutput =
                        "Ich habe leider keine Komponente vom Typ " + itemName
                                + " gefunden. Wie kann ich dir helfen?";
                String repromptSpeech = "Wie kann ich dir noch helfen?";
                return newAskResponse(speechOutput, repromptSpeech);
            }
        } else {
            // There was no item in the intent so return the help prompt.
            return getHelp();
        }
    }

    /**
     * Creates a {@code SpeechletResponse} for the HelpIntent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelp() {
        String speechOutput =
                "Du kannst mich nach dem Status zu Komponenten mit einem bestimmten Typ fragen. "
                        + "Probiere Status vom Typ ldr oder ähnliches... sonst sage beenden."
                        + "Wie kann ich dir nun helfen?";
        String repromptText =
                "Du kannst sagen... Wie ist der Status vom Typ ldr."
                        + " Wie kann ich dir helfen?";
        return newAskResponse(speechOutput, repromptText);
    }

    /**
     * Wrapper for creating the Ask response. The OutputSpeech and {@link Reprompt} objects are
     * created from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(stringOutput);

        PlainTextOutputSpeech repromptOutputSpeech = new PlainTextOutputSpeech();
        repromptOutputSpeech.setText(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);

        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }
}
