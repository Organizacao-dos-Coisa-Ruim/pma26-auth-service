package pma.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

  private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-caracteres";

  private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

  @Test
  void tokenEmitidoEhValidadoComIdERole() {
    String jwt = jwtService.create("conta-1", "user");

    SolveOut out = jwtService.solve(jwt);

    assertThat(out.idAccount()).isEqualTo("conta-1");
    assertThat(out.role()).isEqualTo("user");
  }

  @Test
  void tokenExpiradoEhRecusado() {
    JwtService jaExpirado = new JwtService(SECRET, Duration.ofSeconds(-10));
    String jwt = jaExpirado.create("conta-1", "user");

    assertThatThrownBy(() -> jwtService.solve(jwt)).isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void tokenAssinadoComOutroSegredoEhRecusado() {
    JwtService outro = new JwtService("outro-segredo-tambem-com-mais-de-32-caracteres", Duration.ofHours(1));
    String jwt = outro.create("conta-1", "user");

    assertThatThrownBy(() -> jwtService.solve(jwt)).isInstanceOf(JwtException.class);
  }

  @Test
  void tokenAdulteradoEhRecusado() {
    String jwt = jwtService.create("conta-1", "user");
    String[] parts = jwt.split("\\.");
    // troca o payload por outro (sub = "admin"), mantendo a assinatura original
    String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"iss\":\"pma.auth\",\"sub\":\"admin\",\"role\":\"admin\"}".getBytes());
    String adulterado = parts[0] + "." + payloadFalso + "." + parts[2];

    assertThatThrownBy(() -> jwtService.solve(adulterado)).isInstanceOf(JwtException.class);
  }

  @Test
  void textoQualquerEhRecusado() {
    assertThatThrownBy(() -> jwtService.solve("nao-sou-um-jwt")).isInstanceOf(JwtException.class);
  }

  @Test
  void segredoCurtoImpedeOServicoDeSubir() {
    assertThatThrownBy(() -> new JwtService("curto", Duration.ofHours(1)))
        .isInstanceOf(WeakKeyException.class);
  }

}
