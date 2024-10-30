#!/bin/bash

# Variables for easier customization
MINECRAFT_DIR="/opt/minecraft-server"   # Directory to install the server
JAR_URL="https://launcher.mojang.com/v1/objects/c8f83c5655308435b3dcf03c06d9fe8740a77469/server.jar" # Minecraft server download link
XMS="1G"               # Initial server memory
XMX="2G"               # Max server memory
SERVER_PORT="25565"    # Minecraft server port
RCON_PORT="25575"      # RCON port for remote server control
WORLD_NAME="minecraft_world"  # Default world name
MOTD="Welcome to My Minecraft Server"   # Message of the day (shown on server list)

# Update package lists and install required packages
sudo apt update && sudo apt upgrade -y
sudo apt install openjdk-17-jre-headless screen ufw -y 

# Create Minecraft server directory
sudo mkdir -p "$MINECRAFT_DIR"
cd "$MINECRAFT_DIR"

# Download the Minecraft server JAR file
echo "Downloading Minecraft server..."
sudo wget -O server.jar "$JAR_URL"

# Accept the Minecraft EULA
echo "eula=true" | sudo tee eula.txt > /dev/null

# Create server.properties with default settings
echo "Creating server.properties file..."
sudo tee server.properties > /dev/null <<EOL
# Minecraft server properties
enable-jmx.monitoring=false
rcon.port=$RCON_PORT
level-seed=
gamemode=survival
enable-command-block=false
enable-query=false
generator-settings={}
level-name=$WORLD_NAME
motd=$MOTD
query.port=$SERVER_PORT
pvp=true
generate-structures=true
difficulty=normal
network-compression-threshold=256
require-resource-pack=false
max-tick-time=60000
EOL

# Enable UFW if not enabled
if ! sudo ufw status | grep -q "Status: active"; then
    echo "UFW is not enabled. Enabling UFW..."
    sudo ufw enable
fi

# Configure firewall to allow Minecraft server port
echo "Configuring firewall..."
sudo ufw allow "$SERVER_PORT"
sudo ufw reload

# Start the server in a new screen session
echo "Starting Minecraft server..."
sudo screen -dmS minecraft_server java -Xms$XMS -Xmx$XMX -jar server.jar nogui

# Display screen sessions to confirm server is running
echo "Minecraft server setup complete. Active screen sessions:"
sudo screen -list
