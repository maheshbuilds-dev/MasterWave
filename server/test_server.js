import assert from 'assert';
import http from 'http';
import { WebSocket } from 'ws';

const BASE_URL = 'http://localhost:8889';
const WS_URL = 'ws://localhost:8889/ws';

// Set test port
process.env.PORT = '8889';

console.log('🚀 Starting Jam Server in test mode on port 8889...');
await import('./server.js');

// Helper sleep
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function runTests() {
  await sleep(1000);
  console.log('--- Test 1: Health Check Endpoint ---');
  const healthRes = await fetch(`${BASE_URL}/health`);
  const healthJson = await healthRes.json();
  assert.strictEqual(healthRes.status, 200);
  assert.strictEqual(healthJson.status, 'ok');
  console.log('✅ Health check passed:', healthJson);

  console.log('\n--- Test 2: Host Creates Room via WebSocket ---');
  const hostWs = new WebSocket(WS_URL);
  await new Promise(r => hostWs.on('open', r));

  let createdRoomData = null;
  hostWs.on('message', data => {
    const msg = JSON.parse(data.toString());
    if (msg.type === 'room_created') {
      createdRoomData = msg;
    }
  });

  hostWs.send(JSON.stringify({
    type: 'create_room',
    name: 'Chill Vibes Lounge',
    hostName: 'DJ Master',
    hostId: 'host_123',
    maxCapacity: 2 // Host + 1 guest only
  }));

  await sleep(600);
  assert.ok(createdRoomData, 'Room should be created');
  assert.match(createdRoomData.roomCode, /^[0-9]{6}$/, 'Room code must be exactly 6 digits');
  assert.ok(createdRoomData.hostToken, 'Host token must be present');
  assert.strictEqual(createdRoomData.maxCapacity, 2);
  assert.strictEqual(createdRoomData.participantsCount, 1);
  const roomCode = createdRoomData.roomCode;
  const hostToken = createdRoomData.hostToken;
  console.log(`✅ Room created successfully with 6-digit code: ${roomCode}, capacity: ${createdRoomData.maxCapacity}`);

  console.log('\n--- Test 3: Guest 1 Joins Room ---');
  const guest1Ws = new WebSocket(WS_URL);
  await new Promise(r => guest1Ws.on('open', r));

  let guest1Joined = null;
  guest1Ws.on('message', data => {
    const msg = JSON.parse(data.toString());
    if (msg.type === 'room_joined') {
      guest1Joined = msg;
    }
  });

  guest1Ws.send(JSON.stringify({
    type: 'join_room',
    roomCode,
    userId: 'guest_1',
    userName: 'Alice'
  }));

  await sleep(600);
  assert.ok(guest1Joined, 'Guest 1 should receive room_joined');
  assert.strictEqual(guest1Joined.participantsCount, 2);
  console.log('✅ Guest 1 joined. Current count: 2/2 (Room is now at capacity)');

  console.log('\n--- Test 4: Capacity Enforcement - Guest 2 Rejection ---');
  const guest2Ws = new WebSocket(WS_URL);
  await new Promise(r => guest2Ws.on('open', r));

  let guest2Error = null;
  guest2Ws.on('message', data => {
    const msg = JSON.parse(data.toString());
    if (msg.type === 'error') {
      guest2Error = msg;
    }
  });

  guest2Ws.send(JSON.stringify({
    type: 'join_room',
    roomCode,
    userId: 'guest_2',
    userName: 'Bob'
  }));

  await sleep(600);
  assert.ok(guest2Error, 'Guest 2 should receive error');
  assert.strictEqual(guest2Error.code, 'ROOM_FULL', 'Error must be ROOM_FULL');
  console.log('✅ Room capacity strictly enforced! Guest 2 rejected:', guest2Error.message);

  console.log('\n--- Test 5: Playback Synchronization & Host Token Protection ---');
  let guest1SyncPacket = null;
  guest1Ws.on('message', data => {
    const msg = JSON.parse(data.toString());
    if (msg.type === 'playback_sync') {
      guest1SyncPacket = msg;
    }
  });

  // Unauthorized guest tries to broadcast playback
  guest1Ws.send(JSON.stringify({
    type: 'playback_update',
    roomCode,
    hostToken: 'invalid_token',
    track: { id: 'track_hacked', title: 'Hacked Song' },
    isPlaying: true,
    positionMs: 12000
  }));

  await sleep(400);
  assert.strictEqual(guest1SyncPacket, null, 'Unauthorized guest update must NOT be broadcasted');

  // Authorized host broadcasts playback
  hostWs.send(JSON.stringify({
    type: 'playback_update',
    roomCode,
    hostToken,
    track: {
      id: 'song_999',
      title: 'Fear Song (Devara)',
      artist: 'Anirudh Ravichander',
      album: 'Devara',
      artworkUrl: 'https://example.com/art.jpg',
      durationSeconds: 215,
      streamUrl: 'https://stream.example.com/audio.mp4'
    },
    isPlaying: true,
    positionMs: 45000
  }));

  await sleep(600);
  assert.ok(guest1SyncPacket, 'Guest 1 must receive authoritative playback sync');
  assert.strictEqual(guest1SyncPacket.playback.track.title, 'Fear Song (Devara)');
  assert.strictEqual(guest1SyncPacket.playback.positionMs, 45000);
  assert.strictEqual(guest1SyncPacket.playback.isPlaying, true);
  console.log('✅ Authoritative playback synchronization verified. Guest received track update:', guest1SyncPacket.playback.track.title);

  console.log('\n--- Test 6: Host Closes Room ---');
  let guest1Closed = null;
  guest1Ws.on('message', data => {
    const msg = JSON.parse(data.toString());
    if (msg.type === 'room_closed') {
      guest1Closed = msg;
    }
  });

  hostWs.send(JSON.stringify({
    type: 'close_room',
    roomCode,
    hostToken
  }));

  await sleep(600);
  assert.ok(guest1Closed, 'Guest 1 must receive room_closed event');
  assert.strictEqual(guest1Closed.roomCode, roomCode);
  console.log('✅ Room closed cleanly by host. All participants notified.');

  // Clean up sockets
  hostWs.close();
  guest1Ws.close();
  guest2Ws.close();

  console.log('\n🎉 ALL 6 BACKEND INTEGRATION TESTS PASSED PERFECTLY!\n');
  process.exit(0);
}

runTests().catch(err => {
  console.error('❌ Test failed:', err);
  process.exit(1);
});
