package pma.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import pma.account.AccountController;
import pma.account.AccountIn;
import pma.account.AccountOut;
import pma.account.LoginIn;

/**
 * Regras do Auth. Não tem banco: quem guarda as contas e as senhas é o Account,
 * chamado via Feign (http://account:8080).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  /** Usado se o Account não informar o papel (o token nunca sai sem role). */
  static final String DEFAULT_ROLE = AccountOut.ROLE_USER;

  private final AccountController accountController;
  private final JwtService jwtService;

  public void register(RegisterIn in) {
    if (in == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    try {
      accountController.create(AccountIn.builder()
          .name(in.name())
          .email(in.email())
          .password(in.password())
          .build());
    } catch (FeignException e) {
      // repassa o erro do Account (400 campo faltando, 409 email repetido)
      throw fromAccount(e);
    }
  }

  public TokenOut login(CredentialIn in) {
    if (in == null || isBlank(in.email()) || isBlank(in.password())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "credenciais inválidas");
    }
    AccountOut account;
    try {
      account = accountController.login(LoginIn.builder()
          .email(in.email())
          .password(in.password())
          .build()).getBody();
    } catch (FeignException e) {
      throw fromAccount(e);
    }
    if (account == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "credenciais inválidas");
    }
    String role = account.role() == null || account.role().isBlank() ? DEFAULT_ROLE : account.role();
    return new TokenOut(jwtService.create(account.id(), role));
  }

  public SolveOut solve(SolveIn in) {
    if (in == null || isBlank(in.jwt())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token ausente");
    }
    try {
      return jwtService.solve(in.jwt());
    } catch (JwtException | IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token inválido ou expirado");
    }
  }

  /** 4xx do Account é repassado; Account fora do ar (ou 5xx) vira 503. */
  private static ResponseStatusException fromAccount(FeignException e) {
    HttpStatus status = HttpStatus.resolve(e.status());
    if (status == null || !status.is4xxClientError()) {
      return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "account indisponível");
    }
    return new ResponseStatusException(status);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

}
