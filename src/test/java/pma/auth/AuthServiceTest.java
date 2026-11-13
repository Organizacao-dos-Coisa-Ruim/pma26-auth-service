package pma.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;
import pma.account.AccountController;
import pma.account.AccountIn;
import pma.account.AccountOut;
import pma.account.LoginIn;

/** Testes unitários do AuthService: Account mockado, JwtService real. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private AccountController accountController;

  private final JwtService jwtService =
      new JwtService("segredo-de-teste-com-pelo-menos-32-caracteres", Duration.ofHours(1));

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(accountController, jwtService);
  }

  @Test
  void registerCriaContaNoAccount() {
    authService.register(new RegisterIn("Ana", "ana@insper.edu.br", "123456"));

    ArgumentCaptor<AccountIn> sent = ArgumentCaptor.forClass(AccountIn.class);
    verify(accountController).create(sent.capture());
    assertThat(sent.getValue().email()).isEqualTo("ana@insper.edu.br");
    assertThat(sent.getValue().password()).isEqualTo("123456");
  }

  @Test
  void registerComEmailRepetidoRepassa409() {
    FeignException error = feignError(409);
    when(accountController.create(any())).thenThrow(error);

    assertStatus(() -> authService.register(new RegisterIn("Ana", "ana@insper.edu.br", "123456")),
        HttpStatus.CONFLICT);
  }

  @Test
  void loginDevolveTokenComIdDaConta() {
    when(accountController.login(any(LoginIn.class)))
        .thenReturn(ResponseEntity.ok(new AccountOut("conta-1", "Ana", "ana@insper.edu.br", "user")));

    TokenOut token = authService.login(new CredentialIn("ana@insper.edu.br", "123456"));

    SolveOut solved = jwtService.solve(token.jwt());
    assertThat(solved.idAccount()).isEqualTo("conta-1");
    assertThat(solved.role()).isEqualTo("user");
  }

  @Test
  void tokenDoAdminLevaRoleAdmin() {
    when(accountController.login(any(LoginIn.class)))
        .thenReturn(ResponseEntity.ok(new AccountOut("adm-1", "Admin", "admin@pma.com", "admin")));

    TokenOut token = authService.login(new CredentialIn("admin@pma.com", "s3nha"));

    assertThat(jwtService.solve(token.jwt()).role()).isEqualTo("admin");
  }

  @Test
  void contaSemRoleRecebeUserNoToken() {
    when(accountController.login(any(LoginIn.class)))
        .thenReturn(ResponseEntity.ok(new AccountOut("conta-1", "Ana", "ana@insper.edu.br", null)));

    TokenOut token = authService.login(new CredentialIn("ana@insper.edu.br", "123456"));

    assertThat(jwtService.solve(token.jwt()).role()).isEqualTo("user");
  }

  @Test
  void loginComCredenciaisErradasDa401() {
    FeignException error = feignError(401);
    when(accountController.login(any(LoginIn.class))).thenThrow(error);

    assertStatus(() -> authService.login(new CredentialIn("ana@insper.edu.br", "errada")),
        HttpStatus.UNAUTHORIZED);
  }

  @Test
  void loginSemSenhaDa401SemChamarOAccount() {
    assertStatus(() -> authService.login(new CredentialIn("ana@insper.edu.br", null)),
        HttpStatus.UNAUTHORIZED);
  }

  @Test
  void accountForaDoArDa503() {
    // status -1: o Feign não conseguiu conectar
    FeignException error = feignError(-1);
    when(accountController.login(any(LoginIn.class))).thenThrow(error);

    assertStatus(() -> authService.login(new CredentialIn("ana@insper.edu.br", "123456")),
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void solveComTokenValido() {
    String jwt = jwtService.create("conta-1", "user");

    assertThat(authService.solve(new SolveIn(jwt)).idAccount()).isEqualTo("conta-1");
  }

  @Test
  void solveComTokenInvalidoDa401() {
    assertStatus(() -> authService.solve(new SolveIn("lixo")), HttpStatus.UNAUTHORIZED);
  }

  @Test
  void solveComTokenExpiradoDa401() {
    String expirado = new JwtService("segredo-de-teste-com-pelo-menos-32-caracteres", Duration.ofSeconds(-10))
        .create("conta-1", "user");

    assertStatus(() -> authService.solve(new SolveIn(expirado)), HttpStatus.UNAUTHORIZED);
  }

  private static FeignException feignError(int status) {
    FeignException e = mock(FeignException.class);
    when(e.status()).thenReturn(status);
    return e;
  }

  private static void assertStatus(Runnable call, HttpStatusCode expected) {
    assertThatThrownBy(call::run)
        .isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(expected));
  }

}
