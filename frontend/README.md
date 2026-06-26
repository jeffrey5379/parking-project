# Parking Lot Frontend

React (Vite) dashboard for the `parking-lot-service` backend.

## Features

- Live grid of all 27 spots (9 Small / 9 Medium / 9 Large), grouped by size.
- Park a Motorcycle, Car, or Truck — backend auto-assigns the best spot.
- **Relocation state**: when a vehicle is being relocated, both the source and destination spots pulse yellow with a "Moving…" label until the move completes (2–4 s).
- **Park Concurrently**: spinner inputs to choose how many vehicles of each type to park at once; each runs in its own virtual thread on the backend.
- "Clear" button on every occupied spot; "Reset lot" clears everything.
- Availability summary bar (free/occupied per size and overall).
- **Real-time updates via SSE**: the UI subscribes to `/api/parking/events` on load and receives instant pushes on every state change — no polling.
- **Toast notifications**: success/error messages appear at the top of the page, auto-dismiss after 5 s with a fade-out animation, and can be dismissed manually. Relocation failures (including permanent DLQ failures) are pushed from the backend through the SSE stream.

## Prerequisites

- Node.js 18+
- Backend running locally (see `../backend/README.md`) or deployed to AWS.

## Local setup

```bash
npm install
npm run dev
```

Runs on `http://localhost:5173`. API calls go to `http://localhost:8080/api/parking` (via `.env.development`).

## Project structure

```
src/
  api/
    parkingApi.js               Fetch wrapper for all backend endpoints + EVENTS_URL
  components/
    AvailabilitySummaryBar.jsx  Free/occupied counters per size
    VehicleControls.jsx         Park buttons + Park Concurrently section
    SpinnerInput.jsx            +/− spinner control used in Park Concurrently
    ParkingGrid.jsx             Grid of spots grouped by size
    ParkingSpotCell.jsx         Single spot: FREE / RELOCATING (pulsing) / OCCUPIED
    Toast.jsx                   Auto-dismissing notification banner (5 s fade-out)
  App.jsx                       Top-level state, SSE subscription, action handlers
  App.css                       All styling (parking-lot colour scheme)
  main.jsx                      React entry point
```
