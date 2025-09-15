# Installing HashiCorp Vault v1.18.3 on Red Hat Enterprise Linux Server



This guide provides step-by-step instructions to install and configure HashiCorp Vault v1.18.3 on a Red Hat Enterprise Linux (RHEL) server.

1. **Prerequisites**
   Before installing Vault, ensure the following:
    - A running RHEL server with sudo or root access.
    - An active internet connection.
    - `wget`, `unzip`, and `gnupg` installed.

2. **Update the System**
    - Update your system packages to the latest versions:
      ```sh
      sudo yum update -y
      ```

3. **Install Required Dependencies**
    - Install necessary tools for downloading and verifying Vault:
      ```sh
      sudo yum install -y wget unzip gnupg
      ```

4. **Download the Vault Binary**
    - Download the Vault v1.18.3 binary from the official HashiCorp releases:
      ```sh
      wget https://releases.hashicorp.com/vault/1.18.3/vault_1.18.3_linux_amd64.zip
      ```

5. **Install Vault**
    - Unzip the downloaded binary:
      ```sh
      unzip vault_1.18.3_linux_amd64.zip
      ```
    - Move the binary to `/usr/local/bin`:
      ```sh
      sudo mv vault /usr/local/bin/
      ```
    - Check the installation by verifying the version:
      ```sh
      vault --version
      ```

6. **Create Vault Configuration**
    - Create Configuration Directories:
      ```sh
      sudo mkdir /etc/vault.d
      sudo mkdir -p /var/lib/vault/data
      ```
    - Set Permissions:
      ```sh
      sudo chown -R vault:vault /etc/vault.d /var/lib/vault
      ```
    - Create the Vault Configuration File:
      ```sh
      sudo nano /etc/vault.d/vault.hcl
      ```
      Add the following content:
      ```hcl
      storage "file" {
        path = "/var/lib/vault/data"
      }
 
      listener "tcp" {
        address       = "0.0.0.0:8200"
        tls_disable   = 1
      }
 
      ui = true
      ```

7. **Create a Systemd Service for Vault**
    - Create a systemd service file:
      ```sh
      sudo nano /etc/systemd/system/vault.service
      ```
    - Add the following content:
      ```ini
      [Service]
      User=vault
      Group=vault
      ProtectSystem=full
      ProtectHome=read-only
      PrivateTmp=yes
      PrivateDevices=yes
      SecureBits=keep-caps
      AmbientCapabilities=CAP_IPC_LOCK
      Capabilities=CAP_IPC_LOCK+ep
      CapabilityBoundingSet=CAP_SYSLOG CAP_IPC_LOCK
      NoNewPrivileges=yes
      ExecStart=/usr/local/bin/vault server -config=/etc/vault.d/vault.hcl
      ExecReload=/bin/kill --signal HUP $MAINPID
      KillMode=process
      KillSignal=SIGINT
      Restart=on-failure
      RestartSec=5
      TimeoutStopSec=30
      StartLimitIntervalSec=60
      StartLimitBurst=3
 
      [Install]
      WantedBy=multi-user.target
      ```
    - Reload systemd:
      ```sh
      sudo systemctl daemon-reload
      ```
    - Enable the Vault service to start on boot:
      ```sh
      sudo systemctl enable vault
      ```

8. **Start and Test Vault**
    - Start the Vault service:
      ```sh
      sudo systemctl start vault
      ```
    - Check the status:
      ```sh
      sudo systemctl status vault
      ```

9. **Initialize and set Vault**
    - Set vault address:
      ```sh
      sudo export VAULT_ADDR='http://127.0.0.1:8200'
      ```
    - Initialize vault:
        1. Create a folder `/opt/vault_scripts`:
           ```sh
           sudo mkdir /opt/vault_scripts
           sudo cd /opt/vault_scripts
           ```
        2. Create a shell script named `monitor_hashicorp_vault_service.sh`:
           ```sh
           sudo nano monitor_hashicorp_vault_service.sh
           ```
        3. Add the following content:
           ```sh
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
                   secret_id_ttl=0 token_ttl=157680000  token_max_ttl=157680000 ; then
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
           ```
        4. Make the script executable:
           ```sh
           chmod +x monitor_hashicorp_vault_service.sh
           ```
        5. Execute the script:
           ```sh
           ./monitor_hashicorp_vault_service.sh
           ```
        6. Set public key, private key, username, and password in vault
        7. Create a script named `rotate_rsa_key_pair_password_in_hashicorp_vault_service.sh`:
           ```sh
           sudo nano rotate_rsa_key_pair_password_in_hashicorp_vault_service.sh
           ```
        8. Add the following content:
           ```sh
           #!/bin/bash
           set -euo pipefail
   
           # Variables
           VAULT_ADDR="http://127.0.0.1:8200"
           KEY_PATH="secret/rsa-key-password"
           VAULT_LOG_PATH="/var/log/vault"
   
           export VAULT_ADDR=$VAULT_ADDR
           export VAULT_NAMESPACE=""
   
           # Logging configuration
           mkdir -p ${VAULT_LOG_PATH}
           exec 1> >(logger -s -t $(basename $0)) 2>&1
   
           log() {
               local level=$1
               shift
               logger -t "vault-set-keys" -p "local0.$level" "$@"
               echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*
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
   
           generate_and_store_keys
           ```
      9. Execute the script:
         ```sh
         ./rotate_rsa_key_pair_password_in_hashicorp_vault_service.sh
         ```
      10. Check secret in vault:
          ```sh
          sudo vault kv get secret/rsa-key-password
          ```
11. Verify Vault status:
    ```sh
    vault status
    ```