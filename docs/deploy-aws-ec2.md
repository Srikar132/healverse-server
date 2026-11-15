# Deploying to AWS EC2

The pipeline: push to `main` -> GitHub Actions runs the tests, builds the Docker image, pushes it to GitHub Container
Registry (GHCR), then SSHes into one EC2 instance and runs `deploy/deploy.sh`, which starts the new image and rolls
back automatically if it does not become healthy. The database stays on Neon.

You do the one-time setup below (about 30 minutes). After that every merge to `main` deploys itself.

## 1. Launch the instance (EC2 console -> Launch instance)

| Field | Choose | Why |
|---|---|---|
| Name | `healverse-server` | |
| AMI | **Amazon Linux 2023** (64-bit x86) | Docker installs from its own repo; `deploy/bootstrap-ec2.sh` is written for it. User name is `ec2-user`. If you use Ubuntu instead, the user is `ubuntu` and you install Docker yourself. |
| Instance type | **t3.small** (2 GiB) | A Spring Boot app plus Docker is tight in 1 GiB (t3.micro). t3.micro works only with swap and starts slowly. About 15 USD per month. Keep x86: the CI builds an x86 image. |
| Key pair | **Create new key pair** -> name `healverse-deploy`, type RSA, format `.pem` | See step 2. |
| Network | default VPC, **Auto-assign public IP: Enable** | |
| Security group | Create new. **SSH (22)** from Anywhere, **HTTP (80)** from Anywhere, **HTTPS (443)** from Anywhere | GitHub's runners have no fixed IP, so SSH has to be open to the internet; it is key-only (passwords are off). Do not open 8080: the app is reached on port 80. |
| Storage | **16 GiB gp3**, Encrypted: Yes | The 8 GiB default fills up with Docker images. |
| Advanced details | leave the defaults | |

Launch it, then allocate an address that survives stop/start: **EC2 -> Elastic IPs -> Allocate -> Associate** with the
instance. (A public IPv4 address costs about 3.6 USD per month on AWS.) Use this address as `EC2_HOST` below.

## 2. The `.pem` file (the SSH key)

The `.pem` is created **once**, by the console, when you press *Create key pair*. The browser downloads it
immediately and AWS cannot give it to you again. If you lose it, you create a new key pair and attach it to the instance.

1. Keep the downloaded `healverse-deploy.pem` somewhere safe (for example `C:\Users\<you>\.ssh\`). Never commit it
   (`*.pem` is in `.gitignore`) and never paste it into a chat.
2. Test it from your PC (PowerShell or Git Bash), replacing `<elastic-ip>`:
   ```
   ssh -i ~/.ssh/healverse-deploy.pem ec2-user@<elastic-ip>
   ```
   If Windows complains that the key is "too open", right-click the file -> Properties -> Security -> Advanced ->
   disable inheritance and keep only your own user.
3. GitHub gets a copy of the same private key as a secret (step 5). That key is the only thing the pipeline needs.

## 3. Prepare the server (once)

Log in with the command above, then:

```
curl -fsSL https://raw.githubusercontent.com/Srikar132/healverse-server/main/deploy/bootstrap-ec2.sh -o bootstrap.sh
bash bootstrap.sh
exit            # log out and back in so the docker group applies
docker compose version
```

(The script exists on `main` only after this branch is merged. Until then, copy it over with
`scp -i ~/.ssh/healverse-deploy.pem deploy/bootstrap-ec2.sh ec2-user@<elastic-ip>:` and run it.)

## 4. Create the server's secrets file

On the server, `nano /opt/healverse/.env`, with these five variables (values only on the server, never in git or chat):

```
JWT_SECRET=<base64, at least 32 bytes>
DB_URL=jdbc:postgresql://<neon pooler host>/<db>?sslmode=require
DB_USERNAME=<neon role>
DB_PASSWORD=<neon password>
GROQ_API_KEY=<groq key>
```

Then `chmod 600 /opt/healverse/.env`.

Notes:
- Use Neon's **pooler** host. The direct host is IPv6-only and a default EC2 instance has no IPv6.
- Do not put the user name or password inside `DB_URL`: Flyway prints the URL in the startup log.
- Generate a JWT secret with `openssl rand -base64 32`. Changing it signs every user out.

## 5. GitHub secrets (repo -> Settings -> Secrets and variables -> Actions -> New repository secret)

| Secret | Value |
|---|---|
| `EC2_HOST` | the Elastic IP |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | the **entire** contents of the `.pem` file, including the `-----BEGIN ... -----` and `-----END ... -----` lines |
| `EC2_KNOWN_HOSTS` | output of `ssh-keyscan -t ed25519 <elastic-ip>` run on your PC (pins the server's identity) |

Also create an **Environment** named `production` (Settings -> Environments). Adding yourself under *Required
reviewers* makes each deploy wait for your approval; leave it empty for fully automatic deploys.

## 6. Deploy and verify

Merge to `main` (or Actions -> *Deploy to EC2* -> *Run workflow*). When it is green:

```
curl http://<elastic-ip>/actuator/health/readiness     # {"status":"UP"}
```

On the server: `docker compose -f /opt/healverse/docker-compose.yml logs -f api`.

## Operations

- **Rollback:** automatic if the new version does not become healthy within 3 minutes. To roll back by hand, re-run
  the workflow on an older commit, or on the server: `cd /opt/healverse && IMAGE=ghcr.io/srikar132/healverse-server TAG=<old-sha> docker compose up -d`.
- **Logs** rotate at 3 x 10 MB per container.
- **Update the secrets:** edit `/opt/healverse/.env`, then `cd /opt/healverse && IMAGE=... TAG=$(cat .last-good-tag) docker compose up -d`.

## Not done yet (do these before real users)

- **HTTPS.** Port 80 is plain HTTP. Put a domain in front (Caddy on the instance, or an ALB with an ACM certificate).
  Then set `FORWARD_HEADERS_STRATEGY=framework` in `.env`, so login throttling sees real client addresses, and close port 80
  to the world if the load balancer is the only entry.
- **Close SSH to the world.** Replace the key-based deploy with GitHub OIDC + AWS Systems Manager (no open port 22, no
  long-lived key).
- **Backups and monitoring:** Neon handles database backups; add a CloudWatch alarm on instance status and on
  `/actuator/health`.
