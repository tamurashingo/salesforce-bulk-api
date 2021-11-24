package com.github.tamurashingo.salesforce.bulk;

public class SalesforceBulkException extends Exception {
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public SalesforceBulkException(String message) {
        super(message);
    }

    public SalesforceBulkException(String message, Throwable cause) {
        super(message, cause);
    }
}
