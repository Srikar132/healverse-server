#!/usr/bin/env bash
# One-time server setup for an Amazon Linux 2023 EC2 instance. Run it as ec2-user:
#   curl -fsSL https://raw.githubusercontent.com/<owner>/healverse-server/main/deploy/bootstrap-ec2.sh | bash
# (or copy the file over with scp and run `bash bootstrap-ec2.sh`)
set -euo pipefail

COMPOSE_VERSION="v2.32.4"

echo "== Docker"
sudo dnf install -y docker
sudo mkdir -p /etc/docker
# Keep container logs from filling the small root disk
sudo tee /etc/docker/daemon.json >/dev/null <<'JSON'
{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }
JSON
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

echo "== Docker Compose plugin ${COMPOSE_VERSION}"
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -fsSL "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

echo "== Swap (a 2 GB instance is tight for a JVM plus Docker; 2 GB of swap is cheap insurance)"
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile >/dev/null
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo "== App directory"
sudo mkdir -p /opt/healverse
sudo chown "$USER":"$USER" /opt/healverse

echo
echo "Done. Log out and back in once (so the docker group applies), then check:  docker compose version"
echo "Next: create /opt/healverse/.env  (see docs/deploy-aws-ec2.md, step 4)"
