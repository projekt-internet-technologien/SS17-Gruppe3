/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package de.uzl.itm.ProjektGruppe3.alexa.skill;

import de.dennisboldt.RestClient;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class Status {
    private static Logger LOG = LoggerFactory.getLogger(Status.class);
    private static final String ACCEPT = "application/xml";
    private static final String QUERY_PATTERN = "PREFIX pit: <https://pit.itm.uni-luebeck.de/>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "\n" +
            "SELECT * WHERE {\n" +
            " ?component pit:isType \"%s\"^^xsd:string .\n" +
            " OPTIONAL {\n" +
            "  ?component pit:hasStatus ?status .\n" +
            "  ?status pit:hasValue ?value .\n" +
            "  ?status pit:hasScaleUnit ?unit .\n" +
            " }\n" +
            "}";
    private String host;
    private int port;


    public Status(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String get(String item) {
        String query = String.format(QUERY_PATTERN, item.substring(0, 1).toUpperCase() + item.substring(1));
        LOG.debug("Query: {}", query);

        RestClient client= new RestClient(host, "" + port, query, ACCEPT);
        StringBuilder answer = new StringBuilder();

        try {
            List<String> responses = client.getResult();
            LOG.debug(StringEscapeUtils.unescapeJava(responses.toString()));

            for (Object response :
                    responses) {
                String strJson = (String) response;
                JsonReader jsonReader = Json.createReader(new StringReader(strJson));
                JsonObject json = jsonReader.readObject();

                String xml = json.getString("results");
                InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                Document doc = new SAXBuilder().build( stream );
                Element root = doc.getRootElement();
                Namespace namespace = root.getNamespace();

                Element results = root.getChild("results", namespace);
                List<Element> resultList = results.getChildren();
                int size = resultList.size();

                if (size <= 0) {
                    return null;
                } else  {
                    answer.append(String.format("Ich habe %s %s gefunden. Ich lese %s dir nun vor... ",
                            size == 1 ? "ein" : size , size > 1 ? "Ergebnisse" : "Ergebnis",
                            size > 1 ? "sie" : "es"));

                    for (int i=0; i < size; i++) {
                        Element result = resultList.get(i);
                        Map<String, String> resultMap = new HashMap<>();

                        for (Element binding :
                                result.getChildren("binding", namespace)) {
                            Element literal = binding.getChild("literal", namespace);

                            if (literal != null) {

                                LOG.debug(binding.getAttribute("name").getValue());
                                LOG.debug(literal.getText());

                                resultMap.putIfAbsent(binding.getAttribute("name").getValue(),
                                        literal.getText());
                            }
                        }

                        LOG.debug(resultMap.toString());

                        if (resultMap.get("value") == null) {
                            answer.append("Kein Status gefunden.");
                        } else {
                            answer.append(resultMap.get("value"))
                                    .append(" ")
                                    .append(resultMap.get("unit"));

                            if (i < size-1) {
                                answer.append(". Nächstes Ergebnis... ");
                            }
                        }
                    }
                }
            }

            LOG.debug(answer.toString());
            return answer.toString();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return null;
        }
    }
}
