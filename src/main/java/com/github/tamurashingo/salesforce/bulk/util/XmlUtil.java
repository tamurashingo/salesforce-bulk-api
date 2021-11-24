package com.github.tamurashingo.salesforce.bulk.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlUtil {

  private XmlUtil() {
  }

  public static String parseXml(Document doc, String expression) throws XPathExpressionException {
      XPath xpath = XPathFactory.newInstance().newXPath();
      return xpath.evaluate(expression, doc);
  }

  public static String parseXml(InputStream in, String expression) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(in);

      return parseXml(doc, expression);
  }

  
}
