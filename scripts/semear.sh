#!/usr/bin/env bash
set -uo pipefail

B="http://localhost:${PORTA:-8080}/api/v1"
JSON='Content-Type: application/json'
SENHA='medistock2026'
CORPO=$(mktemp)
trap 'rm -f "$CORPO"' EXIT

post() {
    local caminho="$1" token="$2" dados="$3"
    local args=(-s -o "$CORPO" -w '%{http_code}' -X POST "$B$caminho" -H "$JSON")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    args+=(-d "$dados")
    curl "${args[@]}"
}
campo() { python3 -c "import sys,json;print(json.load(sys.stdin).get('$1',''))" < "$CORPO" 2>/dev/null; }

if ! curl -sf "$B/auth/login" -o /dev/null -X POST -H "$JSON" -d '{"email":"x@y.z","senha":"a"}' 2>/dev/null; then
    if ! curl -s -o /dev/null "$B/itens" 2>/dev/null; then
        echo "A API nao respondeu em $B. Suba a aplicacao antes."
        exit 1
    fi
fi

echo "Usuarios"
usuario() {
    local corpo
    corpo=$(printf '{"nome":"%s","email":"%s@fiap.com.br","senha":"%s","matricula":"%s","departamento":"%s","cargo":"%s","registroProfissional":%s,"hospital":"HC Unicamp","perfil":"%s"}' \
        "$1" "$2" "$SENHA" "$3" "$4" "$5" "$6" "$7")
    local codigo
    codigo=$(post /auth/registro "" "$corpo")
    if [ "$codigo" = "201" ]; then
        echo "  $1 ($7)"
    elif [ "$codigo" = "409" ]; then
        echo "  $1 ja existia"
    else
        echo "  $1 falhou com $codigo"
    fi
}
usuario Luana     luana     "AD-2024-00101" "Diretoria Tecnica" "Coordenadora"       null                ADMIN
usuario Bruno     bruno     "GE-2024-00214" "Suprimentos"       "Gestor de Estoque"  null                GESTOR
usuario Guilherme guilherme "GE-2024-00318" "Suprimentos"       "Analista"           null                GESTOR
usuario Carolina  carolina  "FA-2023-00087" "Farmacia Central"  "Farmaceutica"       '"CRF-SP 41208"'    FARMACEUTICO
usuario Vitor     vitor     "EN-2025-00330" "Centro Cirurgico"  "Enfermeiro"         '"COREN-SP 512340"' ENFERMEIRO

entrar() {
    post /auth/login "" "$(printf '{"email":"%s@fiap.com.br","senha":"%s"}' "$1" "$SENHA")" > /dev/null
    campo token
}
ADMIN=$(entrar luana)
FARM=$(entrar carolina)
if [ -z "$ADMIN" ]; then
    echo "Nao consegui autenticar. O banco ja tem dados de outra rodada?"
    exit 1
fi

echo
echo "Fornecedores"
fornecedor() {
    post /fornecedores "$ADMIN" \
        "$(printf '{"nome":"%s","cnpj":"%s","email":"%s","telefone":"%s","slaHoras":%s,"scoreConfiabilidade":%s}' "$1" "$2" "$3" "$4" "$5" "$6")" > /dev/null
    campo id
}
F_CRISTALIA=$(fornecedor "Cristalia Produtos Quimicos" 44734671000151 vendas@cristalia.com.br    "(19) 3863-9500" 48 96)
F_ASPEN=$(fornecedor     "Aspen Pharma"                11222333000181 contato@aspen.com.br       "(11) 4083-6200" 72 88)
F_UNIAO=$(fornecedor     "Uniao Quimica"               60665981000118 sac@uniaoquimica.com.br    "(11) 2189-8000" 24 91)
F_ANTIGO=$(fornecedor    "Distribuidora Sao Jorge"     49324221000103 contato@saojorge.com.br    "(11) 3355-1200" 96 42)
curl -s -o /dev/null -X DELETE "$B/fornecedores/$F_ANTIGO" -H "Authorization: Bearer $ADMIN"
echo "  4 fornecedores, um deles inativado"

