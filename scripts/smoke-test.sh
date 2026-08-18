#!/usr/bin/env bash
set -uo pipefail

PORTA="${PORTA:-18080}"
PERSISTENCIA="${PERSISTENCIA:-memoria}"
BASE="http://localhost:$PORTA/api/v1"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG="$(mktemp)"
CORPO="$(mktemp)"
SUFIXO=$(printf '%05d' $(( $$ % 100000 )))
JSON='Content-Type: application/json'

OK=0
FALHOU=0

encerrar() {
    [ -n "${PID:-}" ] && kill "$PID" 2>/dev/null
    lsof -ti tcp:"$PORTA" 2>/dev/null | xargs kill 2>/dev/null
    rm -f "$LOG" "$CORPO"
}
trap encerrar EXIT INT TERM

if ! java -version > /dev/null 2>&1; then
    echo "JDK 21 nao encontrado no PATH."
    exit 1
fi

if lsof -ti tcp:"$PORTA" > /dev/null 2>&1; then
    echo "A porta $PORTA ja esta em uso. Rode com PORTA=19000 ./scripts/smoke-test.sh"
    exit 1
fi

echo "Compilando..."
if ! (cd "$RAIZ" && ./mvnw -q clean compile > "$LOG" 2>&1); then
    tail -20 "$LOG"
    exit 1
fi

echo "Subindo a aplicacao na porta $PORTA (persistencia: $PERSISTENCIA)..."
(cd "$RAIZ" && ./mvnw spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Dserver.port=$PORTA -Dmedistock.persistencia=$PERSISTENCIA -Dspring.devtools.restart.enabled=false" \
    > "$LOG" 2>&1) &
PID=$!

for _ in $(seq 1 90); do
    grep -q "Started ApiApplication" "$LOG" && break
    sleep 1
done
if ! grep -q "Started ApiApplication" "$LOG"; then
    echo "A aplicacao nao subiu:"
    tail -20 "$LOG"
    exit 1
fi
echo

verificar() {
    if [ "$2" = "$3" ]; then
        OK=$((OK + 1))
        printf '  ok      %s\n' "$1"
    else
        FALHOU=$((FALHOU + 1))
        printf '  FALHOU  %s (esperado %s, veio %s)\n' "$1" "$2" "$3"
        printf '          resposta: %s\n' "$(head -c 200 "$CORPO")"
    fi
}

chamar() {
    local metodo="$1" caminho="$2" token="$3" dados="${4:-}"
    local args=(-s -o "$CORPO" -w '%{http_code}' -X "$metodo" "$BASE$caminho")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$dados" ] && args+=(-H "$JSON" -d "$dados")
    curl "${args[@]}"
}

campo() {
    python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('$1',''))" < "$CORPO" 2>/dev/null
}

echo "Autenticacao"
registro() {
    printf '{"nome":"%s","email":"%s","senha":"senhaSegura123","matricula":"%s","departamento":"Farmacia","cargo":"Analista","perfil":"%s"}' "$1" "$2" "$3" "$4"
}

ADMIN_EMAIL="ana.admin.$SUFIXO@hc.unicamp.br"
ENF_EMAIL="carlos.enf.$SUFIXO@einstein.br"

verificar "cadastra usuario ADMIN" 201 \
    "$(chamar POST /auth/registro "" "$(registro 'Ana Admin' "$ADMIN_EMAIL" "AD-2026-$SUFIXO" ADMIN)")"
verificar "cadastra usuario ENFERMEIRO" 201 \
    "$(chamar POST /auth/registro "" "$(registro 'Carlos Enf' "$ENF_EMAIL" "EN-2026-$SUFIXO" ENFERMEIRO)")"
verificar "recusa e-mail fora do dominio institucional" 422 \
    "$(chamar POST /auth/registro "" "$(registro 'Fulano' "fulano.$SUFIXO@gmail.com" "XX-2026-$SUFIXO" ADMIN)")"
verificar "recusa e-mail ja cadastrado" 409 \
    "$(chamar POST /auth/registro "" "$(registro 'Ana Admin' "$ADMIN_EMAIL" "ZZ-2026-$SUFIXO" ADMIN)")"

