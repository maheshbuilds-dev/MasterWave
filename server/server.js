import http from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import crypto from 'crypto';

const PORT = parseInt(process.env.PORT || '8888', 10);
const HOST_GRACE_PERIOD_MS = 60 * 1000; // 60 seconds for host reconnection
const ROOM_INACTIVITY_TIMEOUT_MS = 2 * 60 * 60 * 1000; // 2 hours

/**
 * MasterWave In-Memory Room Store
 * Map<string, Room>
 */
const rooms = new Map();

/**
 * Generates a unique 6-digit numeric room code (100000 - 999999).
 * Atomically checks for collisions against currently active rooms.
 */
function generateUniqueRoomCode() {
  for (let attempt = 0; attempt < 100; attempt++) {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    if (!rooms.has(code)) {
      return code;
    }
  }
  // Fallback: 6-digit crypto random
  const code = (crypto.randomInt(100000, 999999)).toString();
  return code;
}

function broadcastToRoom(room, messageObj, excludeWs = null) {
  const payload = JSON.stringify(messageObj);
  for (const participant of room.participants.values()) {
    if (participant.ws && participant.ws !== excludeWs && participant.ws.readyState === WebSocket.OPEN) {
      try {
        participant.ws.send(payload);
      } catch (err) {
        console.error(`Error sending to participant ${participant.userId}:`, err.message);
      }
    }
  }
}

function getSanitizedParticipants(room) {
  return Array.from(room.participants.values()).map(p => ({
    userId: p.userId,
    name: p.name,
    isHost: p.isHost,
    joinedAt: p.joinedAt
  }));
}

function closeRoom(roomCode, reason = 'Host closed room') {
  const room = rooms.get(roomCode);
  if (!room) return;

  room.isClosed = true;
  if (room.hostGraceTimeout) {
    clearTimeout(room.hostGraceTimeout);
    room.hostGraceTimeout = null;
  }

  broadcastToRoom(room, {
    type: 'room_closed',
    roomCode,
    reason,
    timestamp: Date.now()
  });

  for (const p of room.participants.values()) {
    if (p.ws && p.ws.readyState === WebSocket.OPEN) {
      try {
        p.ws.close(1000, reason);
      } catch (_) {}
    }
  }

  rooms.delete(roomCode);
  console.log(`[JamServer] Room ${roomCode} closed. Reason: ${reason}. Remaining active rooms: ${rooms.size}`);
}

// Create HTTP server for REST API and health checks
const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = url.pathname;

  // Health check
  if (req.method === 'GET' && pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      service: 'masterwave-jam-server',
      version: '1.0.0',
      activeRooms: rooms.size,
      totalParticipants: Array.from(rooms.values()).reduce((sum, r) => sum + r.participants.size, 0),
      uptimeSeconds: Math.floor(process.uptime()),
      timestamp: Date.now()
    }));
    return;
  }

  // GET /api/rooms/:code
  const roomMatch = pathname.match(/^\/api\/rooms\/([0-9]{6})$/);
  if (req.method === 'GET' && roomMatch) {
    const code = roomMatch[1];
    const room = rooms.get(code);
    if (!room || room.isClosed) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'ROOM_NOT_FOUND', message: 'Room not found or expired' }));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      code: room.code,
      name: room.name,
      hostName: room.hostName,
      maxCapacity: room.maxCapacity,
      participantsCount: room.participants.size,
      isFull: room.participants.size >= room.maxCapacity,
      currentPlayback: room.currentPlayback
    }));
    return;
  }

  // POST /api/rooms (HTTP fallback creation)
  if (req.method === 'POST' && pathname === '/api/rooms') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const data = JSON.parse(body || '{}');
        const hostName = (data.hostName || 'Host').trim();
        const hostId = (data.hostId || crypto.randomUUID()).trim();
        const roomName = (data.name || `${hostName}'s Room`).trim();
        const maxCapacity = Math.min(Math.max(parseInt(data.maxCapacity, 10) || 5, 2), 100);

        const code = generateUniqueRoomCode();
        const hostToken = crypto.randomBytes(24).toString('hex');

        const newRoom = {
          code,
          name: roomName,
          hostId,
          hostName,
          hostToken,
          maxCapacity,
          createdAt: Date.now(),
          lastActivityAt: Date.now(),
          isClosed: false,
          hostConnected: false,
          hostDisconnectedAt: null,
          hostGraceTimeout: null,
          currentPlayback: {
            track: null,
            isPlaying: false,
            positionMs: 0,
            updatedAt: Date.now()
          },
          participants: new Map()
        };

        rooms.set(code, newRoom);
        console.log(`[JamServer] Room created via HTTP: ${code} (capacity: ${maxCapacity}, host: ${hostName})`);

        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          code,
          name: roomName,
          hostToken,
          maxCapacity,
          inviteUrl: `https://masterwave.vercel.app/join?code=${code}`
        }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'INVALID_JSON', message: err.message }));
      }
    });
    return;
  }

  // POST /api/rooms/:code/validate
  const validateMatch = pathname.match(/^\/api\/rooms\/([0-9]{6})\/validate$/);
  if (req.method === 'POST' && validateMatch) {
    const code = validateMatch[1];
    const room = rooms.get(code);
    if (!room || room.isClosed) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ valid: false, error: 'INVALID_CODE', message: 'Room does not exist or has expired.' }));
      return;
    }
    if (room.participants.size >= room.maxCapacity) {
      res.writeHead(409, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        valid: false,
        error: 'ROOM_FULL',
        message: `Room is full (${room.participants.size}/${room.maxCapacity} seats occupied).`,
        maxCapacity: room.maxCapacity,
        participantsCount: room.participants.size
      }));
      return;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      valid: true,
      code: room.code,
      name: room.name,
      hostName: room.hostName,
      maxCapacity: room.maxCapacity,
      participantsCount: room.participants.size
    }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'NOT_FOUND' }));
});

