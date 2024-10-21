#!/bin/bash

# Update package lists and install required packages
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-17-jre-headless screen ufw
sudo ufw allow 25565

# Download the Minecraft server JAR file
sudo wget -O server.jar https://launcher.mojang.com/v1/objects/c8f83c5655308435b3dcf03c06d9fe8740a77469/server.jar

# Create eula.txt to accept the EULA
echo "eula=true" | sudo tee eula.txt > /dev/null

# Create server.properties with the desired settings
sudo tee server.properties > /dev/null <<EOL
enable-jmx.monitoring=False
rcon.port=25575
level-seed=
gamemode=survival
enable-command-block=False
enable-query=False
generator-setting={}
level-name=False
motd=Minecraft Development Server
query.port=25565
pvp=True
generate-structures=True
difficulty=Normal
network-compression-threshold=256
require-resource-pack=False
max-tick-time=60000
EOL

# Start the server in a new screen session
sudo screen -dmS minecraft_server java -Xms1G -Xmx2G -jar server.jar nogui

# List screens
sudo screen -list
