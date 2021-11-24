package com.github.tamurashingo.salesforce.bulk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class XmlUtilTest {

    @Test
    void xpathWithoutNamespace() throws Exception {
        String xmlFile = XmlUtilTest.class.getClassLoader().getResource("util/xmlutil/sample.xml").getPath();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        assertEquals("Wikipedia", XmlUtil.parseXml(doc, "//project/@name"));
    }

     @Test
     void xpathWithNamespace() throws Exception {
        String xmlFile = XmlUtilTest.class.getClassLoader().getResource("util/xmlutil/namespace.xml").getPath();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        assertEquals("thisissessionid", XmlUtil.parseXml(doc, "//*[local-name()='sessionId']/text()"));
        assertEquals("https://xxxxx--sandbox.my.salesforce.com/services/Soap/u/41.0/xxxxxxxxxxxxxxx", XmlUtil.parseXml(doc, "//*[local-name()='serverUrl']/text()"));
     }
}
