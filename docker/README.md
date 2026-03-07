# Docker Configuration Files

## 📁 Files trong thư mục này

### init-db.sql
SQL script để khởi tạo database PostgreSQL.
- Chạy tự động khi PostgreSQL container start lần đầu
- Tạo tables, indexes, và seed data

### ngrok.yml
Cấu hình cho ngrok tunnel service.

#### Configuration options:

```yaml
version: "2"

# Web interface port (default: 4040)
web_addr: 0.0.0.0:4040

# Tunnels configuration
tunnels:
  backend:
    proto: http
    addr: host.docker.internal:8080  # Backend address
    inspect: true                     # Enable request inspection

# Region (choose closest for better performance)
# Options: us, eu, ap, au, sa, jp, in
region: ap

# Logging
log_level: info      # info, debug
log_format: term     # json, logfmt, term
```

#### Customize tunnel:

**Backend chạy trên port khác:**
```yaml
tunnels:
  backend:
    addr: host.docker.internal:3000  # Change port
```

**Backend chạy trong Docker:**
```yaml
tunnels:
  backend:
    addr: backend:8080  # Reference Docker service name
```

**Multiple tunnels:**
```yaml
tunnels:
  backend:
    proto: http
    addr: host.docker.internal:8080
  
  frontend:
    proto: http
    addr: host.docker.internal:3000
```

**Custom subdomain (Requires Ngrok paid plan):**
```yaml
tunnels:
  backend:
    proto: http
    addr: host.docker.internal:8080
    subdomain: my-restaurant-api
    # URL will be: https://my-restaurant-api.ngrok-free.app
```

**Add authentication:**
```yaml
tunnels:
  backend:
    proto: http
    addr: host.docker.internal:8080
    auth: "username:password"
```

**Custom hostname (Requires Ngrok paid plan):**
```yaml
tunnels:
  backend:
    proto: http
    addr: host.docker.internal:8080
    hostname: my-api.ngrok.app
```

## 🔧 Troubleshooting

### Cannot connect to backend

**Symptom:**
```
ERR_NGROK_108: Failed to connect to localhost:8080
```

**Solutions:**

1. **Verify backend is running:**
   ```bash
   # Windows
   netstat -an | findstr 8080
   
   # Linux/macOS
   lsof -i :8080
   ```

2. **Check backend port in ngrok.yml:**
   ```yaml
   addr: host.docker.internal:8080  # Must match backend port
   ```

3. **If backend is in Docker, use service name:**
   ```yaml
   addr: backend:8080  # Replace with your service name
   ```

### Ngrok tunnel not starting

**Check logs:**
```bash
docker-compose logs ngrok
```

**Common issues:**

1. **Invalid authtoken:**
   ```bash
   # Update NGROK_AUTHTOKEN in .env
   # Restart: docker-compose restart ngrok
   ```

2. **Port 4040 already in use:**
   ```bash
   # Stop other ngrok instances
   # Windows: taskkill /F /IM ngrok.exe
   # Linux/macOS: killall ngrok
   ```

### Cannot access ngrok web UI

**URL:** http://localhost:4040

**Check:**
```bash
# Verify ngrok container is running
docker-compose ps ngrok

# Check port mapping
docker-compose port ngrok 4040
```

## 📚 References

- [Ngrok Docker Documentation](https://ngrok.com/docs/using-ngrok-with/docker/)
- [Ngrok Configuration Reference](https://ngrok.com/docs/ngrok-agent/config/)
- [Project MOMO_SETUP.md](../MOMO_SETUP.md)

## 🆕 Adding new Docker services

### Template for new service:

```yaml
  service_name:
    image: image:tag
    container_name: restaurant_service_name
    environment:
      - VAR_NAME=${VAR_NAME}
    ports:
      - "host_port:container_port"
    volumes:
      - volume_name:/path/in/container
    networks:
      - restaurant_network
    restart: unless-stopped
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "command", "to", "test", "health"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Best practices:

1. ✅ Use environment variables from `.env`
2. ✅ Add to `restaurant_network`
3. ✅ Set `restart: unless-stopped` for reliability
4. ✅ Add healthcheck when possible
5. ✅ Document in this README
