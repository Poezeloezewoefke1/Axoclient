# Discord Rich Presence

Shows **"Playing Axo Client"** on your Discord profile while the launcher is open.

The code is written and shipped. It stays switched off until you register a
Discord application and give the build its id.

---

## What it shows

| When | Discord shows |
| --- | --- |
| Launcher open | *In the launcher* |
| Playing | *Playing Minecraft* + the version, with a timer |
| Joining a server | *Playing on <server>* |
| Launcher closed | nothing |

Players can turn it off in **Settings → Discord**.

---

## Turning it on (one-time setup)

1. Go to https://discord.com/developers/applications
2. Click **New Application**. Name it `Axo Client`.
3. Copy the **Application ID** (a long number).
4. Open **Rich Presence → Art Assets**. Upload the Axo logo and name it
   exactly `axo_logo`.
5. Set the id when you build:

```bash
# Windows (PowerShell)
$env:AXO_DISCORD_APP_ID = "your-application-id"
npm run build:win
```

That's it. No secret, no token. The Application ID is public by design — it
is meant to appear in the client.

---

## How it works

Discord listens on a local socket on your own PC:

- Windows: `\\?\pipe\discord-ipc-0`
- Linux/macOS: `$XDG_RUNTIME_DIR/discord-ipc-0`

The launcher connects to it and sends small JSON messages. Nothing goes over
the internet, and no Axo server is involved.

**Why not a library?** The usual Discord RPC packages include a native addon
that must be recompiled for every Electron version. That breaks often. Our
version (`launcher/src/main/discord.ts`) uses only `node:net`, so there is
nothing to compile and nothing to break on an Electron upgrade.

---

## If it doesn't show up

| Problem | Reason |
| --- | --- |
| Nothing on your profile | `AXO_DISCORD_APP_ID` was not set at build time |
| Nothing, and the id is set | Discord isn't running — that's fine, the launcher ignores it |
| Text shows, logo missing | The art asset isn't named `axo_logo` |
| Still nothing | Discord → Settings → Activity Privacy → turn on "Display current activity" |

Presence failures are always silent. It can never stop a launch.
