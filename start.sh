#!/bin/bash
cd ~/alexa-avs-sample-app/samples/companionService/ && npm start > ~/companionService.log &
cd ~/alexa-avs-sample-app/samples/wakeWordAgent/src/ && ./wakeWordAgent -e sensory > ~/wakewordAgent.log &
sudo java -Xbootclasspath/p:/home/pi/.m2/repository/org/mortbay/jetty/alpn/alpn-boot/8.1.11.v20170118/alpn-boot-8.1.11.v20170118.jar \
-Djavax.net.ssl.keyStore=/home/pi/skill-certs/java-keystore.jks \
-Djavax.net.ssl.keyStorePassword="Ln94y8an" \
-Dcom.amazon.speech.speechlet.servlet.disableRequestSignatureCheck=false \
-Dcom.amazon.speech.speechlet.servlet.supportedApplicationIds="amzn1.ask.skill.b869987a-84d7-4446-8a4c-394c9bf12cda" \
-Dcom.amazon.speech.speechlet.servlet.timestampTolerance=150 \
-jar ProjektGruppe3-4.0.0-SNAPSHOT-jar-with-dependencies.jar $1
