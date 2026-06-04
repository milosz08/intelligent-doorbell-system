package pl.miloszgilga.ids.http.template;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class HtmlTemplateEngine {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlTemplateEngine.class);

    private final TemplateEngine thymeleafEngine;

    public HtmlTemplateEngine(boolean cacheable) {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("/template/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(cacheable);
        thymeleafEngine = new TemplateEngine();
        thymeleafEngine.setTemplateResolver(resolver);
    }

    public void init() {
        LOG.info("Initializing Thymeleaf engine");
        thymeleafEngine.clearTemplateCache();
    }

    public String parseTemplate(HtmlView view, Map<String, Object> data) {
        LOG.debug("Parsing template: {}", view.getPath());
        final Context context = new Context();
        context.setVariables(data);
        return thymeleafEngine.process(view.getPath(), context);
    }

    public String parseTemplate(HtmlView view) {
        return parseTemplate(view, Map.of());
    }
}
