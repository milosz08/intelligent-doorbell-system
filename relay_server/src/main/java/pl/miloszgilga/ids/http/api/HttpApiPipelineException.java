package pl.miloszgilga.ids.http.api;

import org.eclipse.jetty.http.HttpStatus;

import jakarta.ws.rs.core.Response;

public class HttpApiPipelineException extends RuntimeException {
    private final Response.Status status;

    public HttpApiPipelineException(Response.Status status) {
        super(HttpStatus.getMessage(status.getStatusCode()));
        this.status = status;
    }

    public Response.Status getResponseStatus() {
        return status;
    }
}
