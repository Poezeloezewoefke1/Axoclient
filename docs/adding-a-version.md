# Adding a new Minecraft version

The launcher does **not** hardcode a Minecraft version. Adding one is mostly a
data change plus a mod port.

Read this top to bottom the first time. After that, the checklist at the end is
enough.

---

## What actually has to change

| Part | Work needed |
| --- | --- |
| Launcher | **Nothing.** It reads versions from the manifest. |
| Website | Nothing. |
| Manifest | One new entry. |
| Client mod | **Port + rebuild.** This is the real work. |

The mod is the only blocker. It compiles against one Minecraft version's
mappings, so a 1.21.11 jar cannot run on 1.22.

---

## Step 1 — Port the mod

1. In `client/gradle.properties`, bump `minecraft_version`, `yarn_mappings`,
   `loader_version` and `fabric_version` to the new version's values from
   https://fabricmc.net/develop
2. Run `./gradlew build`.
3. Fix whatever fails to compile.

Where breakage usually is, worst first:

| Risk | Files | Why |
| --- | --- | --- |
| **High** | `client/src/main/java/dev/axoclient/mixin/` | Mixins inject into vanilla internals. A renamed method = the mixin fails = **the game will not start** (`axoclient.mixins.json` sets `required: true`). |
| **Low** | `client/src/main/java/dev/axoclient/mixin/optional/` | Same kind of code, but declared in `axoclient.optional.mixins.json` (`required: false`). If these stop matching, the feature stops working and the game still boots — fix them after launch, not before. |
| **Medium** | Anything using `GuiGraphics` | Mojang reworks the render stack often. |
| **Low** | Everything else | Most modules only touch long-stable accessors. |

See `docs/version-audit.md` for the full touchpoint table.

**Test the mixins first.** If the game boots, the hard part is done.

---

## Step 2 — Release the new client jar

Tag and push; the release workflow builds and uploads it. Note the download
URL, the SHA-1 and the file size — the manifest needs all three.

```bash
sha1sum axoclient-<version>.jar
```

---

## Step 3 — Get the mod versions

Modrinth's API tells you the exact file for a Minecraft version:

```bash
for slug in fabric-api sodium lithium; do
  curl -s "https://api.modrinth.com/v2/project/$slug/version?game_versions=%5B%22NEW_MC_VERSION%22%5D&loaders=%5B%22fabric%22%5D" \
    | jq -c '.[0] | {id, version_number, file: (.files[0] | {url, sha1: .hashes.sha1, size})}'
done
```

CI already prints this on every run — check the "Client build" job log if you'd
rather not run it yourself.

> Never bundle these jars in the installer. Always download them from Modrinth
> at install time. See `docs/risks.md` (licensing).

---

## Step 4 — Add the manifest entry

Copy the existing entry in `manifest/axo-manifest.json`, then replace every
id, URL, hash and size. Set `"default"` to the new id when it's ready to be
the one everyone gets.

Then validate — this is not optional:

```bash
node manifest/validate.mjs
```

It checks the schema **and** re-downloads every file to confirm the hashes
match. A wrong hash means every player gets a failed install.

---

## Step 5 — Ship it

Push the manifest. Launchers pick it up on next start; there is nothing to
release for it.

Keep the old version in the list. Players on it keep working, and the version
picker shows both.

---

## Checklist

- [ ] `gradle.properties` bumped
- [ ] `./gradlew build` passes
- [ ] Game **starts** (proves the mixins still apply)
- [ ] Client jar released; URL + SHA-1 + size noted
- [ ] Modrinth ids/hashes fetched for the new Minecraft version
- [ ] Manifest entry added
- [ ] `node manifest/validate.mjs` passes
- [ ] `"default"` moved once it's tested
- [ ] Old version left in place
