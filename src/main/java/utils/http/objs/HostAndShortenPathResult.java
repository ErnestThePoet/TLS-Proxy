package utils.http.objs;

public record HostAndShortenPathResult(String host, String newPath, byte[] newRequestData) {
}
