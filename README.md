# TPSnitch

TPSnitch is a Fabric Minecraft server utility mod for monitoring server performance. It automatically logs TPS (Ticks Per Second), MSPT (Milliseconds Per Tick), and player count at configurable intervals to a JSON file, making it easy to analyze server health over time.

## Features
- Logs TPS, MSPT, and player count to a JSON file keyed by timestamp
- Configurable log interval, debug mode, and log file name
- Player count tracked via join/leave events
- Utility functions for retrieving TPS and MSPT from the server object
- Supports Fabric

## Output Format
The log file is a JSON object where each key is an ISO timestamp, and each value is an object of `TPS`, `MSTP` and `Player Count`:
```json
{
  "2025-04-23T21:00:00Z": {
    "TPS": 20.0,
    "MSPT": 50.0,
    "PlayerCount": 5
  },
  ...
}
```

## Getting Started
1. Requires Java 21
2. Download the latest jar file
3. Place the jar in your server's `mods` folder
4. Start your server

## Configuration
- Log interval, debug mode, and log file name are configurable via your loader's config system
- Logs are saved to a JSON file (default: `tpsnitch_log.json`)

## License
Apache 2.0

---

For questions or support, put it in our [Issues](https://github.com/Dashtiss/TPSnitch/issues).