echo
echo "Itens"
item() {
    post /itens "$FARM" \
        "$(printf '{"nome":"%s","descricao":"%s","categoria":"%s","unidadeMedida":"%s","quantidadeAtual":%s,"quantidadeMinima":%s,"quantidadeMaxima":%s,"localArmazenamento":"%s","fornecedorId":"%s","tipo":"%s","lote":"%s","dataValidade":"%s"}' \
        "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9" "${10}" "${11}" "${12}")" > /dev/null
    campo id
}
read -r V1 V2 V3 V4 V5 V6 V7 V8 V9 V10 <<< "$(python3 -c "
import datetime
h = datetime.date.today()
print(' '.join((h + datetime.timedelta(days=d)).isoformat() for d in (400,900,250,620,180,21,45,1100,300,500)))")"

I_DIPIRONA=$(item   "Dipirona 500mg"           "Analgesico e antitermico"   Medicamento cx  340 80  600  "Armario A3"           "$F_CRISTALIA" PRIMORDIAL              L-2026-0431 "$V1")
I_SORO=$(item       "Soro Fisiologico 0,9%"    "Frasco de 500ml"            Solucao     fr 1200 300 2000 "Deposito Central"     "$F_UNIAO"     PRIMORDIAL              S-2026-1180 "$V2")
I_LUVA=$(item       "Luva de Procedimento M"   "Nitrilo, sem po"            Descartavel cx   60 80  400  "Deposito Central"     "$F_ASPEN"     PRIMORDIAL              LV-2026-077 "$V3")
I_ADRENALINA=$(item "Adrenalina 1mg/ml"        "Ampola, uso em emergencia"  Medicamento un    6 40  200  "Carrinho de Parada"   "$F_CRISTALIA" ESSENCIAL_BAIXA_DEMANDA A-2026-0092 "$V4")
I_N95=$(item        "Mascara N95"              "Protecao respiratoria"      EPI         un    0 200 1500 "Almoxarifado B"       "$F_ASPEN"     PRIMORDIAL              N-2025-2210 "$V5")
I_PROPOFOL=$(item   "Propofol 10mg/ml"         "Anestesico, frasco de 20ml" Medicamento fr   45 30  150  "Camara Fria 2"        "$F_CRISTALIA" ESSENCIAL_BAIXA_DEMANDA P-2026-0015 "$V6")
I_HEPARINA=$(item   "Heparina 5000UI"          "Anticoagulante"             Medicamento fr   90 50  300  "Camara Fria 1"        "$F_UNIAO"     PRIMORDIAL              H-2025-0908 "$V7")
I_SERINGA=$(item    "Seringa Descartavel 10ml" "Com agulha 25x7"            Descartavel un  800 200 3000 "Almoxarifado B"       "$F_ASPEN"     PRIMORDIAL              SR-2026-441 "$V8")
I_GAZE=$(item       "Gaze Esteril 7,5cm"       "Pacote com 10 unidades"     Descartavel pc  150 100 800  "Almoxarifado B"       "$F_UNIAO"     PRIMORDIAL              G-2026-0332 "$V9")
I_FENTANILA=$(item  "Fentanila 50mcg/ml"       "Ampola, controlado"         Medicamento un   25 60  180  "Cofre de Controlados" "$F_CRISTALIA" ESSENCIAL_BAIXA_DEMANDA F-2026-0007 "$V10")
echo "  10 itens"

VENCIDA=$(python3 -c "import datetime;print((datetime.date.today()-datetime.timedelta(days=12)).isoformat())")
curl -s -o /dev/null -X PUT "$B/itens/$I_HEPARINA" -H "$JSON" -H "Authorization: Bearer $FARM" \
    -d "$(printf '{"nome":"Heparina 5000UI","descricao":"Anticoagulante","categoria":"Medicamento","unidadeMedida":"fr","quantidadeAtual":90,"quantidadeMinima":50,"quantidadeMaxima":300,"localArmazenamento":"Camara Fria 1","fornecedorId":"%s","tipo":"PRIMORDIAL","lote":"H-2025-0908","dataValidade":"%s"}' "$F_UNIAO" "$VENCIDA")"
