# TPSnitch

TPSnitch is a cross-loader Minecraft server utility mod for monitoring server performance. It logs TPS (Ticks Per Second), MSPT (Milliseconds Per Tick), and player count at configurable intervals to a JSON file, making it easy to analyze server health over time.

## Features
- Logs TPS, MSPT, and player count to a JSON file keyed by timestamp
- Configurable log interval, debug mode, and log file name
- Player count tracked via join/leave events
- Resets player count on server restart
- Utility functions for retrieving TPS and MSPT from the server object
- Multi-loader: works with Fabric, NeoForge, and Forge (shared common code)

## Configuration
Edit `TPSnitchConfig.java` (or integrate with your config system) to set:
- `debug`: Enable extra logging
- `logFileName`: Name of the output JSON log file
- `logIntervalSeconds`: How often to log data (seconds)

## Output Format
The log file is a JSON object where each key is an ISO timestamp, and each value is an object:
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

## Building & Running
- Requires Java 21
- Build with Gradle for your target loader (Fabric, NeoForge, Forge)
- Place the built mod jar in your server's `mods` folder

## Development
- Shared logic is in the `common` module
- Loader-specific code is in `fabric`, `neoforge`, or `forge` modules
- Uses Fabric/Forge/NeoForge events to track players and schedule logging
- File writing is handled by `CommonClass.saveJson()`

## Advanced Usage
- You can call `TPSnitch.getTPS(server)` and `TPSnitch.getMSPT(server)` to retrieve live stats programmatically (server object is `MinecraftServer`)

## License
MIT or as specified by your project

---

For questions or support, join our [Discord](https://discord.myceliummod.network).
