import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GithubActivity {
    private static void processEvents(String json) {
        System.out.println(json);
    }
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: github-activity <username>");
            return;
        }
        
        String username = args[0];
        System.out.println("GitHub Username: " + username);
        String url = "https://api.github.com/users/" + username + "/events";
        System.out.println("Fetching public events from: " + url);

        
        try{

            HttpClient client = HttpClient.newHttpClient();
    
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 200) {
                processEvents(response.body());
            } else if(response.statusCode() == 404) {
                System.out.println("GitHub Username Not Found");
            } else {
                System.out.println("GitHub API Error: " + response.statusCode());
            }
        }
        catch (Exception e) {
            System.out.println("Unable to fetch GitHub activity: " + e.getMessage());
        }
    }
}