echo "  Heparina com validade vencida em $VENCIDA"

echo
echo "Movimentacoes"
ajustar() {
    curl -s -o /dev/null -X PATCH "$B/itens/$1/quantidade" -H "$JSON" -H "Authorization: Bearer $FARM" \
        -d "$(printf '{"tipo":"%s","quantidade":%s}' "$2" "$3")"
}
for q in 18 24 12 30 15 22; do ajustar "$I_DIPIRONA" SAIDA "$q"; done
for q in 40 65 50 35;       do ajustar "$I_SORO"     SAIDA "$q"; done
for q in 5 8 3;             do ajustar "$I_LUVA"     SAIDA "$q"; done
ajustar "$I_DIPIRONA" ENTRADA 200
ajustar "$I_SERINGA"  SAIDA   120
echo "  16 registros de entrada e saida"

echo
echo "Pedidos"
read -r FUTURO PASSADO HOJE <<< "$(python3 -c "
import datetime
a = datetime.datetime.now(datetime.timezone.utc)
f = '%Y-%m-%dT%H:00:00Z'
print((a + datetime.timedelta(days=6)).strftime(f),
      (a - datetime.timedelta(days=4)).strftime(f),
      a.replace(hour=23).strftime(f))")"

pedido() {
    post /pedidos "$FARM" "$1" > /dev/null
    campo id
}
P1=$(pedido "$(printf '{"fornecedorId":"%s","etaPrevista":"%s","itens":[{"itemId":"%s","quantidade":400,"valorUnitario":1.85},{"itemId":"%s","quantidade":60,"valorUnitario":12.40}]}' "$F_ASPEN" "$FUTURO" "$I_N95" "$I_LUVA")")
echo "  um pendente, chega em 6 dias"

P2=$(pedido "$(printf '{"fornecedorId":"%s","etaPrevista":"%s","itens":[{"itemId":"%s","quantidade":60,"valorUnitario":38.90}]}' "$F_CRISTALIA" "$FUTURO" "$I_ADRENALINA")")
curl -s -o /dev/null -X POST "$B/pedidos/$P2/confirmar" -H "Authorization: Bearer $FARM"
echo "  um confirmado, que deu entrada no estoque"

P3=$(pedido "$(printf '{"fornecedorId":"%s","etaPrevista":"%s","itens":[{"itemId":"%s","quantidade":300,"valorUnitario":0.74}]}' "$F_UNIAO" "$PASSADO" "$I_GAZE")")
echo "  um com o SLA estourado"

P4=$(pedido "$(printf '{"fornecedorId":"%s","etaPrevista":"%s","itens":[{"itemId":"%s","quantidade":500,"valorUnitario":0.42}]}' "$F_UNIAO" "$HOJE" "$I_SERINGA")")
echo "  um chegando hoje, para o dashboard"

echo
echo "Alertas e previsoes"
post /alertas/gerar "$ADMIN" '' > /dev/null
echo "  $(python3 -c "import sys,json;print(len(json.load(sys.stdin)))" < "$CORPO" 2>/dev/null) alertas gerados pela varredura"
post /alertas "$ADMIN" '{"tipo":"IA","severidade":"INFO","titulo":"Sugestao de redistribuicao","mensagem":"A analise indica excesso de Seringa 10ml nesta unidade frente ao consumo dos ultimos 30 dias."}' > /dev/null
echo "  1 alerta manual"
for i in "$I_DIPIRONA" "$I_SORO" "$I_LUVA"; do
    curl -s -o /dev/null -X POST "$B/previsoes/$i/gerar" -H "Authorization: Bearer $FARM"
done
echo "  3 previsoes de demanda"

echo
echo "Pronto. Entre com luana@fiap.com.br e senha $SENHA."
