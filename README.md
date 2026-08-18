# MediStock API

API REST para controle de estoque hospitalar, em Java com Spring Boot. Faz o
cadastro de itens, fornecedores e pedidos, gera alertas de estoque e validade,
projeta demanda e monta um dashboard.

As regras que a API faz valer estão em [REGRAS.md](REGRAS.md). Quem vai consumir
a API deve ler também o [INTEGRACAO.md](INTEGRACAO.md).

## Requisitos

JDK 21. O Maven não precisa ser instalado, o projeto usa o wrapper `./mvnw`.

## Banco

Os dados ficam no Cloud Firestore. Baixe a chave da conta de serviço no console
do Firebase e salve como `serviceAccount.json` na raiz do projeto.

Para rodar sem configurar nada, use a implementação em memória:

```bash
PERSISTENCIA=memoria ./mvnw spring-boot:run
```

## Rodando

```bash
./mvnw spring-boot:run
```

Sobe em `http://localhost:8080`. A documentação fica em
`http://localhost:8080/swagger-ui.html` e é por lá que dá para testar os 33
endpoints.

## Dados de demonstração

Com a aplicação rodando, este script preenche o banco com um cenário completo:
os cinco usuários, quatro fornecedores, dez itens em situações diferentes,
pedidos em vários status, alertas e previsões.

```bash
./scripts/semear.sh
```

Espera um banco vazio. Contra o Firestore, apague as coleções antes de rodar de
novo, senão os cadastros repetidos são recusados. Em memória basta reiniciar a
aplicação.

## Autenticação

Só o cadastro e o login são públicos, o resto precisa de token. O e-mail tem que
ser de domínio institucional.

O banco já vem com um usuário de cada perfil. A senha dos cinco é
`medistock2026`:

| E-mail | Perfil | Pode |
|---|---|---|
| luana@fiap.com.br | ADMIN | tudo, incluindo apagar |
| bruno@fiap.com.br | GESTOR | cadastrar e editar, menos apagar |
| guilherme@fiap.com.br | GESTOR | o mesmo que o Bruno |
| carolina@fiap.com.br | FARMACEUTICO | cadastrar itens e pedidos |
| vitor@fiap.com.br | ENFERMEIRO | consultar e ajustar quantidade |

Para pegar o token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"luana@fiap.com.br","senha":"medistock2026"}'
```

O token vem na resposta e vai nas outras chamadas:

```bash
curl http://localhost:8080/api/v1/itens -H "Authorization: Bearer SEU_TOKEN"
```

No Swagger, use o botão Authorize no topo da página.

## Testes

```bash
./mvnw test
```

79 testes das regras de negócio, sem banco nem rede.

```bash
./scripts/smoke-test.sh
```

45 verificações contra a API rodando, numa porta separada e em memória, então
não mexe no banco real. Para rodar contra o Firestore:
`PERSISTENCIA=firestore ./scripts/smoke-test.sh`.

## Estrutura

Dentro de `src/main/java/br/com/medistock/api`: `controller` recebe as
requisições, `service` tem as regras, `repository` acessa o banco, `model` as
entidades, `dto` os objetos de entrada e saída, mais `config`, `security` e
`exception`.

O controller nunca fala direto com o banco. Cada repository é uma interface com
duas implementações, Firestore e memória, escolhidas pela propriedade
`medistock.persistencia`.

O `serviceAccount.json` e o `.env` estão no `.gitignore` e não podem ir para o
repositório.
