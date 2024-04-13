package ssac.LMS.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClientBuilder;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import ssac.LMS.domain.Enrollment;
import ssac.LMS.domain.Role;
import ssac.LMS.domain.User;
import ssac.LMS.dto.JoinRequestDto;
import ssac.LMS.dto.LoginRequestDto;
import ssac.LMS.dto.LoginResponseDto;
import ssac.LMS.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitoJoinServiceImpl implements AuthService{

    @Value("${spring.aws.region}")
    private String AWS_REGION;

    @Value("${spring.security.oauth2.client.registration.cognito.client-id}")
    private String CLIENT_ID;

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    private final UserRepository userRepository;

    private CognitoIdentityProviderClient getCognitoClient() {
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        return cognitoClient;
    }

    @Override
    public ResponseEntity<String> join(JoinRequestDto joinRequestDto) {

        // 클라이언트 자격 증명 생성
        CognitoIdentityProviderClient cognitoClient = getCognitoClient();

        log.info("cognitoClient={}", cognitoClient);

        // Cognito 회원 가입 진행
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(CLIENT_ID)
                .username(joinRequestDto.getEmail())
                .password(joinRequestDto.getPassword())
                .userAttributes(
                        AttributeType.builder().name("custom:role").value(joinRequestDto.getRole()).build(),
                        AttributeType.builder().name("name").value(joinRequestDto.getUserName()).build()
                )
                .build();
        try {

            cognitoClient.signUp(signUpRequest);

            User user = new User();
            user.setUserName(joinRequestDto.getUserName());
            user.setEmail(joinRequestDto.getEmail());
            user.setTelephone(joinRequestDto.getTelephone());
            String inputRole = joinRequestDto.getRole();
            Role role = Role.fromValue(inputRole);
            user.setRole(role);
            user.setCreatedAt(LocalDateTime.now());
            user.setIsDeleted(false);

            log.info("createdUser={}", user);

            userRepository.save(user);

//            userRepository.existsByTelephone();

            log.info("saveUser={}", user.getUserName());

            return ResponseEntity.ok("User signed up successfully!");

        } catch (Exception e) {

            log.error("error={}", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed");

        }

    }

    @Override
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow("USER_PASSWORD_AUTH")
                .clientId(CLIENT_ID)
                .authParameters(Map.of("USERNAME", loginRequestDto.getEmail(), "PASSWORD", loginRequestDto.getPassword()))
                .build();

        try {
            CognitoIdentityProviderClient cognitoClient = getCognitoClient();
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            AuthenticationResultType authenticationResultType = authResponse.authenticationResult();

            String accessToken = authenticationResultType.accessToken();
            String idToken = authenticationResultType.idToken();
            String refreshToken = authenticationResultType.refreshToken();

            LoginResponseDto loginResponseDto = new LoginResponseDto(accessToken, idToken, refreshToken);
            return loginResponseDto;
        }catch (Exception e) {
            log.info("error={}", e);
            return null;
        }

    }
}
