#!/bin/bash
set -euo pipefail

# Variables
VAULT_ADDR="http://127.0.0.1:8200"
VAULT_CONFIG_PATH="/etc/vault.d/vault.hcl"
VAULT_LOG_PATH="/var/log/vault"
KEY_PATH="secret/rsa-key-password"
UNSEAL_KEY_FILE="/opt/vault_scripts/unseal_keys.txt"
VAULT_POLICY_NAME="rsa-key-policy"
APPROLE_NAME="rsa-key-role"

export VAULT_ADDR=$VAULT_ADDR

# Logging configuration
mkdir -p ${VAULT_LOG_PATH}
exec 1> >(logger -s -t $(basename $0)) 2>&1

log() {
    local level=$1
    shift
    logger -t "vault-maintenance" -p "local0.$level" "$@"
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*"
}

# Function to check if Vault is up
check_vault_status() {
    vault status > /dev/null 2>&1
    return $?
}

# Function to start the Vault server
start_vault_server() {
    log "info" "Starting Vault server..."
    vault server -config=$VAULT_CONFIG_PATH &
    sleep 2
}

# Function to check if Vault is initialized
is_vault_initialized() {
    local status_output
    status_output=$(vault status)
    log "info" "Vault status: $status_output"
    if echo "$status_output" | grep -q 'Initialized *true'; then
      return 0
    else
      return 1
    fi
}



is_vault_sealed() {
    if vault status | grep -q 'Sealed *true';then
      return 1
    else
      return 0
    fi
}


# Function to initialize the Vault
initialize_vault() {
    if is_vault_initialized; then
        log "info" "Vault is already initialized."
    else
        log "info" "Initializing Vault..."
        INIT_OUTPUT=$(vault operator init -format=json)
        if [ $? -ne 0 ]; then
            log "error" "Failed to initialize Vault."
            exit 1
        fi
        UNSEAL_KEYS=($(echo $INIT_OUTPUT | jq -r '.unseal_keys_b64[]'))
        ROOT_TOKEN=$(echo $INIT_OUTPUT | jq -r '.root_token')

        # Create unseal keys file if it does not exist
        if [ ! -f $UNSEAL_KEY_FILE ]; then
            touch $UNSEAL_KEY_FILE
            if [ $? -ne 0 ]; then
                log "error" "Failed to create unseal keys file."
                exit 1
            fi
            # Store unseal keys in a file
            echo "${UNSEAL_KEYS[*]}" > $UNSEAL_KEY_FILE
            if [ $? -ne 0 ]; then
                log "error" "Failed to write unseal keys to file."
                exit 1
            fi
        fi
        log "info" "Vault initialized successfully."
    fi
}


# Function to retrieve unseal keys from file
retrieve_unseal_keys() {
    log "info" "Retrieving unseal keys from file..."
    IFS=' ' read -r -a UNSEAL_KEYS < $UNSEAL_KEY_FILE
}

# Function to unseal the Vault
unseal_vault() {
    log "info" "Unsealing Vault..."
    retrieve_unseal_keys
    for key in "${UNSEAL_KEYS[@]:0:3}"; do
        vault operator unseal $key
    done
}

# Function to enable KV secrets engine
enable_kv_secrets_engine() {
    if ! vault secrets list | grep -q '^secret/'; then
        log "info" "Enabling KV secrets engine..."
        vault secrets enable -path=secret/ kv-v2
    else
        log "info" "KV secrets engine is already enabled."
    fi
}

# Function to create a policy
create_policy() {
    log "info" "Creating Vault policy..."
    if ! vault policy write $VAULT_POLICY_NAME - <<EOF
path "secret/data/rsa-key-password" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
path "secret/data/rsa-key-password?version=*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
EOF
    then
        log "error" "Failed to create policy!"
        exit 1
    fi
}

# Function to setup AppRole
setup_approle() {
    log "info" "Setting up AppRole..."
    if vault auth list | grep -q 'approle/'; then
        log "info" "AppRole is already enabled!"
    else
        if ! vault auth enable approle; then
            log "error" "Failed to enable AppRole!"
            exit 1
        fi
    fi

    # Write the role with specified policies
    if ! vault write auth/approle/role/$APPROLE_NAME token_policies="$VAULT_POLICY_NAME" \
        secret_id_ttl=0 token_ttl=24h  token_max_ttl=32d token_period=24h; then
        log "error" "Failed to write AppRole!"
        exit 1
    fi

    # Retrieve and store the RoleID securely
    ROLE_ID=$(vault read -field=role_id auth/approle/role/$APPROLE_NAME/role-id)
    log "info" "Role ID: $ROLE_ID"

    # Check if SecretID file exists, create if it does not
    SECRET_ID_FILE="/opt/vault_scripts/secret_id.txt"
    if [ -f $SECRET_ID_FILE ]; then
        SECRET_ID=$(cat $SECRET_ID_FILE)
        log "info" "Using existing Secret ID: $SECRET_ID"
    else
        # Generate and store the SecretID securely
        SECRET_ID=$(vault write -f -field=secret_id auth/approle/role/$APPROLE_NAME/secret-id)
        echo $SECRET_ID > $SECRET_ID_FILE
        log "info" "Generated new Secret ID: $SECRET_ID"
    fi
}

# Function to set private and public keys in Vault
set_keys_in_vault() {
    log "info" "Setting private and public keys in Vault..."
    # Generate new RSA keys
    openssl genrsa -out private.pem 2048
    openssl rsa -in private.pem -pubout -out public.pem

    # Read keys into variables
    PRIVATE_KEY=$(cat private.pem)
    PUBLIC_KEY=$(cat public.pem)

    # Authenticate with Vault using AppRole
    VAULT_TOKEN=$(vault write -field=token auth/approle/login role_id=$ROLE_ID secret_id=$SECRET_ID)

    # Store new keys in Vault
    vault kv put $KEY_PATH private_key="$PRIVATE_KEY" public_key="$PUBLIC_KEY" -token=$VAULT_TOKEN

    # Clean up local files
    rm private.pem public.pem

    log "info" "Keys set successfully in Vault."
}

# Main script execution
if check_vault_status; then
    log "info" "Vault is up and running."
else
    log "warning" "Vault is down. Starting Vault server..."
    start_vault_server
    initialize_vault
    if is_vault_sealed;then
      unseal_vault
    fi
    enable_kv_secrets_engine
    create_policy
    setup_approle
    #set_keys_in_vault
fi
