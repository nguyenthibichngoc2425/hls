# HLS Monitor - Internal Swing GUI

## Overview
The HLS Monitor is an internal monitoring system that runs alongside the Spring Boot server. When you start the HLS server application, a Swing GUI window automatically opens on the server machine, providing real-time monitoring of:
- Login activities (success/fail)
- HLS streaming requests (master playlists, quality playlists, segments)
- Currently watching users

## Features

### Logs Tab
- **Real-time log display** of all login and streaming activities
- **Columns**: Time, Action, Account, IP, VideoId, Quality, Path, Message
- **Filter**: Text-based filtering by account, IP, videoId, quality, or action type
- **Auto-refresh**: Updates every 1 second (can be disabled)
- **Clear logs**: Button to clear all logs
- **Ring buffer**: Keeps maximum 2000 log entries in memory

### Online Tab
- **Live view** of currently watching users
- **Columns**: Account, IP, VideoId, Quality, Started At, Last Seen, User Agent
- **Auto-refresh**: Updates every 1 second automatically
- **Online count**: Shows total number of active viewers
- **Session tracking**: Tracks unique sessions by account + IP + videoId
- **Auto-cleanup**: Sessions timeout after 10 seconds of inactivity (configurable)

## Running the Application

Simply start the Spring Boot application as usual:

```bash
cd hls-server
mvn spring-boot:run
```

The "HLS Monitor" window will automatically open when the server starts.

## Configuration

In `application.yml`, you can configure the session timeout:

```yaml
monitor:
  online:
    timeoutSeconds: 10  # Default: 10 seconds
```

## Architecture

### Components

1. **Model Classes**
   - `WatchingSession`: Represents an active viewing session
   - `LogEntry`: Represents a log entry with action type

2. **Store Classes**
   - `OnlineWatchingStore`: Thread-safe ConcurrentHashMap for active sessions
   - `LogStore`: Thread-safe ring buffer for log entries (max 2000)

3. **Services**
   - `MonitorTrackerService`: Tracks login and HLS streaming activities
   - `OnlineCleanupJob`: Scheduled job that removes timed-out sessions every 5 seconds

4. **GUI Components**
   - `SwingMonitorFrame`: Main GUI window with tabs
   - `SwingMonitorLauncher`: ApplicationRunner that launches GUI on Spring Boot startup

### Integration Points

The monitoring system hooks into:
- **AuthService.loginUser()**: Tracks login success/fail with IP address
- **HlsStreamingController.getMasterPlaylist()**: Tracks master playlist requests
- **HlsStreamingController.getQualityPlaylist()**: Tracks quality playlist requests and updates online sessions
- **HlsStreamingController.getSegment()**: Tracks segment requests and updates online sessions

### Thread Safety

- All stores use thread-safe data structures (ConcurrentHashMap, ReentrantLock)
- Swing GUI updates are performed on the EDT (Event Dispatch Thread)
- No blocking operations in request handling paths

## Log Actions

- `LOGIN_SUCCESS`: User successfully logged in
- `LOGIN_FAIL`: Login attempt failed
- `HLS_MASTER`: Master playlist requested
- `HLS_PLAYLIST`: Quality playlist requested (360p/720p)
- `HLS_SEGMENT`: Video segment (.ts file) requested

## Notes

- The monitoring system is completely internal and runs in the same JVM as the Spring Boot server
- No web endpoints are created for the monitoring UI
- No Spring Security is required for the monitoring system
- The system is lightweight and does not impact streaming performance
- All data is stored in memory (no database persistence)
- The GUI window runs independently of user roles/permissions

## Memory Management

- **Log Store**: Ring buffer limited to 2000 entries (oldest removed when full)
- **Online Store**: Automatic cleanup every 5 seconds removes timed-out sessions
- **No memory leaks**: All data structures have size limits or automatic cleanup

## Future Enhancements

Possible improvements:
- Support for authenticated users (currently shows "anonymous")
- Export logs to file
- Statistics and charts
- Custom alert notifications
- Configurable refresh intervals
