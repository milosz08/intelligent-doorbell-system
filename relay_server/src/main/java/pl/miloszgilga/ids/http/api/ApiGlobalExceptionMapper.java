package pl.miloszgilga.ids.http.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ApiGlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(ApiGlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("An unexpected api error occurred: {}", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("")
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
