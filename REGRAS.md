# Regras de negócio

O enunciado pede uma API de estoque hospitalar mas não diz o que cada operação
deve recusar. Estas são as regras que defini antes de programar. O Swagger e os
testes citam os números daqui.

1. Cadastro só aceita e-mail de domínio institucional. A lista fica no
   `application.yml`, não no código.

2. Saída maior que o estoque é recusada com 422. Saída igual ao estoque zera o
   item e é permitida.

3. No cadastro a validade tem que ser futura. Na atualização não, senão item
   vencido ficaria impossível de corrigir.

4. Confirmar pedido soma as quantidades ao estoque e conclui. Só o `/confirmar`
   chega a `ENTREGUE`; o `PATCH /status` recusa esse valor. Confirmar duas vezes
   dá 422, senão o estoque somaria de novo.

5. Fornecedor nunca é apagado, só inativado. Apagar deixaria pedidos antigos
   apontando para nada. Pedido para fornecedor inativo é recusado.

6. A varredura de alertas avalia quantidade, validade e atraso em separado,
   porque um item pode ter dois problemas ao mesmo tempo. Não duplica alerta que
   ainda não foi resolvido, e alerta ignorado também bloqueia a recriação.

7. CNPJ do fornecedor e código do pedido são únicos. O código é gerado pelo
   sistema no formato `#0001`.

8. A quantidade máxima do item não pode ser menor que a mínima.

9. O valor total do pedido é somado dos itens, nunca aceito na requisição.

10. Pedido sem `slaHoras` herda o do fornecedor.

## Status do item

Não é armazenado, é calculado a cada leitura e depende só da quantidade:

| Condição | Status |
|---|---|
| `atual <= minima * 0,3` | `CRITICO` |
| `atual <= minima` | `ATENCAO` |
| acima disso | `NORMAL` |

A validade não entra porque um item pode estar vencendo com quantidade folgada.
Quem precisa dela usa `GET /itens/vencendo`.

## SLA

Pedido está atrasado quando a data prevista passou e ele ainda não chegou a um
estado final: `ENTREGUE`, `NAO_ENTREGUE`, `EXTRAVIO_REEMBOLSO` ou `CANCELADO`.

## 404 ou 422

Id inexistente na URL é 404. Id inexistente dentro do corpo é 422.
