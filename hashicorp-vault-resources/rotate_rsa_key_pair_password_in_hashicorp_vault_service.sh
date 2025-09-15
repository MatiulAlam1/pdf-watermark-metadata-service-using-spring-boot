
#!/bin/bash
set -euo pipefail

# Variables
PRIMARY_VAULT_ADDR="http://127.0.0.1:8200"
KEY_PATH="secret/rsa-key-password"
VAULT_LOG_PATH="/var/log/vault"
SECONDARY_VAULT_ADDR="http://v4520a.unx.vstage.co:8200"
SECONDARY_KEY_PATH="secret/rsa-key-password"

export VAULT_ADDR=$PRIMARY_VAULT_ADDR
export VAULT_NAMESPACE=""

# Logging configuration
mkdir -p ${VAULT_LOG_PATH}
exec 1> >(logger -s -t $(basename $0)) 2>&1

log() {
    local level=$1
    shift
    logger -t "vault-set-keys" -p "local0.$level" "$@"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*"
}

# Generate RSA keys and store in Vault
generate_and_store_keys() {
    log "info" "Generating RSA keys..."
    openssl genrsa -out private.pem 2048
    # Extract public key
    openssl rsa -in private.pem -pubout -out public.pem
    # Read keys into variables
    log "info" "Storing keys in Vault..."
    #PRIVATE_KEY=$(cat private.pem)
    #PUBLIC_KEY=$(cat public.pem)
    PRIVATE_KEY=$(sed -e '/^-----BEGIN PRIVATE KEY-----$/d' -e '/^-----END PRIVATE KEY-----$/d' private.pem)
    PUBLIC_KEY=$(sed -e '/^-----BEGIN PUBLIC KEY-----$/d' -e '/^-----END PUBLIC KEY-----$/d' public.pem)
    PASSWORD="Valmet@231"
    USERNAME="valmet_watermark_generator"
    # Store keys and password in Vault
    vault kv put $KEY_PATH private_key="$PRIVATE_KEY" public_key="$PUBLIC_KEY" watermark.password="$PASSWORD" watermark.username="$USERNAME"
    # Clean up local files after storing in Vault
    rm private.pem public.pem
    log "info" "Keys generated and stored successfully in Vault."
    log "info" "Public and private keys updated on $(date '+%Y-%m-%d %H:%M:%S')."

}

sync_keys_to_secondary_vault() {
    PRIMARY_KEY_PATH="secret/data/rsa-key-password"
    PRIMARY_ROLE_ID="7d1d6fcb-3e92-d815-bc62-56ce951be46c"
    PRIMARY_SECRET_ID="a14e1c2f-94bb-3b4b-ee22-b3706b8ef553"


    SECONDARY_ROLE_ID="7d1d6fcb-3e92-d815-bc62-56ce951be46c"
    SECONDARY_SECRET_ID="eb383526-c39d-3b25-bdcc-d9130b519465"

    log "info" "Authenticating to primary Vault ($PRIMARY_VAULT_ADDR)..."
    PRIMARY_TOKEN=$(curl --silent --request POST \
    --data "{\"role_id\": \"$PRIMARY_ROLE_ID\", \"secret_id\": \"$PRIMARY_SECRET_ID\"}" \
    "$PRIMARY_VAULT_ADDR/v1/auth/approle/login" | jq -r '.auth.client_token')
    log "info" "Fetching latest key from primary Vault..."
    KEY_DATA=$(curl --silent --header "X-Vault-Token: $PRIMARY_TOKEN" \
    "$PRIMARY_VAULT_ADDR/v1/$PRIMARY_KEY_PATH")

    PRIVATE_KEY=$(echo "$KEY_DATA" | jq -r '.data.data.private_key')
    PUBLIC_KEY=$(echo "$KEY_DATA" | jq -r '.data.data.public_key')
    # Print the keys
    echo "============== Retrieved Keys =============="
    echo "PUBLIC KEY:"
    echo "$PUBLIC_KEY"
    echo ""
    echo "PRIVATE KEY:"
    echo "$PRIVATE_KEY"
    echo "=========================================="

    PASSWORD="Valmet@231"
    USERNAME="valmet_watermark_generator"

    log "info" "Writing keys to local Vault ($SECONDARY_VAULT_ADDR)..."
    log "info" "Authenticating to secondary vault..."
    SECONDARY_TOKEN=$(curl --silent --request POST \
      --data "{\"role_id\": \"$SECONDARY_ROLE_ID\", \"secret_id\": \"$SECONDARY_SECRET_ID\"}" \
      "$SECONDARY_VAULT_ADDR/v1/auth/approle/login" | jq -r '.auth.client_token')

    if [[ -z "$SECONDARY_TOKEN" || "$SECONDARY_TOKEN" == "null" ]]; then
        log "error" "Failed to obtain token from secondary vault"
        return 1
    fi

    export VAULT_ADDR=$SECONDARY_VAULT_ADDR
    export VAULT_TOKEN="$SECONDARY_TOKEN"
    export VAULT_NAMESPACE=""
    # Assume local token or approle already setup
    vault kv put $SECONDARY_KEY_PATH private_key="$PRIVATE_KEY" public_key="$PUBLIC_KEY" watermark.username="$USERNAME" watermark.password="$PASSWORD"

    log "info" "Keys successfully synced to secondary Vault."
    unset VAULT_TOKEN
    export VAULT_ADDR=$PRIMARY_VAULT_ADDR
   # export VAULT_ADDR=$SECONDARY_VAULT_ADDR
   # export VAULT_NAMESPACE=""
    # Assume local token or approle already setup
   # vault kv put $SECONDARY_KEY_PATH private_key="$PRIVATE_KEY" public_key="$PUBLIC_KEY" watermark.username="$USERNAME" watermark.password="$PASSWORD"

   # log "info" "Keys successfully synced to secondary Vault."
   # export VAULT_ADDR=$PRIMARY_VAULT_ADDR

}

if generate_and_store_keys;then
    sync_keys_to_secondary_vault
else
    log "error" "Failed to generate and store keys"
fi

