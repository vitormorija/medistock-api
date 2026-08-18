# Integração com a API

Notas para quem vai consumir a API, em especial o painel em Angular.

## Documentação

Suba a aplicação e abra `http://localhost:8080/swagger-ui.html`. As 33 operações
estão lá, com parâmetros, corpo e códigos de resposta. É gerado do código, então
não desatualiza.

Para gerar o cliente em vez de escrever as interfaces à mão:

```bash
curl http://localhost:8080/v3/api-docs -o openapi.json
npx ng-openapi-gen --input openapi.json --output src/app/api
```

O CORS já libera `http://localhost:4200`.

## Permissões

O Swagger mostra que existe resposta 403, mas não diz quem passa. Por padrão
basta estar autenticado. As exceções:

| Rota | Perfis |
|---|---|
| `POST` e `PUT /itens` | ADMIN, GESTOR, FARMACEUTICO |
| `POST /pedidos` e `/pedidos/{id}/confirmar` | ADMIN, GESTOR, FARMACEUTICO |
| `POST /previsoes/{itemId}/gerar` | ADMIN, GESTOR, FARMACEUTICO |
| `PATCH /pedidos/{id}/status` | ADMIN, GESTOR |
| `POST` e `PUT /fornecedores` | ADMIN, GESTOR |
| `POST /alertas` e `/alertas/gerar` | ADMIN, GESTOR |
| `DELETE` de item, fornecedor ou alerta | ADMIN |

Só `POST /auth/registro` e `POST /auth/login` dispensam token.

## Nomes vindos do app

Os recursos têm os mesmos nomes das tabelas do app: `itens`, `pedidos`,
`fornecedores`, `alertas`. Os campos também, convertidos de `snake_case` para
`camelCase`: `quantidade_atual` vira `quantidadeAtual`.

Dois não são conversão direta: `data_eta` virou `etaPrevista` e `data_resolucao`
virou `resolvidoEm`. Os valores dos enums são os mesmos, em maiúsculas.

Quatro tabelas do app não têm equivalente aqui: `eventos_risco`,
`hospitais_parceiros`, `transferencias` e `casos_clinicos`. E a API tem duas
coisas que o app não tem: previsão de demanda e registro de movimentações.

## Detalhes que custam tempo

404 é o id da URL, 422 é o corpo. Pedir `/itens/xyz` que não existe dá 404;
mandar `fornecedorId` inexistente dentro de um pedido dá 422.

O `status` do item não se envia nem se grava, a API calcula a cada leitura a
partir da quantidade.

O erro 400 traz um array `campos`, cada item com `campo` e `erro`, que dá para
ligar direto nos campos do formulário.
