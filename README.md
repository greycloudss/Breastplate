# Breastplate

Breastplate is a **peer‑to‑peer, self‑healing file‑synchronisation layer** for local folders. Each node continuously scans its directory, exchanges SHA‑256 hashes with its peers and decides—by majority vote—whether to **download, send or replace** files until every replica converges.

> Part of the **Armourer** series of projects.  
> **Work in Progress**
---

## Requirements

* Java 17 or newer installed
* `bash` shell (for build script on Unix‑like systems)
* `javac` and `jar` utilities on your PATH
* Network connectivity between peers on chosen ports
* (Optional) OpenSSH server running on hosts for SFTP fallback

## Installation

Clone the repository and build the JAR:

```bash
git clone <repository_url>
cd <repository_folder>
javac -d out $(find src -name "*.java")
jar --create --file breastplate.jar -C out .
```

## Usage

Generate an SSH key:
```bash
ssh-keygen -t ed25519 -C "breastplate-node" -f ~/.ssh/breastplate_id_ed25519 -N "passingWord"
```

Run the JAR with the required parameters:

```bash
java -jar breastplate.jar -port <port_number> -dir <directory_path> -add <peer_hosts:port> -user <ssh_username> -password <ssh_password>
```

### Example

```bash
java -jar breastplate.jar -port 7443 -dir /path/to/folder -add 192.168.10.2:7443 192.168.50.3:7443 -user Gandalf -password youShallNotGuess
```

## Features

* **Recursive Scanner** – Walks the root directory on each cycle and computes SHA‑256 hashes for every file.
* **Secure Hash Exchange** – Peers exchange hash lists over TLS sockets (`SSLServerSocket`/`SSLSocket`).
* **Democratic Conflict Resolution** – Majority‑vote algorithm to decide whether to download, send, or replace mismatched files.
* **Transfer Modes** – Small changes stream over the TLS connection; bulk transfers use SFTP for resume support and reliability.
* **Hot‑Swap Peers** – Dynamic peer management: `ConnectionManager` handles joining or dropping peers without interrupting sync.
* **Pluggable Authentication** – SSH user/password configuration for SFTP fallback; credentials managed by `SSHUtil`.
* **Multi‑Threaded Core** – Separate threads for scanning, voting, listening, and transferring ensure zero UI blocking.

## License

MIT License — fork, modify, and contribute freely.
