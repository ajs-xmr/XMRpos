#!/bin/bash

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

MONEROPAY_DIR="${MONEROPAY_DIR:-/root/moneropay}"
XMRPOS_DIR="${XMRPOS_DIR:-/root/XMRpos}"
XMRPOS_BACKEND_DIR="${XMRPOS_DIR}/XMRpos-backend"

MONERO_DAEMON_HOST="${MONERO_DAEMON_HOST:-node.monerodevs.org}"
MONERO_DAEMON_PORT="${MONERO_DAEMON_PORT:-18089}"
MONEROPAY_DB_USER="${MONEROPAY_DB_USER:-moneropay}"
MONEROPAY_DB_NAME="${MONEROPAY_DB_NAME:-moneropay}"
MONEROPAY_HOST_PORT="${MONEROPAY_HOST_PORT:-5000}"
MONEROPAY_WALLET_RPC_HOST_PORT="${MONEROPAY_WALLET_RPC_HOST_PORT:-18083}"

XMRPOS_DB_USER="${XMRPOS_DB_USER:-xmrpos}"
XMRPOS_DB_NAME="${XMRPOS_DB_NAME:-xmrpos}"
BACKEND_PORT="${BACKEND_PORT:-8080}"

MONEROPAY_BASE_URL_FOR_XMRPOS="${MONEROPAY_BASE_URL_FOR_XMRPOS:-http://host.docker.internal:5000}"
MONEROPAY_CALLBACK_URL_FOR_XMRPOS="${MONEROPAY_CALLBACK_URL_FOR_XMRPOS:-http://host.docker.internal:8080/callback/receive/{jwt}}"
MONERO_WALLET_RPC_ENDPOINT_FOR_XMRPOS="${MONERO_WALLET_RPC_ENDPOINT_FOR_XMRPOS:-http://host.docker.internal:18083/json_rpc}"

require_root() {
    if [[ "$(id -u)" -ne 0 ]]; then
        echo -e "${RED}Please run this script with sudo or as root.${NC}"
        exit 1
    fi
}

ensure_prereqs() {
    echo -e "${GREEN}Installing prerequisites...${NC}"
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq git curl ca-certificates jq openssl >/dev/null
}

random_hex() {
    openssl rand -hex "${1:-32}"
}

update_env_var() {
    local file="$1" key="$2" value="$3"
    python3 - <<PY
from pathlib import Path
path = Path("$file")
lines = path.read_text().splitlines() if path.exists() else []
for idx,line in enumerate(lines):
    if line.startswith("$key="):
        lines[idx] = "$key=$value"
        break
else:
    lines.append("$key=$value")
path.write_text("\n".join(lines) + "\n")
PY
}

install_moneropay() {
    if [[ ! -d "$MONEROPAY_DIR" ]]; then
        echo -e "${GREEN}Cloning MoneroPay...${NC}"
        git clone https://gitlab.com/moneropay/moneropay.git "$MONEROPAY_DIR"
    fi

    cd "$MONEROPAY_DIR"

    [[ -f .env ]] || cp .env.example .env
    [[ -f docker-compose.override.yaml ]] || cp docker-compose.override.yaml.example docker-compose.override.yaml

    local postgres_password="${MONEROPAY_DB_PASSWORD:-$(random_hex 12)}"

    update_env_var ".env" "MONERO_DAEMON_RPC_HOSTNAME" "$MONERO_DAEMON_HOST"
    update_env_var ".env" "MONERO_DAEMON_RPC_PORT" "$MONERO_DAEMON_PORT"
    update_env_var ".env" "MONERO_DAEMON_RPC_USERNAME" ""
    update_env_var ".env" "MONERO_DAEMON_RPC_PASSWORD" ""
    update_env_var ".env" "POSTGRES_USERNAME" "$MONEROPAY_DB_USER"
    update_env_var ".env" "POSTGRES_PASSWORD" "$postgres_password"
    update_env_var ".env" "POSTGRES_DATABASE" "$MONEROPAY_DB_NAME"
    update_env_var ".env" "ZERO_CONF" "true"
    update_env_var ".env" "LOG_FORMAT" "pretty"
    update_env_var ".env" "POLL_FREQUENCY" "1"

    cat > docker-compose.override.yaml <<EOF
services:
  moneropay:
    ports:
      - ${MONEROPAY_HOST_PORT}:5000
  monero-wallet-rpc:
    ports:
      - ${MONEROPAY_WALLET_RPC_HOST_PORT}:28081
EOF

    echo -e "${GREEN}Starting MoneroPay stack...${NC}"
    docker compose up -d

    echo -e "${YELLOW}MoneroPay containers:${NC}"
    docker compose ps

    echo -e "${YELLOW}Waiting for MoneroPay health endpoint (takes about 30s)...${NC}"
    local health_url="http://127.0.0.1:${MONEROPAY_HOST_PORT}/health"
    local attempts=10
    while (( attempts-- > 0 )); do
        if curl -fsS "${health_url}" >/tmp/moneropay-health 2>/dev/null; then
            cat /tmp/moneropay-health | jq .
            if jq -e '.status == 200 and .services.walletrpc == true' /tmp/moneropay-health >/dev/null 2>&1; then
                echo -e "${GREEN}Enabling wallet auto-refresh (2s) via RPC...${NC}"
                curl -s -H 'Content-Type: application/json' \
                    -d '{"jsonrpc":"2.0","id":"auto_refresh","method":"auto_refresh","params":{"enable":true,"period":2}}' \
                    "http://127.0.0.1:${MONEROPAY_WALLET_RPC_HOST_PORT}/json_rpc" >/dev/null || true
            fi
            rm -f /tmp/moneropay-health
            return
        fi
        sleep 3
    done
    rm -f /tmp/moneropay-health 2>/dev/null || true
    echo -e "${RED}MoneroPay health endpoint not reachable yet. Check logs with 'cd ${MONEROPAY_DIR} && docker compose logs -f'.${NC}"
}

