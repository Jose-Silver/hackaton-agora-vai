#!/bin/bash

# Configurações da API
BASE_URL="http://localhost:8080"
ENDPOINT="/emprestimos/v1/simulacoes"
URL="${BASE_URL}${ENDPOINT}"

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== Script de Simulações de Empréstimo ===${NC}"
echo "Endpoint: $URL"
echo "Iniciando simulações..."
echo ""

# Arrays com diferentes valores para testar
valores=(5000.00 10000.00 15000.00 25000.00 50000.00 75000.00 100000.00 150000.00 200000.00 300000.00)
prazos=(12 24 36 48 60 72 84 96 108 120)

# Contador de simulações
contador=1

# Loop para gerar simulações variadas
for i in {0..9}; do
    VALOR=${valores[$i]}
    PRAZO=${prazos[$i]}

    echo -e "${GREEN}[Simulação $contador]${NC} Valor: R$ $VALOR - Prazo: $PRAZO meses"

    # Fazer a requisição curl
    response=$(curl -s -w "\n%{http_code}" -X POST "$URL" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "{
            \"valorDesejado\": $VALOR,
            \"prazo\": $PRAZO
        }")

    # Extrair o código de status HTTP
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓ Sucesso (HTTP $http_code)${NC}"
        # Exibir apenas parte da resposta para não poluir o terminal
        echo "$response_body" | head -n 3
    else
        echo -e "${RED}✗ Erro (HTTP $http_code)${NC}"
        echo "$response_body"
    fi

    echo "---"
    ((contador++))

    # Pausa pequena para não sobrecarregar a API
    sleep 1
done

# Simulações adicionais com casos extremos
echo -e "${YELLOW}=== Testando Casos Extremos ===${NC}"

# Valor muito baixo
echo -e "${GREEN}[Simulação $contador]${NC} Testando valor baixo: R$ 100,00"
curl -s -X POST "$URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"valorDesejado": 100.00, "prazo": 12}' | head -n 3
echo "---"
((contador++))

# Prazo muito longo
echo -e "${GREEN}[Simulação $contador]${NC} Testando prazo longo: 180 meses"
curl -s -X POST "$URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"valorDesejado": 50000.00, "prazo": 180}' | head -n 3
echo "---"
((contador++))

# Valor alto
echo -e "${GREEN}[Simulação $contador]${NC} Testando valor alto: R$ 500.000,00"
curl -s -X POST "$URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"valorDesejado": 500000.00, "prazo": 60}' | head -n 3
echo "---"

echo -e "${YELLOW}=== Fim das Simulações ===${NC}"
echo "Total de simulações realizadas: $contador"
