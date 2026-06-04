package pl.miloszgilga.ids.http.api.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pl.miloszgilga.ids.http.api.HttpApiPipelineException;

public abstract class ApiResourceBase {
    protected Response generateGenericError(HttpApiPipelineException ex) {
        return Response.status(ex.getResponseStatus())
                .entity("")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
