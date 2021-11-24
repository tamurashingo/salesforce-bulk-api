package com.github.tamurashingo.salesforce.bulk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.github.tamurashingo.salesforce.bulk.Connection.ConnectionException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConnectionTest {

    @Nested
    class GenerateURL {
        @ParameterizedTest
        @CsvSource({
            "41.0, false, https://login.salesforce.com/services/Soap/u/41.0",
            "41.0, true, https://test.salesforce.com/services/Soap/u/41.0",
            "51.0, false, https://login.salesforce.com/services/Soap/u/51.0",
            "51.0, true, https://test.salesforce.com/services/Soap/u/51.0"
        })
        void generateURLTest(String apiVersion, boolean sandboxMode, String expectedURL) throws MalformedURLException {
            URL url = Connection.generateURL(apiVersion, sandboxMode);
            assertEquals(expectedURL, url.toString());
        }
    }

    @Nested
    class Login {
        @Mock
        HttpURLConnection urlConn;

        @Test
        void sandboxLogin() throws Exception {
            doReturn(200).when(urlConn).getResponseCode();
            InputStream xml = ConnectionTest.class.getClassLoader().getResourceAsStream("SandboxResponse.xml");
            doReturn(xml).when(urlConn).getInputStream();
            OutputStream out = new ByteArrayOutputStream();
            doReturn(out).when(urlConn).getOutputStream();

            URLStreamHandler handler = new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    return urlConn;
                }
            };

            Field sessionId = Connection.class.getDeclaredField("sessionId");
            sessionId.setAccessible(true);
            Field serverURL = Connection.class.getDeclaredField("serverURL");
            serverURL.setAccessible(true);
            Field instanceHost = Connection.class.getDeclaredField("instanceHost");
            instanceHost.setAccessible(true);
            Field apiVersion = Connection.class.getDeclaredField("apiVersion");
            apiVersion.setAccessible(true);

            URL url = new URL("http", "dummyhost", 8080, "/login", handler);
            Connection conn = Connection.login(url, "username", "password", "apiVersion");

            assertEquals("thisissessionid", sessionId.get(conn));
            assertEquals("https://xxxxx--sandbox.my.salesforce.com/services/Soap/u/41.0/xxxxxxxxxxxxxxx", serverURL.get(conn));
            assertEquals("xxxxx--sandbox.my.salesforce.com", instanceHost.get(conn));
            assertEquals("apiVersion", apiVersion.get(conn));
        }

        @Test
        void productionLogin() throws Exception {
            doReturn(200).when(urlConn).getResponseCode();
            InputStream xml = ConnectionTest.class.getClassLoader().getResourceAsStream("ProductionResponse.xml");
            doReturn(xml).when(urlConn).getInputStream();
            OutputStream out = new ByteArrayOutputStream();
            doReturn(out).when(urlConn).getOutputStream();

            URLStreamHandler handler = new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    return urlConn;
                }
            };

            Field sessionId = Connection.class.getDeclaredField("sessionId");
            sessionId.setAccessible(true);
            Field serverURL = Connection.class.getDeclaredField("serverURL");
            serverURL.setAccessible(true);
            Field instanceHost = Connection.class.getDeclaredField("instanceHost");
            instanceHost.setAccessible(true);
            Field apiVersion = Connection.class.getDeclaredField("apiVersion");
            apiVersion.setAccessible(true);

            URL url = new URL("http", "dummyhost", 8080, "/login", handler);
            Connection conn = Connection.login(url, "username", "password", "apiVersion");

            assertEquals("thisissessionid", sessionId.get(conn));
            assertEquals("https://xxxxx.my.salesforce.com/services/Soap/u/41.0/xxxxxxxxxxxxxxx", serverURL.get(conn));
            assertEquals("xxxxx.my.salesforce.com", instanceHost.get(conn));
            assertEquals("apiVersion", apiVersion.get(conn));
        }

        @Test
        void loginFailure() throws Exception {
            doReturn(500).when(urlConn).getResponseCode();
            InputStream xml = ConnectionTest.class.getClassLoader().getResourceAsStream("FailureResponse.xml");
            doReturn(xml).when(urlConn).getErrorStream();
            OutputStream out = new ByteArrayOutputStream();
            doReturn(out).when(urlConn).getOutputStream();

            URLStreamHandler handler = new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                    return urlConn;
                }
            };

            URL url = new URL("http", "dummyhost", 8080, "/login", handler);
            assertThrows(ConnectionException.class, () -> Connection.login(url, "username", "password", "apiVersion"));
        }
    }
}
