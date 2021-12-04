package com.github.tamurashingo.salesforce.bulk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import com.github.tamurashingo.salesforce.bulk.util.XmlUtil;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Job {

    public static final String CREATE_JOB_XML =
          "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        + "<jobInfo xmlns=\"http://www.force.com/2009/06/asyncapi/dataload\">"
        + "  <operation>%s</operation>"
        + "  <object>%s</object>"
    ;

    public static final String CLOSE_JOB_XML =
          "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
        + "<jobInfo xmlns=\"http://www.force.com/2009/06/asyncapi/dataload\">"
        + "  <state>Closed</state>"
        + "</jobInfo>"
    ;

    public static final String XPATH_JOB_ID = "/*[local-name() = 'jobInfo' and namespace-uri()='http://www.force.com/2009/06/asyncapi/dataload']/*[local-name() = 'id' and namespace-uri()='http://www.force.com/2009/06/asyncapi/dataload']/text()";
    public static final String XPATH_BATCH_ID = "/jobInfo/batchId/text()";

    /** Salesforce Connection */
    private final Connection connection;
    private final JobType jobType;
    private final String sobject;
    private final String records;
    private final boolean hasExternalField;
    private String externalField = null;

    /** jobId */
    private String jobId;

    public Job(Connection connection, JobType jobType, String sobject, String records) {
        this.connection = connection;
        this.jobType = jobType;
        this.sobject = sobject;
        this.records = records;
        this.hasExternalField = false;
    }

    public Job(Connection connection, JobType jobType, String sobject, String records, String externalField) {
        this.connection = connection;
        this.jobType = jobType;
        this.sobject = sobject;
        this.records = records;
        this.hasExternalField = true;
        this.externalField = externalField;
    }

    public void createJob() throws SalesforceBulkException {
        String xml = String.format(
              "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<jobInfo xmlns=\"http://www.force.com/2009/06/asyncapi/dataload\">"
            + "  <operation>%s</operation>"
            + "  <object>%s</object>"
            + "%s%s%s"
            + "  <contentType>CSV</contentType>"
            + "</jobInfo>"
            , jobType.command()
            , sobject
            , hasExternalField ? "<externalFieldName>" : ""
            , hasExternalField ? externalField : ""
            , hasExternalField ? "</externalFieldName>" : ""
        );
        String path = "job";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml; charset=UTF-8");

        try {
            String response = connection.postXml(path, xml, headers);
            this.jobId = parseJobID(response);
        } catch (IOException|ParserConfigurationException|SAXException|XPathExpressionException ex) {
            throw new JobException("Failed to create a job", ex);
        }
    }

    public void addBatch() throws SalesforceBulkException {
        String path = String.format("job/%s/batch/", this.jobId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/csv; charset=UTF-8");
        try {
            connection.postXml(path, records, headers);
        } catch (IOException ex) {
            throw new JobException("Failed to add records", ex);
        }
    }

    public void closeJob() throws SalesforceBulkException {
        String path = String.format("job/%s", jobId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml; charset=UTF-8");

        try {
            connection.postXml(path, CLOSE_JOB_XML, headers);
        } catch (IOException ex) {
            throw new JobException("Failed to close the job", ex);
        }
    }

    private String parseJobID(String xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
        return XmlUtil.parseXml(doc, XPATH_JOB_ID);
    }

    public static enum JobType {
        INSERT("insert"),
        DELETE("delete"),
        HARD_DELETE("hardDelete"),
        UPDATE("update"),
        UPSERT("upsert"),
        QUERY("query");

        private String cmd;

        private JobType(String cmd) {
            this.cmd = cmd;
        }
        public String command() {
            return this.cmd;
        }
    }

    public static class JobException extends SalesforceBulkException {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        public JobException(String message) {
            super(message);
        }
        
        public JobException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
