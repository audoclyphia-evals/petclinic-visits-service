package org.springframework.samples.petclinic.visits.web;  // This Java source file defines the ResourceNotFoundException class, which is part of the REST API layer (Cluster_1) in the petclinic-visits service. The class is used to handle situations where a resource, such as a Visit entity (Cluster_0), is not found, and it returns an HTTP 404 status code due to its @ResponseStatus annotation.

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The ResourceNotFoundException class extends RuntimeException and is annotated with @ResponseStatus(value = HttpStatus.NOT_FOUND) to automatically set the HTTP status to 404 when thrown. It has a constructor that accepts a String message parameter for error details. This exception is used in the REST API layer (Cluster_1) to signal missing resources like Visit entities (Cluster_0), and it is part of the web package for exception handling in the petclinic-visits service.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
