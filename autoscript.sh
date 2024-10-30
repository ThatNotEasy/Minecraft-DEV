#!/bin/bash

# Variables for easier customization
XMS="1G"
XMX="2G"
SERVER_PORT="25565"
RCON_PORT="25575"
WORLD_NAME="minecraft_world"
MOTD="Minecraft Development Server"

# Update package lists and install required packages
sudo apt update && sudo apt upgrade -y
sudo apt install openjdk-17-jre-headless screen ufw -y 
sudo ufw allow "$SERVER_PORT"

# Download the Minecraft server JAR file
sudo wget -O server.jar https://launcher.mojang.com/v1/objects/c8f83c5655308435b3dcf03c06d9fe8740a77469/server.jar

# Accept the Minecraft EULA
echo "eula=true" | sudo tee eula.txt > /dev/null

# Create server.properties with the desired settings
sudo tee server.properties > /dev/null <<EOL
enable-jmx.monitoring=false
rcon.port=$RCON_PORT
level-seed=
gamemode=survival
enable-command-block=false
enable-query=false
generator-setting={}
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

# Start the server in a new screen session with logging enabled
sudo screen -dmSL minecraft_server java -Xms$XMS -Xmx$XMX -jar server.jar nogui

# List active screens
sudo screen -list