chamar POST /auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"senha\":\"senhaSegura123\"}" > /dev/null
ADMIN=$(campo token)
chamar POST /auth/login "" "{\"email\":\"$ENF_EMAIL\",\"senha\":\"senhaSegura123\"}" > /dev/null
ENF=$(campo token)
verificar "login devolve token" ok "$([ ${#ADMIN} -gt 50 ] && echo ok || echo vazio)"
LOGIN_ERRADO=$(printf '{"email":"%s","senha":"senhaErrada"}' "$ADMIN_EMAIL")
verificar "senha errada nao autentica" 401 "$(chamar POST /auth/login "" "$LOGIN_ERRADO")"
verificar "/auth/me com token" 200 "$(chamar GET /auth/me "$ADMIN")"
verificar "sem token da 401" 401 "$(chamar GET /itens "")"

echo
echo "Itens"
verificar "cadastra item com estoque folgado" 201 \
    "$(chamar POST /itens "$ADMIN" '{"nome":"Dipirona 500mg","categoria":"Medicamento","unidadeMedida":"cx","quantidadeAtual":50,"quantidadeMinima":10,"dataValidade":"2027-06-30"}')"
DIPIRONA=$(campo id)
verificar "status derivado e NORMAL" NORMAL "$(campo status)"

chamar POST /itens "$ADMIN" '{"nome":"Adrenalina 1mg","categoria":"Medicamento","unidadeMedida":"un","quantidadeAtual":2,"quantidadeMinima":20,"dataValidade":"2028-01-31"}' > /dev/null
verificar "estoque em 10% do minimo e CRITICO" CRITICO "$(campo status)"

verificar "recusa quantidade negativa" 400 \
    "$(chamar POST /itens "$ADMIN" '{"nome":"Teste","categoria":"Medicamento","unidadeMedida":"un","quantidadeAtual":-5,"quantidadeMinima":10,"dataValidade":"2027-01-01"}')"
verificar "recusa maximo menor que o minimo" 422 \
    "$(chamar POST /itens "$ADMIN" '{"nome":"Teste","categoria":"Medicamento","unidadeMedida":"un","quantidadeAtual":10,"quantidadeMinima":100,"quantidadeMaxima":40,"dataValidade":"2027-01-01"}')"

verificar "lista itens" 200 "$(chamar GET /itens "$ADMIN")"
verificar "detalha um item" 200 "$(chamar GET "/itens/$DIPIRONA" "$ADMIN")"
verificar "id inexistente da 404" 404 "$(chamar GET /itens/nao-existe "$ADMIN")"
verificar "lista os criticos" 200 "$(chamar GET /itens/criticos "$ADMIN")"
verificar "lista os que estao vencendo" 200 "$(chamar GET /itens/vencendo "$ADMIN")"

chamar PATCH "/itens/$DIPIRONA/quantidade" "$ADMIN" '{"tipo":"SAIDA","quantidade":5}' > /dev/null
verificar "saida desconta do estoque" 45 "$(campo quantidadeAtual)"
chamar PATCH "/itens/$DIPIRONA/quantidade" "$ADMIN" '{"tipo":"ENTRADA","quantidade":10}' > /dev/null
verificar "entrada soma ao estoque" 55 "$(campo quantidadeAtual)"
verificar "regra 2: saida maior que o estoque e recusada" 422 \
    "$(chamar PATCH "/itens/$DIPIRONA/quantidade" "$ADMIN" '{"tipo":"SAIDA","quantidade":999}')"

verificar "ENFERMEIRO nao pode cadastrar item" 403 \
    "$(chamar POST /itens "$ENF" '{"nome":"Teste","categoria":"Medicamento","unidadeMedida":"un","quantidadeAtual":1,"quantidadeMinima":1,"dataValidade":"2027-01-01"}')"
verificar "ENFERMEIRO pode ajustar quantidade" 200 \
    "$(chamar PATCH "/itens/$DIPIRONA/quantidade" "$ENF" '{"tipo":"SAIDA","quantidade":1}')"

echo
echo "Fornecedores"
CNPJ_A="447346710$SUFIXO"
CNPJ_B="112223330$SUFIXO"
NOVO_FORNECEDOR=$(printf '{"nome":"Cristalia","cnpj":"%s","email":"vendas@cristalia.com.br","slaHoras":48}' "$CNPJ_A")
verificar "cadastra fornecedor" 201 "$(chamar POST /fornecedores "$ADMIN" "$NOVO_FORNECEDOR")"
FORNECEDOR=$(campo id)
verificar "score de confiabilidade comeca em 100" 100 "$(campo scoreConfiabilidade)"
REPETIDO=$(printf '{"nome":"Outra","cnpj":"%s","email":"a@b.com.br","slaHoras":24}' "$CNPJ_A")
verificar "recusa CNPJ repetido" 409 "$(chamar POST /fornecedores "$ADMIN" "$REPETIDO")"

