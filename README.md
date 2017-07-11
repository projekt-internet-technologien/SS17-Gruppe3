# Getting Started

## Maven Dependencies

Es müssen einige spezifische Maven Depenencies installiert
werden, um da Projekt zum Laufen zu bekommen.
Dafür müssen folgende Schritte ausgeführt werden:

### Installation nCoAP-Core
``` 
git clone https://github.com/okleine/nCoAP.git <path_to_directory>
cd <path_to_directory>
mvn install
```
### Installation RXTX
```
git clone git@github.com:projekt-internet-technologien/RXTX.git <path_to_directory>
cd <path_to_directory>
mvn install
```

### Installation SSP-REST-Util

```
git clone https://github.com/projekt-internet-technologien/SSP-REST-Util.git <path_to_directoy>
cd <path_to_directory>
mvn install
```

## Raspberry Pi
### Alexa
#### Alexa Voice Service
Der AVS ist sehr komplex und benötigt mehrere Bibliotheke, die im Betriebssystem installiert sein müssen. Des Weiteren wird eine Desktopumgebung auf dem Raspberry Pi voraussgesetzt.
Dies wird benötigt, da für die Authentifizierung ein Browser geöffnet werden muss.
Mit Hilfe eines [Tutorials](https://www.raspberrypi.org/forums/viewtopic.php?f=66&t=133691) haben wir die _Pixel GUI_ Desktopumgebung installiert.
Dieser Vorgang dauert eine ganze Weile, weshalb es sich anbietet mit einem anderen PC bereits einen [_Amazon developer account_](https://github.com/alexa/alexa-avs-sample-app/wiki/Raspberry-Pi#lets-get-started) anzulegen.

Sobald die Installtion der Desktopumgebung fertig ist, kann ein Browser über den Befehl ```sudo apt-get install chromium-browser``` installiert werden.
Nun sind die Schritte 3 - 7 der [AVS Installationsanleitung](https://github.com/alexa/alexa-avs-sample-app/wiki/Raspberry-Pi#step-3-create-a-device-and-security-profile) auszuführen.
Danach sollte es möglich sein die Beispielanwendung zu starten und die ersten Fragen an Alexa zu stellen.

Ein wichtiger Punkt bei der Integration in unsere Anwendung war der korrekte Zugriff auf die wichtigen Bibliotheken.
Besonders bei den VLC Funktionen gab es viele Schwierigkeiten. Diese werden benutzt um Audioaufnahmen kodieren und formatieren zu können, sowie die Sprachausgabe abzuspielen.  
Es hat sich herausgestellt, dass eine falsche Installation der Betriebssystembibliothek, sowie inkompatible Maven Abhängigkeiten zu Problemen geführt haben.
Zum Reparieren wurde zuerst VLC entfernt und die automatisch konfigurierten Umgebungsvariablen gelöscht.

```bash
sudo apt-get remove -y vlc vlc-nox vlc-data && sudo apt-get autoremove
sudo rm /etc/ld.so.conf.d/vlc_lib.conf
sudo rm /etc/environment
```

Nach einem Neustart konnte alles wieder installiert werden.

```bash
sudo apt-get install -y vlc vlc-nox vlc-data
#Make sure that the libraries can be found
sudo sh -c "echo \"/usr/lib/vlc\" >> /etc/ld.so.conf.d/vlc_lib.conf"
sudo sh -c "echo \"VLC_PLUGIN_PATH=\"/usr/lib/vlc/plugin\"\" >> /etc/environment"
sudo ldconfig
```

Nun mussten wir jedoch darauf achten, welche Version der Java Bibliothek für den Zugriff benutzt werden kann.
Dafür musste die neueste Version 3.10.1 der [Vlcj Bibliothek](https://mvnrepository.com/artifact/uk.co.caprica/vlcj/3.10.1) in Maven eingebunden werden.
Allerdings benutzt diese wiederum eine etwas veraltete Version der _Jna_ Bibliothek als Abhängigkeit. Um dieses Problem beheben zu können, musste davon ebenfalls die [neueste Version](https://mvnrepository.com/artifact/net.java.dev.jna/jna/4.4.0) als Abhängigkeit definiert werden.

#### Skill
Bevor die Anwendung gestartet und der Skill getestet wird, müssen einige Dinge vorbereitet werden.
Zuerst benötigt der Raspberry Pi eine dynamisch zugewiesene IP-Adresse.

Dies ist notwendig, da der Skill von unserer Anwendung selbst als _Web Service_ gehostet wird und von der Amazon Cloud erreichbar sein muss.
Dafür wird der Anbieter [_NoIP_](https://www.noip.com) verwendet, da dieser für einen ausreichenden Zeitraum eine kostenlose Testphase anbietet.
Dort musste man sich registrieren und den Client auf dem PI installieren.
Damit nun auch aus dem Internet mit dem Pi kommuniziert werden kann, muss im Router eine Portweiterleitung eingerichtet werden.
Für die Kommunikation mit der Amazon Cloud nutzt der Skill HTTPS und somit den Port 443.

Im Netzwerk der Universität zu Lübeck gab es die Besonderheit, dass der NoIP Client von der Firewall blockiert wurde.
Deshalb muss nach dem Start des Pi's die IP manuell auf der Webseite eingetragen werden.
Außerdem ist die Portweiterleitung nur über einen Antrag bei einem der Systemadministratoren möglich.

Eine impliziete Vorraussetzung durch das HTTPS Protokoll ist das Verwenden eines SSL-Zertifikats.
Dieses wird auf dem Pi generiert und später in der Amazon Developer Console eingetragen.
In [dieser Anleitung](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/testing-an-alexa-skill#create-a-private-key-and-self-signed-certificate-for-testing) ist genau beschrieben, wie das Zertifikat generiert wird.
Dabei muss darauf geachtet werden, dass unter *[subject\_alternate\_names]* die erstellte Domain von NoIP eingetragen wird.
Dieser Eintrag muss eine Domain enthalten, da Amazon das Zertifikat bei einer IP-Adresse nicht akzeptiert.

Als nächstes muss der Skill bei Amazon registriert werden.
Dafür ist die [offizielle Dokumentation](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/registering-and-managing-alexa-skills-in-the-developer-portal#register) erneut eine große Hilfe.
Das unter Punkt 8 genannte _Interaction Model_ ist bereits fertig und befindet sich innerhalb unseres Repositories im Ordner _src/main/ressources/speechAssets_. Bei Punkt 9 muss nun auch das zuvor erstellte SSL-Zertifikat eingebunden werden.

Um dieses Zertifikat nun auch von der Java Anwendung aus nutzen zu können, ist es notwendig einen _Java KeyStore_ zu erstellen.
Dafür bitte die Schritte 2 und 3 der [Skill Dokumentation](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/deploying-a-sample-skill-as-a-web-service#h3_keystore) befolgen und den Pfad zum keyStore und das Passwort notieren. Dieses wird im nächsten Schritt benötigt.  
Nachdem der Skill nun erfolgreich konfiguriert wurde, kann die Anwendung gestartet und getestet werden. Da für den Start einige Argumente an die Java Virtual Machine (JVM) übergeben werden müssen, haben wir ein kleines Shell-Skript erstellt. Dieses erleichtert nun den Start und erlaubt es weiterhin, optionale Komandenzeilenparameter zu übergeben. Zusätzlich werden mit diesem Skript auch der _CompanionService_ und _WakeWordAgent_ gestartet.

In diesem Skript müssen vor dem ersten Start die folgenden Parameter angepasst werden:

* -Dcom.amazon.speech.speechlet.servlet.supportedApplicationIds
* -Djavax.net.ssl.keyStore
* -Djavax.net.ssl.keyStorePassword

Die _supportedApplicationIds_ wurden beim anlegen des Skills automatisch erstellt und müssen dementsprechend ausgetauscht werden.
Der SSL _keyStore_ und das _keyStorePassword_ wurden im vorherigen Schritt erstellt und müssen nun hier eingesetzt werden.  
Falls auf dem Pi nicht das Standardverzeichnis für Maven Repositories genutzt wird, muss auch noch der _-Xbootclasspath_ angepasst werden.

Nun kann die Anwendung gestartet werden.
Vorrausgesetzt man befindet sich bereits in dem Vezeichnis, wo sich auch das Skript befindet, reicht der einfache Befehl ```./start.sh```.  
Möchte man nun noch Kommandozeilenargumente übergeben wie z.B. _--port_, so ist dies wie gewohnt möglich: ```./start.sh "--port 5678"```

#### Dynamic Host Configuration Protocol - Server

Aufgrund der eingeschränkten Netzwerkverbindungen im Labor wurde auf dem Raspberry Pi ein eigener DHCP-Server aufgesetzt. Hierzu wurde die Library [Dnsmasq](http://thekelleys.org.uk/dnsmasq/doc.html) verwendet. Mit dem nachfolgenden Befehl wird das Paket heruntergeladen und installiert:

```
sudo apt-get install dnsmasq
```

Im nächsten Schritt muss der Schnittstelle *eth0* eine statische IP-Adresse in den Netzwerkeinstellungen zugewiesen werden. Diese wird später von dem DHCP-Server als Gateway verwendet. Hierzu bearbeitet man den *eth0*-Abschnitt in der Datei "*/etc/network/interfaces*" wie folgt:

```
auto eth0
allow-hotplug eth0
iface eth0 inet static
		address 192.168.1.1
		netmask 255.255.255.0
		network 192.168.1.0
		broadcast 192.168.1.255
```

Als nächstes konfiguriert man den DHCP-Server von Dnsmasq wie folgt. Die einzelnen Einstellungen können aus den Kommentaren entnommen werden.

```
# Use interface eth0
interface=eth0
# listen on
listen-address=192.168.1.1
# Assign IP addresses between 192.168.1.50 and 192.168.1.100 with a
# 12 hour lease time
dhcp-range=192.168.1.50,192.168.1.100,12h
dhcp-host=00:17:88:10:28:c8,Philips-hue,192.168.1.60,infinite
# Bind to the interface to make sure we aren't sending things elsewhere
bind-interfaces
server=8.8.8.8
# Don't forward short names
domain-needed
# Never forward addresses in the non-routed address spaces
bogus-priv
```

Danach ist es notwendig die Paket-Weiterleitung von IPv4 zu aktivieren. Hierzu bearbeitet man die Datei "*/etc/sysctl.conf*" und kommentiert die Zeile "*net.ipv4.ip\_forward=1*" wieder ein.

Des Weiteren muss die WLAN-Internetverbindung von dem Raspberrry Pi an die Philips Hue weitergeleitet werden. Wenn die Philips Hue über ein LAN-Kabel mit dem Pi verbunden wird, so soll diese auch eine Internetverbindung erhalten. Hierzu ist es notwending eine Network Address Translation (NAT) zu konfigurieren.

```
sudo iptables -t nat -A POSTROUTING -o wlan0 -j MASQUERADE  
sudo iptables -A FORWARD -i wlan0 -o eth0 -m state --state RELATED,ESTABLISHED -j ACCEPT  
sudo iptables -A FORWARD -i eth0 -o wlan0 -j ACCEPT
```

Da diese Eigenschaften nach jedem Reboot gelten müssen, ist es notwending die IP-Tables in einer Datei abzulegen.

```
sudo sh -c "iptables-save > /etc/iptables.ipv4.nat"
```

Im letzten Schritt müssen diese Einstellungen bei jedem Reboot aus der Datei wiederhergestellt werden. Dafür ist die nachfolgende Zeile in der Datei "*/etc/rc.local*" über "*exit 0*" zu ergänzen.

```
iptables-restore < /etc/iptables.ipv4.nat  
```