install_xmrpos() {
    if [[ ! -d "$XMRPOS_DIR" ]]; then
        echo -e "${GREEN}Cloning XMRpos...${NC}"
        git clone https://github.com/MoneroKon/XMRpos "$XMRPOS_DIR"
    fi

    cd "$XMRPOS_BACKEND_DIR"

    cat > .env <<EOF
ADMIN_NAME=${ADMIN_NAME:-admin}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-$(random_hex 8)}

PORT=${BACKEND_PORT}

DB_HOST=db
DB_USER=${XMRPOS_DB_USER}
DB_PASSWORD=${XMRPOS_DB_PASSWORD:-$(random_hex 16)}
DB_NAME=${XMRPOS_DB_NAME}
DB_PORT=5432

JWT_SECRET=$(random_hex 32)
JWT_REFRESH_SECRET=$(random_hex 32)
JWT_MONEROPAY_SECRET=$(random_hex 32)

MONEROPAY_BASE_URL=${MONEROPAY_BASE_URL_FOR_XMRPOS}
MONEROPAY_CALLBACK_URL=${MONEROPAY_CALLBACK_URL_FOR_XMRPOS}

MONERO_WALLET_RPC_ENDPOINT=${MONERO_WALLET_RPC_ENDPOINT_FOR_XMRPOS}
MONERO_WALLET_RPC_USERNAME=
MONERO_WALLET_RPC_PASSWORD=

WALLET_NAME=wallet
WALLET_PASSWORD=
EOF

    echo -e "${GREEN}Building XMRpos backend image...${NC}"
    docker compose build --no-cache

    echo -e "${GREEN}Starting XMRpos stack...${NC}"
    docker compose up -d

    echo -e "${YELLOW}XMRpos containers:${NC}"
    docker compose ps

    echo -e "${YELLOW}Waiting for XMRpos health endpoint...${NC}"
    local health_url="http://127.0.0.1:${BACKEND_PORT}/misc/health"
    sleep 5
    local attempts=10
    while (( attempts-- > 0 )); do
        if curl -fsS "${health_url}" >/tmp/xmrpos-health 2>/dev/null; then
            cat /tmp/xmrpos-health | jq .
            rm -f /tmp/xmrpos-health
            return
        fi
        sleep 3
    done
    rm -f /tmp/xmrpos-health 2>/dev/null || true
    echo -e "${RED}XMRpos health endpoint not reachable yet. Check logs with 'cd ${XMRPOS_BACKEND_DIR} && docker compose logs -f'.${NC}"
}

install_all() {
    require_root
    ensure_prereqs
    install_moneropay
    install_xmrpos
    echo -e "${GREEN}Combined MoneroPay + XMRpos installation complete.${NC}"
}

clean_all() {
    require_root
    echo -e "${RED}WARNING: This will remove MoneroPay and XMRpos, including wallets under ${MONEROPAY_DIR}/data/wallet and ~/wallets. Make sure you have backups before continuing.${NC}"
    read -r -p "Type 'delete' to confirm cleanup: " response
    if [[ "$response" != "delete" ]]; then
        echo -e "${YELLOW}Cleanup cancelled.${NC}"
        exit 0
    fi

    if [[ -d "$XMRPOS_BACKEND_DIR" ]]; then
        echo -e "${YELLOW}Stopping XMRpos stack...${NC}"
        (cd "$XMRPOS_BACKEND_DIR" && docker compose down -v >/dev/null 2>&1 || true)
        rm -rf "$XMRPOS_DIR"
    fi

    if [[ -d "$MONEROPAY_DIR" ]]; then
        echo -e "${YELLOW}Stopping MoneroPay stack...${NC}"
        (cd "$MONEROPAY_DIR" && docker compose down -v >/dev/null 2>&1 || true)
        rm -rf "$MONEROPAY_DIR"
    fi

    if [[ -d "/root/wallets" ]]; then
        echo -e "${YELLOW}Removing wallet directory /root/wallets...${NC}"
        rm -rf /root/wallets
    fi

    echo -e "${GREEN}Cleanup complete. System is ready for a fresh installation.${NC}"
}

case "${1:-install}" in
    install)
        install_all
        ;;
    clean)
        clean_all
        ;;
    *)
        echo -e "${RED}Usage: $0 [install|clean]${NC}"
        exit 1
        ;;
esac
