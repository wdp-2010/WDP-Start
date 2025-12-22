#!/bin/bash

# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║                         WDP-Start Deploy Script                            ║
# ║                   Professional Quest System Deployment                     ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
PLUGIN_NAME="WDP-Start"
PLUGIN_VERSION="1.0.0"
JAR_NAME="WDP-Start-${PLUGIN_VERSION}.jar"
CONTAINER_ID="b8f24891-b5be-4847-a96e-c705c500aece"
SERVER_DIR="/var/lib/pterodactyl/volumes/${CONTAINER_ID}"
PLUGIN_DIR="${SERVER_DIR}/plugins"

# Header
echo -e "${CYAN}"
echo "╔═══════════════════════════════════════════════════════════════════════════╗"
echo "║                         WDP-Start Deploy Script                            ║"
echo "║                        Version: ${PLUGIN_VERSION}                                     ║"
echo "╚═══════════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Function: Print step
print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Function: Print success
print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function: Print warning
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function: Print error
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running from correct directory
if [ ! -f "pom.xml" ]; then
    print_error "Please run this script from the WDP-Start project directory"
    exit 1
fi

# Step 1: Build the project
print_step "Building ${PLUGIN_NAME}..."
mvn clean package -DskipTests -q

if [ ! -f "target/${JAR_NAME}" ]; then
    print_error "Build failed! JAR not found: target/${JAR_NAME}"
    exit 1
fi

print_success "Build completed: target/${JAR_NAME}"

# Step 2: Stop the server (robust Pterodactyl Docker handling)
print_step "Stopping server container..."

# Get docker container name from pterodactyl container ID
DOCKER_CONTAINER=$(docker ps -a --format '{{.Names}}' | grep -i "${CONTAINER_ID:0:12}" | head -1)

if [ -z "$DOCKER_CONTAINER" ]; then
    DOCKER_CONTAINER="${CONTAINER_ID:0:12}"
fi

# Check if container is running
if docker ps -q --filter "name=${DOCKER_CONTAINER}" | grep -q . 2>/dev/null || docker ps -q --filter "id=${CONTAINER_ID:0:12}" | grep -q . 2>/dev/null; then
    docker stop "$DOCKER_CONTAINER" > /dev/null 2>&1 || docker stop "${CONTAINER_ID:0:12}" > /dev/null 2>&1
    
    print_step "Waiting for container to stop..."
    STOP_TIMEOUT=30
    STOP_COUNTER=0
    
    while docker ps -q --filter "name=${DOCKER_CONTAINER}" | grep -q . 2>/dev/null || docker ps -q --filter "id=${CONTAINER_ID:0:12}" | grep -q . 2>/dev/null; do
        sleep 1
        STOP_COUNTER=$((STOP_COUNTER + 1))
        
        if [ $STOP_COUNTER -ge $STOP_TIMEOUT ]; then
            print_warning "Container did not stop within ${STOP_TIMEOUT} seconds!"
            print_step "Forcing container stop..."
            docker kill "$DOCKER_CONTAINER" > /dev/null 2>&1 || docker kill "${CONTAINER_ID:0:12}" > /dev/null 2>&1
            sleep 2
            break
        fi
    done
    
    print_success "Container stopped (took ${STOP_COUNTER}s)"
else
    print_warning "Container is already stopped or not found"
fi

# Safety wait for file handles to release
sleep 2

# Step 3: Backup existing plugin
if [ -f "${PLUGIN_DIR}/${JAR_NAME}" ]; then
    print_step "Backing up existing plugin..."
    BACKUP_NAME="${PLUGIN_NAME}-backup-$(date +%Y%m%d_%H%M%S).jar"
    mv "${PLUGIN_DIR}/${JAR_NAME}" "${PLUGIN_DIR}/${BACKUP_NAME}" 2>/dev/null || true
    print_success "Backup created: ${BACKUP_NAME}"
fi

# Step 4: Copy new JAR
print_step "Copying new plugin to server..."
cp "target/${JAR_NAME}" "${PLUGIN_DIR}/"
print_success "Plugin copied to ${PLUGIN_DIR}/"

# Step 5: Copy config files if they don't exist
CONFIG_DIR="${PLUGIN_DIR}/${PLUGIN_NAME}"
if [ ! -d "${CONFIG_DIR}" ]; then
    print_step "Creating config directory..."
    mkdir -p "${CONFIG_DIR}"
fi

# Copy default configs if they don't exist
if [ ! -f "${CONFIG_DIR}/config.yml" ]; then
    print_step "Copying default config.yml..."
    cp "src/main/resources/config.yml" "${CONFIG_DIR}/"
fi

if [ ! -f "${CONFIG_DIR}/messages.yml" ]; then
    print_step "Copying default messages.yml..."
    cp "src/main/resources/messages.yml" "${CONFIG_DIR}/"
fi

# Step 6: Set permissions
print_step "Setting file permissions..."
chown -R pterodactyl:pterodactyl "${PLUGIN_DIR}/${JAR_NAME}" 2>/dev/null || true
chown -R pterodactyl:pterodactyl "${CONFIG_DIR}" 2>/dev/null || true

# Step 7: Start the server
print_step "Starting server container..."

docker start "$DOCKER_CONTAINER" > /dev/null 2>&1 || docker start "${CONTAINER_ID:0:12}" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    print_success "Container started"
    
    # Wait for startup and show status
    print_step "Waiting for server to initialize (15s)..."
    sleep 15
    
    # Check container health
    if docker ps -q --filter "name=${DOCKER_CONTAINER}" | grep -q . 2>/dev/null || docker ps -q --filter "id=${CONTAINER_ID:0:12}" | grep -q . 2>/dev/null; then
        print_success "Server is running"
    else
        print_warning "Server may have stopped - check Pterodactyl console"
    fi
else
    print_warning "Could not start container automatically"
    print_step "Please start the server manually from Pterodactyl panel"
fi

# Summary
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                        Deployment Complete!                                ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Plugin:${NC} ${PLUGIN_NAME} v${PLUGIN_VERSION}"
echo -e "${CYAN}JAR:${NC} ${PLUGIN_DIR}/${JAR_NAME}"
echo -e "${CYAN}Config:${NC} ${CONFIG_DIR}/"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Wait for server to start"
echo "  2. Check console for errors"
echo "  3. Test with /quests command"
echo ""