// WebSocket Server
const wss = new WebSocketServer({ server, path: '/ws' });

wss.on('connection', (ws, req) => {
  let attachedRoomCode = null;
  let attachedUserId = null;
  let isClientHost = false;

  ws.isAlive = true;
  ws.on('pong', () => { ws.isAlive = true; });

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString());
      const type = msg.type;

      switch (type) {
        case 'ping': {
          ws.send(JSON.stringify({ type: 'pong', serverTime: Date.now() }));
          break;
        }

        case 'create_room': {
          const hostName = (msg.hostName || 'Host').trim();
          const hostId = (msg.hostId || crypto.randomUUID()).trim();
          const roomName = (msg.name || `${hostName}'s Room`).trim();
          const maxCapacity = Math.min(Math.max(parseInt(msg.maxCapacity, 10) || 5, 2), 100);

          const code = generateUniqueRoomCode();
          const hostToken = crypto.randomBytes(24).toString('hex');

          const newRoom = {
            code,
            name: roomName,
            hostId,
            hostName,
            hostToken,
            maxCapacity,
            createdAt: Date.now(),
            lastActivityAt: Date.now(),
            isClosed: false,
            hostConnected: true,
            hostDisconnectedAt: null,
            hostGraceTimeout: null,
            currentPlayback: {
              track: msg.initialTrack || null,
              isPlaying: !!msg.isPlaying,
              positionMs: msg.positionMs || 0,
              updatedAt: Date.now()
            },
            participants: new Map()
          };

          // Host is first participant
          newRoom.participants.set(hostId, {
            userId: hostId,
            name: hostName,
            isHost: true,
            joinedAt: Date.now(),
            lastSeenAt: Date.now(),
            ws
          });

          rooms.set(code, newRoom);
          attachedRoomCode = code;
          attachedUserId = hostId;
          isClientHost = true;

          console.log(`[JamServer] Room created via WS: ${code} (capacity: ${maxCapacity}, host: ${hostName})`);

          ws.send(JSON.stringify({
            type: 'room_created',
            roomCode: code,
            name: roomName,
            hostToken,
            maxCapacity,
            participantsCount: 1,
            participants: getSanitizedParticipants(newRoom),
            currentPlayback: newRoom.currentPlayback,
            inviteUrl: `https://masterwave.vercel.app/join?code=${code}`,
            serverTime: Date.now()
          }));
          break;
        }

        case 'join_room': {
          const code = (msg.roomCode || '').trim();
          const userName = (msg.userName || 'Listener').trim();
          const userId = (msg.userId || crypto.randomUUID()).trim();

          const room = rooms.get(code);
          if (!room || room.isClosed) {
            ws.send(JSON.stringify({
              type: 'error',
              code: 'INVALID_ROOM',
              message: 'Invalid room code or room has expired.'
            }));
            return;
          }

          // Atomic capacity check
          // If already inside the room (reconnecting), don't count twice
          const isReconnecting = room.participants.has(userId);
          if (!isReconnecting && room.participants.size >= room.maxCapacity) {
            ws.send(JSON.stringify({
              type: 'error',
              code: 'ROOM_FULL',
              message: `Room has reached maximum capacity (${room.participants.size}/${room.maxCapacity} listeners).`,
              maxCapacity: room.maxCapacity,
              currentCount: room.participants.size
            }));
            return;
          }

          // Register participant
          room.participants.set(userId, {
            userId,
            name: userName,
            isHost: false,
            joinedAt: Date.now(),
            lastSeenAt: Date.now(),
            ws
          });
          room.lastActivityAt = Date.now();

          attachedRoomCode = code;
          attachedUserId = userId;
          isClientHost = false;

          console.log(`[JamServer] User ${userName} (${userId}) joined room ${code} (${room.participants.size}/${room.maxCapacity})`);

          // Reply with initial room state to joining participant
          ws.send(JSON.stringify({
            type: 'room_joined',
            roomCode: code,
            roomName: room.name,
            hostName: room.hostName,
            maxCapacity: room.maxCapacity,
            participantsCount: room.participants.size,
            participants: getSanitizedParticipants(room),
            currentPlayback: room.currentPlayback,
            serverTime: Date.now()
          }));

          // Notify everyone else
          broadcastToRoom(room, {
            type: 'participant_joined',
            roomCode: code,
            participant: { userId, name: userName, isHost: false, joinedAt: Date.now() },
            participantsCount: room.participants.size,
            maxCapacity: room.maxCapacity
          }, ws);
          break;
        }

        case 'reconnect_host': {
          const code = (msg.roomCode || '').trim();
          const hostToken = (msg.hostToken || '').trim();
          const hostId = (msg.hostId || '').trim();

          const room = rooms.get(code);
          if (!room || room.isClosed) {
            ws.send(JSON.stringify({ type: 'error', code: 'ROOM_NOT_FOUND', message: 'Room no longer exists.' }));
            return;
          }

          if (room.hostToken !== hostToken) {
            ws.send(JSON.stringify({ type: 'error', code: 'UNAUTHORIZED', message: 'Invalid host credentials.' }));
            return;
          }

          // Clear any pending disconnect grace timer
          if (room.hostGraceTimeout) {
            clearTimeout(room.hostGraceTimeout);
            room.hostGraceTimeout = null;
          }

          room.hostConnected = true;
          room.hostDisconnectedAt = null;
          room.lastActivityAt = Date.now();

          room.participants.set(hostId, {
            userId: hostId,
            name: room.hostName,
            isHost: true,
            joinedAt: Date.now(),
            lastSeenAt: Date.now(),
            ws
          });

          attachedRoomCode = code;
          attachedUserId = hostId;
          isClientHost = true;

          console.log(`[JamServer] Host reconnected to room ${code}`);

          ws.send(JSON.stringify({
            type: 'host_reconnected',
            roomCode: code,
            name: room.name,
            participantsCount: room.participants.size,
            participants: getSanitizedParticipants(room),
            currentPlayback: room.currentPlayback,
            serverTime: Date.now()
          }));

          broadcastToRoom(room, {
            type: 'host_status_changed',
            roomCode: code,
            hostConnected: true
          }, ws);
          break;
        }

        case 'playback_update': {
          const code = (msg.roomCode || attachedRoomCode || '').trim();
          const room = rooms.get(code);
          if (!room || room.isClosed) return;

          // Enforce security: Host-only verification
          if (msg.hostToken !== room.hostToken && !isClientHost) {
            ws.send(JSON.stringify({ type: 'error', code: 'FORBIDDEN', message: 'Only the host can broadcast playback updates.' }));
            return;
          }

          const now = Date.now();
          room.currentPlayback = {
            track: msg.track || room.currentPlayback.track,
            isPlaying: typeof msg.isPlaying === 'boolean' ? msg.isPlaying : room.currentPlayback.isPlaying,
            positionMs: typeof msg.positionMs === 'number' ? msg.positionMs : room.currentPlayback.positionMs,
            updatedAt: now
          };
          room.lastActivityAt = now;

          // Broadcast authoritative sync packet to all guests
          broadcastToRoom(room, {
            type: 'playback_sync',
            roomCode: code,
            playback: room.currentPlayback,
            serverTime: now
          }, ws);
          break;
        }

        case 'reaction': {
          const code = (msg.roomCode || attachedRoomCode || '').trim();
          const room = rooms.get(code);
          if (!room || room.isClosed) return;

          const emoji = (msg.emoji || '❤️').trim();
          const senderName = (msg.userName || 'Listener').trim();
          const senderId = (msg.userId || attachedUserId || 'unknown').trim();

          broadcastToRoom(room, {
            type: 'reaction',
            roomCode: code,
            emoji,
            senderName,
            senderId,
            timestamp: Date.now()
          });
          break;
        }

        case 'leave_room': {
          if (!attachedRoomCode || !attachedUserId) return;
          const room = rooms.get(attachedRoomCode);
          if (room) {
            const leavingUser = room.participants.get(attachedUserId);
            room.participants.delete(attachedUserId);
            console.log(`[JamServer] User ${attachedUserId} left room ${attachedRoomCode}. Remaining: ${room.participants.size}`);

            broadcastToRoom(room, {
              type: 'participant_left',
              roomCode: attachedRoomCode,
              userId: attachedUserId,
              userName: leavingUser ? leavingUser.name : 'Participant',
              participantsCount: room.participants.size
            });
          }
          attachedRoomCode = null;
          attachedUserId = null;
          break;
        }

        case 'close_room': {
          const code = (msg.roomCode || attachedRoomCode || '').trim();
          const room = rooms.get(code);
          if (!room) return;

          if (msg.hostToken !== room.hostToken && !isClientHost) {
            ws.send(JSON.stringify({ type: 'error', code: 'FORBIDDEN', message: 'Only the host can close the room.' }));
            return;
          }

          closeRoom(code, 'Host closed room');
          break;
        }

        default:
          console.warn(`[JamServer] Unknown message type: ${type}`);
      }
    } catch (err) {
      console.error('[JamServer] Parse error:', err.message);
    }
  });

  ws.on('close', () => {
    if (!attachedRoomCode || !attachedUserId) return;
    const room = rooms.get(attachedRoomCode);
    if (!room) return;

    if (isClientHost) {
      console.log(`[JamServer] Host disconnected from room ${attachedRoomCode}. Starting ${HOST_GRACE_PERIOD_MS / 1000}s grace timer...`);
      room.hostConnected = false;
      room.hostDisconnectedAt = Date.now();

      broadcastToRoom(room, {
        type: 'host_status_changed',
        roomCode: attachedRoomCode,
        hostConnected: false,
        reconnectGraceMs: HOST_GRACE_PERIOD_MS
      });

      // Disconnect grace timer: close room if host does not reconnect
      room.hostGraceTimeout = setTimeout(() => {
        if (!room.hostConnected) {
          console.log(`[JamServer] Host did not return to room ${attachedRoomCode} within grace period. Closing room.`);
          closeRoom(attachedRoomCode, 'Host disconnected');
        }
      }, HOST_GRACE_PERIOD_MS);
    } else {
      const leaving = room.participants.get(attachedUserId);
      room.participants.delete(attachedUserId);
      console.log(`[JamServer] Guest ${attachedUserId} socket closed. Remaining in ${attachedRoomCode}: ${room.participants.size}`);

      broadcastToRoom(room, {
        type: 'participant_left',
        roomCode: attachedRoomCode,
        userId: attachedUserId,
        userName: leaving ? leaving.name : 'Participant',
        participantsCount: room.participants.size
      });
    }
  });
});

// Periodic heartbeat & room garbage collection
setInterval(() => {
  // 1. WebSocket Ping/Pong keepalive
  for (const client of wss.clients) {
    if (!client.isAlive) {
      client.terminate();
      continue;
    }
    client.isAlive = false;
    client.ping();
  }

  // 2. Inactivity cleanup
  const now = Date.now();
  for (const [code, room] of rooms.entries()) {
    if (now - room.lastActivityAt > ROOM_INACTIVITY_TIMEOUT_MS) {
      console.log(`[JamServer] Room ${code} expired due to inactivity (>2 hours).`);
      closeRoom(code, 'Room expired due to inactivity');
    }
  }
}, 30 * 1000);

server.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`🎵 MasterWave Listen Together Jam Server running on port ${PORT}`);
  console.log(`   HTTP Health:    http://0.0.0.0:${PORT}/health`);
  console.log(`   WebSocket URL:  ws://0.0.0.0:${PORT}/ws`);
  console.log(`=======================================================`);
});
