package com.github.tamurashingo.salesforce.bulk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.github.tamurashingo.salesforce.bulk.util.XmlUtil;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Connection {
  
    /** Login URL for Production */
    public static final String LOGIN_URL = "https://login.salesforce.com";
    /** Login URL for Sandbox */
    public static final String LOGIN_URL_SANDBOX = "https://test.salesforce.com";

    /** Access PATH for Login */
    public static final String LOGIN_PATH = "/services/Soap/u";
    /** Access PATH for API */
    public static final String API_PATH_PREFIX = "/services/async";

    /** SOAP message for Login */
    public static final String LOGIN_SOAP_MESSGE =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
        + "              xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
        + "              xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "  <env:Body>"
        + "    <n1:login xmlns:n1=\"urn:partner.soap.sforce.com\">"
        + "      <n1:username>%s</n1:username>"
        + "      <n1:password>%s</n1:password>"
        + "    </n1:login>"
        + "  </env:Body>"
        + "</env:Envelope>"
    ;

    public static final String XPATH_SESSION_ID = "//*[local-name()='sessionId']/text()";
    public static final String XPATH_SERVER_URL = "//*[local-name()='serverUrl']/text()";

    public static final Pattern SERVER_INSTANCE_PATTERN = Pattern.compile("https://([a-zA-Z0-9\\-\\.]{2,}).salesforce.com");

    //private final String loginHost;
    private final String sessionId;
    private final String serverURL;
    private final String instanceHost;
    private final String apiVersion;

    private Connection(String sessionId, String serverURL, String instanceHost, String apiVersion) {
        //this.loginHost = loginHost;
        this.sessionId = sessionId;
        this.serverURL = serverURL;
        this.instanceHost = instanceHost;
        this.apiVersion = apiVersion;
    }

    public static URL generateURL(String apiVersion, boolean sandboxMode) throws MalformedURLException {
        String loginUrl = sandboxMode ? LOGIN_URL_SANDBOX : LOGIN_URL;
        String path = createLoginPath(loginUrl, apiVersion);
        return new URL(path);
    }

    public static Connection login(URL loginURL, String username, String password, String apiVersion) throws ConnectionException {
        try {
            // setup
            HttpURLConnection connection = (HttpURLConnection)loginURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);

            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setRequestProperty("SOAPAction", "login");
            connection.connect();

            // post request
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"))) {
                writer.write(createLoginSoapMessage(username, password));
            }

            // response
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                InputStream in = connection.getErrorStream();
                String errorMessage;
                try {
                    errorMessage = IOUtils.toString(in, "UTF-8");
                } catch (IOException ex) {
                    errorMessage = "Server Response Error:" + responseCode;
                }
                throw new ConnectionException(errorMessage);
            }

            // read response body
            InputStream in = connection.getInputStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            String sessionId = XmlUtil.parseXml(doc, XPATH_SESSION_ID);
            String serverURL = XmlUtil.parseXml(doc, XPATH_SERVER_URL);
            String serverInstance = parseServerInstance(serverURL);
            String instanceHost = String.format("%s.salesforce.com", serverInstance);

            return new Connection(sessionId, serverURL, instanceHost, apiVersion);
        } catch (IOException|ParserConfigurationException|SAXException|XPathExpressionException ex) {
            throw new ConnectionException("Login Error", ex);
        }
    }

    private static String createLoginPath(String url, String apiVersion) {
        return String.format("%s%s/%s", url, LOGIN_PATH, apiVersion);
    }

    private static String createLoginSoapMessage(String username, String password) {
        return String.format(LOGIN_SOAP_MESSGE, username, password);
    }

    public static String parseServerInstance(String serverURL) throws ConnectionException {
        Matcher matcher = SERVER_INSTANCE_PATTERN.matcher(serverURL);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ConnectionException("Unable to get the server instance name:" + serverURL);
    }

    public String postXml(String path, String xml, Map<String, String> headers) throws IOException, ConnectionException {
        URL url = new URL(String.format("https://%s/%s/%s/%s", this.instanceHost, API_PATH_PREFIX, this.apiVersion, path));

        // setup
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.addRequestProperty("X-SFDC-Session", this.sessionId);
        headers.forEach((k, v) -> connection.addRequestProperty(k, v));

        connection.connect();

        // post request
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"))) {
            writer.write(xml);
        }

        // response
        int responseCode = connection.getResponseCode();
        System.out.println("response:" + responseCode);
        if (responseCode < 200 || responseCode >= 400) {
            InputStream in = connection.getErrorStream();
            String errorMessage;
            try {
                errorMessage = IOUtils.toString(in, "UTF-8");
            } catch (IOException ex) {
                errorMessage = "Server Response Error:" + responseCode;
            }
            throw new ConnectionException(errorMessage);
        }

        // read response body
        InputStream in = connection.getInputStream();
        return IOUtils.toString(in, "UTF-8");
    }

    public static class ConnectionException extends SalesforceBulkException {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        public ConnectionException(String message) {
            super(message);
        }

        public ConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
