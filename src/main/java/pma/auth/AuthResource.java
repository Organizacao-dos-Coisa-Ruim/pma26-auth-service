package pma.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Implementa o contrato AuthController: as rotas vêm das anotações da interface. */
@RestController
@RequiredArgsConstructor
public class AuthResource implements AuthController {

  private final AuthService authService;

  @Override
  public ResponseEntity<Void> register(RegisterIn in) {
    authService.register(in);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Override
  public ResponseEntity<TokenOut> login(CredentialIn in) {
    return ResponseEntity.ok(authService.login(in));
  }

  @Override
  public ResponseEntity<SolveOut> solve(SolveIn in) {
    return ResponseEntity.ok(authService.solve(in));
  }

  @Override
  public ResponseEntity<Void> healthCheck() {
    return ResponseEntity.ok().build();
  }

}
