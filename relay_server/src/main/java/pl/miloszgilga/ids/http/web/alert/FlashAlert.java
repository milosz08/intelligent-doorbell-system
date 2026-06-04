package pl.miloszgilga.ids.http.web.alert;

import java.io.Serializable;

public record FlashAlert(String type, String message) implements Serializable {
    public static FlashAlert info(String message) {
        return new FlashAlert("info", message);
    }

    public static FlashAlert success(String message) {
        return new FlashAlert("success", message);
    }

    public static FlashAlert warning(String message) {
        return new FlashAlert("warning", message);
    }

    public static FlashAlert danger(String message) {
        return new FlashAlert("danger", message);
    }
}
