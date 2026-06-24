# Parking Lot Frontend

React (Vite) dashboard for the `parking-lot-service` backend.

## Features

- Live grid of all 27 spots (9 Small / 9 Medium / 9 Large), grouped by size.
- Park a Motorcycle, Car, or Truck — backend auto-assigns the best spot.
- **Relocation state**: when a vehicle is being relocated, the destination spot pulses yellow with a "Moving…" label until the move completes (5–10 s).
- **Park Concurrently**: spinner inputs to choose how many vehicles of each type to park at once; each runs in its own virtual thread on the backend.
- "Clear" button on every occupied spot; "Reset lot" clears everything.
- Availability summary bar (free/occupied per size and overall).
- Auto-refreshes every 5 seconds + immediately after every action.

## Prerequisites

- Node.js 18+
- Backend running locally (see `../backend/README.md`) or deployed to AWS.

## Local setup

```bash
npm install
npm run dev
```

Runs on `http://localhost:5173`. API calls go to `http://localhost:8080/api/parking` (via `.env.development`).

## Build and deploy to AWS

```bash
npm run build
# verify no localhost in the bundle
grep -r "localhost" dist/assets/*.js

aws s3 sync dist/ s3://<frontend-bucket>/ --delete
aws cloudfront create-invalidation --distribution-id <cf-id> --paths "/*"
```

Both values (`<frontend-bucket>`, `<cf-id>`) are available as Terraform outputs.

## Project structure

```
src/
  api/
    parkingApi.js               Fetch wrapper for all backend endpoints
  components/
    AvailabilitySummaryBar.jsx  Free/occupied counters per size
    VehicleControls.jsx         Park buttons + Park Concurrently section
    SpinnerInput.jsx            +/− spinner control used in Park Concurrently
    ParkingGrid.jsx             Grid of spots grouped by size
    ParkingSpotCell.jsx         Single spot: FREE / RELOCATING (pulsing) / OCCUPIED
    Toast.jsx                   Success/error notification banner
  App.jsx                       Top-level state, polling, action handlers
  App.css                       All styling (parking-lot colour scheme)
  main.jsx                      React entry point
```
