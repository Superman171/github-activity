import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GithubActivity {

    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("You need to enter any <username> for getting 'Github-Activity' of that user");
            System.exit(1);
        }

        String username = args[0];
        if (!username.matches("[A-Za-z0-9-]+")) {
            System.err.println("Invalid GitHub username.");
            System.exit(1);
        }

        try {
            String json = fetchUserEvents(username);
            Object parsed = new JsonParser(json).parse();

            if (!(parsed instanceof List<?> events) || events.isEmpty()) {
                System.out.println("No recent activity found for " + username + ".");
                return;
            }

            for (Object event : events) {
                if (!(event instanceof Map<?, ?> eventMap)
                        || !(eventMap.get("repo") instanceof Map<?, ?> repo)) {
                    continue;
                }
                Object type = eventMap.get("type");
                Object repoName = repo.get("name");
                System.out.println("Event Type: " + type + ", Repository: " + repoName);
            }
        } catch (ApiException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Error parsing GitHub response: " + e.getMessage());
            System.exit(1);
        }

    }

    // ------Networking_Part------
    private static String fetchUserEvents(String username) throws ApiException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/users/" + username + "/events"))
                .header("User-Agent", "github-activity-cli")
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> response;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiException("Network Error: " + e.getMessage());
        }

        int status = response.statusCode();

        if (status == 404) {
            throw new ApiException("User \"" + username + "\" not found.");
        }
        if (status == 403) {
            throw new ApiException("API rate limit exceeded. Please try again later.");
        }
        if (status != 200) {
            throw new ApiException("Unexpected response status: " + status);
        }
        return response.body();

    }

    // ------Exception class for handling API errors
    private static class ApiException extends Exception {
        ApiException(String message) {
            super(message);
        }
    }

    // ------JSON_Parser (Using no external libraries)------
    static class JsonParser {
        private final String s;
        private int position = 0;

        JsonParser(String s) {
            this.s = s;
        }

        Object parse() {
            skipWhiteSpace();
            Object value = parseValue();
            skipWhiteSpace();
            if (position != s.length()) {
                throw new RuntimeException("Unexpected character at position " + position);
            }
            return value;
        }

        private Object parseValue() {
            skipWhiteSpace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        // ------Parsing_Object------
        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhiteSpace();
            if (peek() == '}') {
                position++;
                return map;
            }
            while (true) {
                skipWhiteSpace();
                String key = parseString();
                skipWhiteSpace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhiteSpace();
                char c = s.charAt(position++);
                if (c == '}') {
                    break;
                }
                if (c != ',') {
                    throw new RuntimeException("Expected ',' or '}' at position " + position);
                }
            }
            return map;
        }

        // ------Parsing_Array------
        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhiteSpace();
            if (peek() == ']') {
                position++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhiteSpace();
                char c = s.charAt(position++);
                if (c == ']')
                    break;
                if (c != ',')
                    throw new RuntimeException("Expected ',' or ']' at position " + position);
            }
            return list;
        }

        // ------Parsing_String------
        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(position++);
                if (c == '"')
                    break;
                if (c == '\\') {
                    char next = s.charAt(position++);
                    switch ((next)) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = s.substring(position, position + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            position += 4;
                        }
                        default -> sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        // ------Parsing_Boolean_Values------
        private Boolean parseBoolean() {
            if (s.startsWith("true", position)) {
                position += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", position)) {
                position += 5;
                return Boolean.FALSE;
            }
            throw new RuntimeException("Invalid boolean at position " + position);
        }

        // ------Parsing_Null------
        private Object parseNull() {
            if (s.startsWith("null", position)) {
                position += 4;
                return null;
            }
            throw new RuntimeException("Invalid null at position " + position);
        }

        // ------Parsing_Number-----
        private Double parseNumber() {
            int start = position;
            while (position < s.length()) {
                char c = s.charAt(position);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    position++;
                } else {
                    break;
                }
            }
            return Double.parseDouble(s.substring(start, position));
        }

        private char peek() {
            return s.charAt(position);
        }

        private void skipWhiteSpace() {
            while (position < s.length() && Character.isWhitespace(s.charAt(position))) {
                position++;
            }
        }

        private void expect(char c) {
            if (s.charAt(position) != c) {
                throw new RuntimeException("Expected '" + c + "' at position " + position);
            }
            position++;
        }
    }
}