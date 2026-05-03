package pl.miloszgilga.ids.db.dto;

import pl.miloszgilga.ids.db.Role;

public record UserDetails(Integer id, String username, Role role) {
}
