# pma26-auth-service

Implementação do serviço **Auth**: registro, login com emissão de JWT e validação de token para o gateway.
Implementa a interface [`pma26-auth`](https://github.com/Organizacao-dos-Coisa-Ruim/pma26-auth).

Não tem banco: as contas e as senhas (com hash) ficam no **Account**, chamado via Feign.

## Fluxo

```
cliente ──POST /auth/register──▶ gateway ──▶ auth ──Feign POST /accounts───────▶ account
cliente ──POST /auth/login─────▶ gateway ──▶ auth ──Feign POST /accounts/login─▶ account
                                              └── devolve {jwt}
cliente ──GET /accounts (Bearer jwt)──▶ gateway ──POST /auth/solve──▶ auth
                                          └── token ok: repassa id-account / role-account para o serviço
```

## Estrutura (`pma.auth`)

| Classe | Papel |
|--------|-------|
| `AuthResource` | Controller: implementa `AuthController` |
| `AuthService` | Regras: chama o Account, converte erros do Feign em status HTTP |
| `JwtService` | Emite e valida o JWT (HMAC-SHA256, jjwt) |

O token leva o id da conta em `sub`, o papel em `role` (hoje sempre `user`, porque o Account ainda não tem papéis),
emissor `pma.auth` e expiração.

## Variáveis de ambiente

| Variável | Padrão | |
|----------|--------|-|
| `JWT_SECRET` | – | **obrigatória**, mínimo de 32 caracteres. Sem ela (ou com menos) o serviço não sobe |
| `JWT_DURATION` | `1h` | validade do token (`30m`, `1h`, `1d`...) |
| `ACCOUNT_URL` | `http://account:8080` | |

## Erros

| Situação | Status |
|----------|--------|
| register com campo faltando / email repetido | `400` / `409` (repassados do Account) |
| login com credenciais erradas | `401` |
| solve com token inválido, adulterado ou expirado | `401` |
| Account fora do ar | `503` |

## Build, testes e imagem

```bash
cd ../pma26-auth && ./mvnw clean install && cd -
cd ../pma-26-account && ./mvnw clean install && cd -
./mvnw clean package     # testes: JwtService, AuthService e endpoints (Account mockado)
docker build -t auth .
```
