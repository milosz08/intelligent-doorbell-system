package pl.miloszgilga.ids.http.web;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import pl.miloszgilga.ids.AppBuildConfig;
import pl.miloszgilga.ids.http.template.AppHtmlView;
import pl.miloszgilga.ids.http.template.HtmlTemplateEngine;
import pl.miloszgilga.ids.http.web.alert.FlashAlert;

public class WebGlobalExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(WebGlobalExceptionMapper.class);

    private final HtmlTemplateEngine htmlTemplateEngine;

    public WebGlobalExceptionMapper(HtmlTemplateEngine htmlTemplateEngine) {
        this.htmlTemplateEngine = htmlTemplateEngine;
    }

    @Override
    public Response toResponse(Throwable exception) {
        final Map<String, Object> data = new HashMap<>();
        LOG.error("An unexpected web error occurred: {}", exception.getMessage());
        String message = exception.getMessage();
        if (message == null) {
            message = "Internal server error";
        }
        data.put("alert", FlashAlert.danger(message));
        data.put("compilationHash", AppBuildConfig.COMPILATION_HASH);
        data.put("compilationLongHash", AppBuildConfig.COMPILATION_LONG_HASH);
        data.put("lastUpdatedUTC", AppBuildConfig.BUILD_TIME);
        return Response
                .status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .entity(htmlTemplateEngine.parseTemplate(AppHtmlView.ERROR, data))
                .type(MediaType.TEXT_HTML)
                .build();
    }
}
