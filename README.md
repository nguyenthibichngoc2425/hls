# ⚡ QUICK START - 30 Seconds

## 🚀 Run Everything Automatically

```bash
cd d:\IdeaProjects\HTTP-Live-Streaming
startup.bat
```

**Done!** All components start automatically:
- ✅ 3 Backend servers (8081, 8082, 8083)  
- ✅ Load Balancer (8080)
- ✅ Health checks every 3s
- ✅ Round-robin load distribution

---

## 🧪 Test It

### Simple test:
```bash
curl http://localhost:8080/api/data
```

### Check status:
```bash
curl http://localhost:8080/lb/status
```

### Kill a backend (test failover):
```bash
# Kill port 8081 in Task Manager or PowerShell:
Get-Process java | Where-Object {$_.CommandLine -like "*8081*"} | Stop-Process -Force

# Send request - Should auto-failover to another backend
curl http://localhost:8080/api/data
```

---

## 📖 Full Testing Guide

**See:** [RUN_COMPLETE_TEST.md](RUN_COMPLETE_TEST.md)

Includes all tests:
- Round-robin distribution (Test 2)
- Failover scenarios (Test 3)  
- Degraded mode (Test 4)
- Health endpoints (Test 5)
- Concurrent load (Test 6)
- Error injection (Test 7)

---

## 🛠️ Manual Start (If Needed)

### Terminal 1: Start backends
```bash
start-backends.bat
```

### Terminal 2: Start load balancer (after 10s)
```bash
start-loadbalancer.bat
```

---

## 📂 Files

```
✅ RUN_COMPLETE_TEST.md  ← MAIN: Full test guide with all scenarios
✅ startup.bat           ← Auto-start everything
✅ start-backends.bat    ← Start 3 backends only
✅ start-loadbalancer.bat ← Start LB only
```

**Deleted (Redundant):**
- ❌ PROJECT_CONTEXT.md (old design docs)
- ❌ MULTI_SERVER_GUIDE.md (combined into main guide)
- ❌ QUICK_START.md (superseded)
- ❌ DEMO_OUTPUT.md (replaced with executable tests)

---

## ✅ Status

All features tested and working:
- ✅ Load balancing (Round Robin)
- ✅ Failover (Automatic retry on dead server)
- ✅ Health monitoring (Every 3s)
- ✅ Auto recovery (Server restart detected)
- ✅ Error injection (5% simulated)
- ✅ Concurrent requests (30+)
- ✅ Graceful degradation (1-3 backends)

**Next Step:** [RUN_COMPLETE_TEST.md](RUN_COMPLETE_TEST.md) → Start with TEST 1 & 2

---

**Version:** 1.0 - Production Ready  
**Last Updated:** April 16, 2026
