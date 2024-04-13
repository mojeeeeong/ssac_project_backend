package ssac.LMS.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {

    private String accessToken;
    private String idToken;
    private String refreshToken;
}
