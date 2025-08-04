# Rewrite Design Goals

- Server acts as SFU
  
  - No more peer-to-peer connections

- "Audio sources" synced to clients rather than players.
  
  - Allows server to supply custom audio

- Relay generates session ID rather than server

- More internal error handling

- Send data through RTC to reduce load on relay

- Optimize for unreliable relay server
