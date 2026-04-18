# HLS Monitor - Internal Swing GUI

## Overview
The HLS Monitor is an internal Swing GUI that runs with each Spring Boot instance (SERVER-A or SERVER-B).
It provides real-time monitoring for:

- Auth events (login success/fail, logout)
- HLS events (master, playlist, segment)
- Generic API requests and error events
- Currently watching users on the current server instance

## Features

### Logs Tab

- Real-time log display from shared PostgreSQL table `system_logs`
- Columns: `Time | Server | Event | User | IP | Endpoint`
- Category filter: `ALL | LOGIN | HLS | ERROR`
- Text filter: search by server, event, user, IP, endpoint, message
- Auto-refresh: every 2 seconds (toggle available)
- Clear logs: clears records in `system_logs`
- Color coding:
   - LOGIN/LOGOUT: green
   - HLS: blue
   - ERROR: red

### Online Tab

- Live view of current viewers tracked on this server instance
- Columns: `Account | IP | VideoId | Quality | Started At | Last Seen | Duration | User Agent`
- Auto-refresh: every 2 seconds
- Online count clearly shows current server scope
- Session tracking key: `account + ip + videoId`
- Auto-cleanup via scheduled job using timeout config

## Running the Application

Simply start the Spring Boot application as usual:

```bash
cd hls-server
mvn spring-boot:run
```

The "HLS Monitor" window will automatically open when the server starts.

In multi-server demo, you can run:

- SERVER-A (`8081`) and SERVER-B (`8082`)
- Both monitors will read the same log source from shared DB

## Configuration

In `application.yml`, you can configure the session timeout:

```yaml
monitor:
  online:
      timeoutSeconds: 15  # demo default
```

## Architecture

### Components

1. **Model Classes**
   - `WatchingSession`: active watching session
   - `SystemLog`: persistent structured log entity

2. **Store Classes**
   - `OnlineWatchingStore`: in-memory store for active sessions

3. **Services**
   - `MonitorTrackerService`: writes structured monitoring events to DB and updates online store
   - `SystemLogService`: saves/reads/clears structured logs in DB
   - `OnlineCleanupJob`: removes timed-out sessions

4. **GUI Components**
   - `SwingMonitorFrame`: main window with status header + Logs/Online tabs
   - `SwingMonitorLauncher`: launches Swing monitor at startup

### Integration Points

The monitoring system hooks into:
- `RequestLoggingFilter`: API_REQUEST structured logs
- `RandomFailureSimulationFilter`: ERROR logs for simulated failures
- `MonitorTrackerService`: LOGIN/HLS structured logs
- `AuthService`: auth flow integration
- `HlsStreamingController`: HLS flow integration

### Thread Safety

- All stores use thread-safe data structures (ConcurrentHashMap, ReentrantLock)
- Swing GUI updates are performed on the EDT (Event Dispatch Thread)
- No blocking operations in request handling paths

## Event Types

- `LOGIN_SUCCESS`
- `LOGIN_FAIL`
- `LOGOUT`
- `HLS_MASTER`
- `HLS_PLAYLIST`
- `HLS_SEGMENT`
- `API_REQUEST`
- `ERROR`

## Notes

- The monitoring system is completely internal and runs in the same JVM as the Spring Boot server
- No web endpoints are created for the monitoring UI
- No Spring Security is required for the monitoring system
- Structured logs are persisted in shared PostgreSQL (not ring-buffer memory)
- Viewer sessions are still in-memory by design for lightweight demo
- The GUI window runs independently of user roles/permissions

## Data Management

- **System logs**: persisted in `system_logs` table
- **Online store**: auto cleanup for inactive sessions
- **No heavy dependencies**: no Kafka/Redis required in this demo

## Future Enhancements

Possible improvements:
- Add date-range filter and paging for very large log volume
- Add dedicated dashboard endpoint for monitor snapshots
- Add optional aggregated viewer count across all servers
