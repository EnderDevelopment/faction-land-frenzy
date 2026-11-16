# FactionLandClaimFrenzy Minecraft Plugin

FactionLandClaimFrenzy is a Paper Minecraft plugin for running scheduled land claiming events and chunk capture competitions for factions communities. During an active event, players register the chunk they are standing in with `/raceclaim`, and their scoreboard team earns points on a shared leaderboard.

## Install

1. Build the plugin with Java 21 and Maven:

   ```bash
   mvn clean package
   ```

2. Copy the generated `target/plugin.jar` file into your Paper server's `plugins` folder.

3. Start or restart the server.

4. Edit `plugins/WeeklyLandRace/config.yml` if needed:

   ```yaml
   event-duration-hours: 48
   start-day: 1
   start-hour: 18
   time-zone: UTC
   ```

5. Restart the server after changing the configuration.

The plugin requires a Paper 1.21.11 server or a compatible Paper server and Java 21.

## Use

The event starts automatically each week at the configured day, hour, and time zone. The default schedule starts on Monday at 18:00 UTC and runs for 48 hours.

Players must belong to a Bukkit scoreboard team to participate.

During an active event:

1. Travel to the chunk your team wants to register.
2. Stand inside that chunk.
3. Run:

   ```text
   /raceclaim
   ```

4. The chunk is registered for the player's scoreboard team and adds one point to that team's score.

Each chunk can be registered only once during the current event. The plugin identifies chunks by world, chunk X coordinate, and chunk Z coordinate.

Available commands:

```text
/landrace
/landrace status
/landrace top
/landrace start
/landrace end
/raceclaim
```

`/landrace`, `/landrace status`, and `/landrace top` display event information and rankings. `/landrace start` and `/landrace end` require the `landrace.admin` permission, which is granted to server operators by default.

The plugin saves event status, the event end time, and scores in its configuration file. State is restored after a server restart while an event is active.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/80683ed4-f4a6-4ec8-ada0-32918be8850b?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=80683ed4-f4a6-4ec8-ada0-32918be8850b
