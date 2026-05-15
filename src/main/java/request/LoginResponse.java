package request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    @JsonProperty("auth-token")
    private String authToken;

    private String email;
    private String username;
    private String status;
    private String message;
}