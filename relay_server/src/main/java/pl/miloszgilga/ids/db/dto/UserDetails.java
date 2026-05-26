package pl.miloszgilga.ids.db.dto;

public record UserDetails(Integer id, String username, long permissionsMask) {
}