DESCARTAVEL_CORPO=$(printf '{"nome":"Aspen","cnpj":"%s","email":"contato@aspen.com.br","slaHoras":72}' "$CNPJ_B")
chamar POST /fornecedores "$ADMIN" "$DESCARTAVEL_CORPO" > /dev/null
DESCARTAVEL=$(campo id)
chamar DELETE "/fornecedores/$DESCARTAVEL" "$ADMIN" > /dev/null
chamar GET "/fornecedores/$DESCARTAVEL" "$ADMIN" > /dev/null
verificar "excluir fornecedor apenas inativa" False "$(campo ativo)"

echo
echo "Pedidos"
NOVO_PEDIDO=$(printf '{"fornecedorId":"%s","etaPrevista":"2027-09-01T14:00:00Z","itens":[{"itemId":"%s","quantidade":100,"valorUnitario":12.50}]}' "$FORNECEDOR" "$DIPIRONA")
verificar "cria pedido" 201 "$(chamar POST /pedidos "$ADMIN" "$NOVO_PEDIDO")"
PEDIDO=$(campo id)
verificar "status inicial e PENDENTE" PENDENTE "$(campo status)"
verificar "valor total somado dos itens" 1250.0 "$(campo valorTotal)"
verificar "SLA herdado do fornecedor" 48 "$(campo slaHoras)"

SEM_FORNECEDOR=$(printf '{"fornecedorId":"nao-existe","etaPrevista":"2027-09-01T14:00:00Z","itens":[{"itemId":"%s","quantidade":1}]}' "$DIPIRONA")
verificar "recusa fornecedor inexistente" 422 "$(chamar POST /pedidos "$ADMIN" "$SEM_FORNECEDOR")"
verificar "PATCH status nao aceita ENTREGUE" 422 \
    "$(chamar PATCH "/pedidos/$PEDIDO/status" "$ADMIN" '{"status":"ENTREGUE"}')"

ANTES=$(chamar GET "/itens/$DIPIRONA" "$ADMIN" > /dev/null; campo quantidadeAtual)
chamar POST "/pedidos/$PEDIDO/confirmar" "$ADMIN" > /dev/null
verificar "confirmar conclui o pedido" ENTREGUE "$(campo status)"
chamar GET "/itens/$DIPIRONA" "$ADMIN" > /dev/null
verificar "regra 4: confirmar da entrada no estoque" "$((ANTES + 100))" "$(campo quantidadeAtual)"
verificar "confirmar duas vezes e recusado" 422 "$(chamar POST "/pedidos/$PEDIDO/confirmar" "$ADMIN")"

echo
echo "Alertas, previsoes e dashboard"
verificar "gera alertas automaticos" 200 "$(chamar POST /alertas/gerar "$ADMIN")"
verificar "lista alertas" 200 "$(chamar GET /alertas "$ADMIN")"
chamar POST /alertas "$ADMIN" '{"tipo":"IA","severidade":"INFO","titulo":"Teste","mensagem":"Alerta manual"}' > /dev/null
ALERTA=$(campo id)
chamar PATCH "/alertas/$ALERTA/resolver" "$ADMIN" > /dev/null
verificar "resolver muda o status do alerta" RESOLVIDO "$(campo status)"
chamar POST /alertas "$ADMIN" '{"tipo":"IA","severidade":"INFO","titulo":"Teste 2","mensagem":"Outro"}' > /dev/null
OUTRO=$(campo id)
chamar PATCH "/alertas/$OUTRO/ignorar" "$ADMIN" > /dev/null
verificar "ignorar muda o status do alerta" IGNORADO "$(campo status)"

verificar "gera previsao de demanda" 200 "$(chamar POST "/previsoes/$DIPIRONA/gerar" "$ADMIN")"
verificar "consulta a previsao gerada" 200 "$(chamar GET "/previsoes/$DIPIRONA" "$ADMIN")"
verificar "ENFERMEIRO nao pode gerar previsao" 403 "$(chamar POST "/previsoes/$DIPIRONA/gerar" "$ENF")"

verificar "resumo do dashboard" 200 "$(chamar GET /dashboard/resumo "$ADMIN")"
verificar "dashboard exige token" 401 "$(chamar GET /dashboard/resumo "")"

echo
echo "----------------------------------------------------------------------"
if [ "$FALHOU" -eq 0 ]; then
    echo "$OK verificacoes, todas passaram."
    exit 0
fi
echo "$OK passaram, $FALHOU falharam."
exit 1
