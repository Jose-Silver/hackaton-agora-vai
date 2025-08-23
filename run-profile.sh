#!/bin/bash

# Script para facilitar o uso dos perfis do Quarkus

echo "==================================="
echo "  Gerenciador de Perfis Quarkus"
echo "==================================="
echo ""
echo "Perfis disponíveis:"
echo "1. Desenvolvimento (dev) - SQL Server Dev Services + H2"
echo "2. Produção (prod) - SQL Server Azure + H2"
echo "3. Testar conexão com banco de produção"
echo "4. Limpar dados de desenvolvimento"
echo "5. Ver logs de aplicação"
echo ""

read -p "Escolha uma opção (1-5): " opcao

case $opcao in
    1)
        echo "Iniciando aplicação em modo desenvolvimento..."
        echo "- SQL Server será iniciado automaticamente via Dev Services"
        echo "- H2 console disponível em http://localhost:8080/h2"
        echo "- Aplicação disponível em http://localhost:8080"
        echo ""
        mvn quarkus:dev -Dquarkus.profile=dev
        ;;
    2)
        echo "Compilando e executando em modo produção..."
        echo "- Conectando ao SQL Server Azure"
        echo "- H2 em modo arquivo persistente"
        echo ""
        mvn clean package -Dquarkus.profile=prod
        if [ $? -eq 0 ]; then
            echo "Iniciando aplicação em produção..."
            java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar
        else
            echo "Erro na compilação!"
        fi
        ;;
    3)
        echo "Testando conexão com banco de produção..."
        echo "Host: dbhackathon.database.windows.net"
        echo "Porta: 1433"
        echo "Database: hack"
        echo ""
        # Teste simples de conectividade
        nc -z dbhackathon.database.windows.net 1433
        if [ $? -eq 0 ]; then
            echo "✅ Conectividade com o servidor OK"
        else
            echo "❌ Não foi possível conectar ao servidor"
            echo "Verifique sua conexão de rede e firewall"
        fi
        ;;
    4)
        echo "Limpando dados de desenvolvimento..."
        rm -rf data/testdb*
        rm -rf target/
        echo "✅ Dados limpos!"
        ;;
    5)
        echo "Últimos logs da aplicação:"
        echo "========================="
        if [ -f "quarkus.log" ]; then
            tail -50 quarkus.log
        else
            echo "Nenhum arquivo de log encontrado"
        fi
        ;;
    *)
        echo "Opção inválida!"
        ;;
esac
