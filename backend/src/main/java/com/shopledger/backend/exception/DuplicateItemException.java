
package com.shopledger.backend.exception;

public class DuplicateItemException extends RuntimeException {

    public DuplicateItemException(String partNumber) {
        super("An item with manufacturer part number '"
                + partNumber + "' already exists.");
    }
}
