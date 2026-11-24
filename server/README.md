# MasterWave Listen Together Real-Time Jam Server

Production-ready, low-latency, real-time WebSocket and HTTP server for synchronized music rooms in MasterWave.

## Features

- **Unique 6-Digit Numeric Room Codes**: Fast, collision-free code generation (`100000` to `999999`).
- **Server-Authoritative Room State**: Atomic participant registration and strict capacity enforcement (e.g., 2, 5, 10, 20, 50, or custom).
- **Host Security Token**: Actions such as playback changes, track switching, and closing the room require the cryptographic `hostToken` issued exclusively to the host during room creation.
- **Playback Synchronization**: Server broadcasts authoritative track metadata, play/pause states, and playback position timestamps with latency compensation.
- **Host Disconnect Grace Period**: 60-second grace timer allowing the host to reconnect without immediately destroying the party.
- **Automatic Inactivity Cleanup**: Inactive rooms are automatically garbage collected after 2 hours.
- **Health & Monitoring**: `GET /health` endpoint for load balancers and uptime monitoring.

---

## HTTP Endpoints

### 1. Health Check
`GET /health`
```json
{
  "status": "ok",
  "service": "masterwave-jam-server",
  "version": "1.0.0",
  "activeRooms": 3,
  "totalParticipants": 9,
  "uptimeSeconds": 1420
}
```

### 2. Room Pre-Validation
`POST /api/rooms/:code/validate`
- Checks whether a 6-digit room code exists, is active, and has capacity before joining.
- Returns `404` for invalid/expired codes, `409` if the room has reached maximum capacity.

### 3. Room Info
`GET /api/rooms/:code`
- Returns public room metadata, participant count, and current track.

---

## WebSocket Events (`/ws`)

### Client -> Server:
- `create_room`: `{ type: "create_room", name, hostName, hostId, maxCapacity, initialTrack }`
- `join_room`: `{ type: "join_room", roomCode, userId, userName }`
- `reconnect_host`: `{ type: "reconnect_host", roomCode, hostId, hostToken }`
- `playback_update`: `{ type: "playback_update", roomCode, hostToken, track, isPlaying, positionMs }`
- `reaction`: `{ type: "reaction", roomCode, emoji, userName, userId }`
- `leave_room`: `{ type: "leave_room", roomCode, userId }`
- `close_room`: `{ type: "close_room", roomCode, hostToken }`

### Server -> Client:
- `room_created`: `{ type: "room_created", roomCode, hostToken, maxCapacity, inviteUrl }`
- `room_joined`: `{ type: "room_joined", roomCode, roomName, hostName, maxCapacity, participants, currentPlayback }`
- `participant_joined`: `{ type: "participant_joined", participant, participantsCount }`
- `participant_left`: `{ type: "participant_left", userId, userName, participantsCount }`
- `playback_sync`: `{ type: "playback_sync", playback: { track, isPlaying, positionMs, updatedAt }, serverTime }`
- `host_status_changed`: `{ type: "host_status_changed", hostConnected, reconnectGraceMs }`
- `room_closed`: `{ type: "room_closed", reason }`
- `error`: `{ type: "error", code, message }`

---

## Deployment Options

### 1. Deploy to Render (Recommended Free/Starter)
1. Push this directory or repo to GitHub.
2. In Render Dashboard, click **New > Web Service**.
3. Select this repository and the `server` directory.
4. Render automatically picks up `server/render.yaml`.
5. Your service URL will be: `https://masterwave-jam.onrender.com` (WebSocket: `wss://masterwave-jam.onrender.com/ws`).

### 2. Deploy to Fly.io
```bash
cd server
fly launch --config fly.toml
fly deploy
```

### 3. Deploy via Docker on Any VPS
```bash
docker build -t masterwave-jam-server .
docker run -d -p 8888:8888 --name mw-jam --restart unless-stopped masterwave-jam-server
```
