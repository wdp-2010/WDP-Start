#!/bin/bash

# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║                         WDP-Start Deploy Script                            ║
# ║                   Professional Quest System Deployment                     ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

# Don't exit on error - we need to handle errors gracefully
set +e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
PLUGIN_NAME="WDP-Start"
PLUGIN_VERSION="2.1.1"
JAR_NAME="WDP-Start-${PLUGIN_VERSION}.jar"
CONTAINER_ID_DEV="b8f24891-b5be-4847-a96e-c705c500aece"
CONTAINER_ID_MAIN="27298ea1-0c1b-4b41-a5aa-a7d29ff04566"

# Determine which server to deploy to
if [ "$1" == "main" ] || [ "$1" == "production" ] || [ "$1" == "prod" ]; then
    CONTAINER_ID="$CONTAINER_ID_MAIN"
    SERVER_NAME="MAIN"
else
    CONTAINER_ID="$CONTAINER_ID_DEV"
    SERVER_NAME="DEV"
fi

SERVER_DIR="/var/lib/pterodactyl/volumes/${CONTAINER_ID}"
PLUGIN_DIR="${SERVER_DIR}/plugins"

# Header
echo -e "${CYAN}"
echo "╔═══════════════════════════════════════════════════════════════════════════╗"
echo "║                    WDP-Start Deploy Script (${SERVER_NAME})                        ║"
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

BUILD_EXIT=$?
if [ $BUILD_EXIT -ne 0 ]; then
    print_error "Build failed with exit code $BUILD_EXIT"
    exit 1
fi

if [ ! -f "target/${JAR_NAME}" ]; then
    print_error "Build failed! JAR not found: target/${JAR_NAME}"
    exit 1
fi

print_success "Build completed: target/${JAR_NAME}"

# Step 2: Stop the server container
print_step "Stopping server container..."

# Find the actual running container by the pterodactyl container ID
CONTAINER_FULL_ID=$(docker ps -a --filter "name=b8f24891-b5be-4847-a96e-c705c500aece" --format "{{.ID}}" | head -1)

if [ -z "$CONTAINER_FULL_ID" ]; then
    print_error "Could not find container!"
    exit 1
fi

print_step "Found container: $CONTAINER_FULL_ID"

# Stop the container
docker stop "$CONTAINER_FULL_ID" 2>/dev/null
STOP_EXIT=$?

if [ $STOP_EXIT -eq 0 ]; then
    print_step "Waiting for container to stop..."
    WAIT_COUNT=0
    while docker ps -q --filter "id=$CONTAINER_FULL_ID" 2>/dev/null | grep -q . 2>/dev/null; do
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
        if [ $WAIT_COUNT -ge 15 ]; then
            print_warning "Container slow to stop, killing..."
            docker kill "$CONTAINER_FULL_ID" 2>/dev/null
            sleep 2
            break
        fi
    done
    print_success "Container stopped (took ${WAIT_COUNT}s)"
else
    print_warning "Container stop command failed, but continuing..."
fi

# Safety wait for file handles
sleep 2

# Step 3: Remove old plugin if exists
if [ -f "${PLUGIN_DIR}/${JAR_NAME}" ]; then
    rm -f "${PLUGIN_DIR}/${JAR_NAME}"
fi

# Step 3.5: Remove old shop directory to ensure fresh deployment
if [ -d "${PLUGIN_DIR}/${PLUGIN_NAME}/SkillCoinsShop" ]; then
    print_step "Removing old SkillCoinsShop directory..."
    rm -rf "${PLUGIN_DIR}/${PLUGIN_NAME}/SkillCoinsShop"
    print_success "Old shop removed"
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

if [ ! -f "${CONFIG_DIR}/navbar.yml" ]; then
    print_step "Copying default navbar.yml..."
    cp "src/main/resources/navbar.yml" "${CONFIG_DIR}/"
fi

# Copy SkillCoinsShop directory (always fresh)
print_step "Copying SkillCoinsShop directory..."
cp -r "src/main/resources/SkillCoinsShop" "${CONFIG_DIR}/"
print_success "SkillCoinsShop copied"

# Step 6: Set permissions
print_step "Setting file permissions..."
chown -R pterodactyl:pterodactyl "${PLUGIN_DIR}/${JAR_NAME}" 2>/dev/null || true
chown -R pterodactyl:pterodactyl "${CONFIG_DIR}" 2>/dev/null || true

# Step 7: Start the server
print_step "Starting server container..."

# Start container using the full container ID we found earlier
docker start "$CONTAINER_FULL_ID" 2>&1
START_EXIT=$?

if [ $START_EXIT -ne 0 ]; then
    print_warning "Start command returned error code $START_EXIT, but checking if running anyway..."
fi

# Wait for container to actually start
print_step "Verifying container is running..."
sleep 3
VERIFY_COUNT=$(docker ps -q --filter "id=$CONTAINER_FULL_ID" 2>/dev/null | wc -l)

if [ $VERIFY_COUNT -gt 0 ]; then
    print_success "Container is running!"
    print_step "Waiting for server to initialize (20s)..."
    INIT_FAIL=0
    for i in $(seq 1 20); do
        STILL_RUNNING=$(docker ps -q --filter "id=$CONTAINER_FULL_ID" 2>/dev/null | wc -l)
        if [ $STILL_RUNNING -eq 0 ]; then
            print_error "Container stopped unexpectedly during initialization!"
            INIT_FAIL=1
            break
        fi
        sleep 1
    done
    
    if [ $INIT_FAIL -eq 0 ]; then
        print_success "Server initialization complete"
    else
        exit 1
    fi
else
    print_error "Container failed to start! Status:"
    docker ps -a --filter "id=$CONTAINER_FULL_ID" --format "ID: {{.ID}}\nName: {{.Names}}\nStatus: {{.Status}}"
    exit 1
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
