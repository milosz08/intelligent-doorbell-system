package pl.miloszgilga.ids.http.web;

public class HttpWebPipelineException extends RuntimeException {
    public HttpWebPipelineException(String message, Object... args) {
        super(String.format(message, args));
    }
}
