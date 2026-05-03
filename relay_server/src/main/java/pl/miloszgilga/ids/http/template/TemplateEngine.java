package pl.miloszgilga.ids.http.template;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class TemplateEngine {
    private final VelocityEngine engine;

    public TemplateEngine() {
        engine = new VelocityEngine();
        engine.setProperty("resource.loaders", "classpath");
        engine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        final String charset = StandardCharsets.UTF_8.displayName();
        engine.setProperty(RuntimeConstants.INPUT_ENCODING, charset);
        engine.setProperty(RuntimeConstants.ENCODING_DEFAULT, charset);
        engine.setProperty(RuntimeConstants.RUNTIME_LOG_REFERENCE_LOG_INVALID, "false");
    }

    public void init() {
        engine.init();
    }

    public String parseTemplate(HtmlView view, Map<String, Object> data) {
        final VelocityContext context = new VelocityContext(data);
        final Template template = engine.getTemplate(view.getPath());
        final StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public String parseTemplate(HtmlView view) {
        return parseTemplate(view, Map.of());
    }
}
