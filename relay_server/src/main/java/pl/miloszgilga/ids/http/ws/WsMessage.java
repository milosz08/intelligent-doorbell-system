package pl.miloszgilga.ids.http.ws;

import com.google.gson.JsonElement;

public record WsMessage(int op, JsonElement data) {
}
