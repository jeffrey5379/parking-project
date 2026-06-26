const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/parking';

export const EVENTS_URL = `${API_BASE_URL}/events`;

/**
 * Thin wrapper around the parking lot REST API.
 * Each function returns parsed JSON or throws an Error with a readable
 * message (extracted from the backend's error payload when available).
 */

async function handleResponse(response) {
  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data?.message || `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return data;
}

export async function fetchSpots() {
  const response = await fetch(`${API_BASE_URL}/spots`);
  return handleResponse(response);
}

export async function fetchAvailability() {
  const response = await fetch(`${API_BASE_URL}/availability`);
  return handleResponse(response);
}

export async function parkVehicle(vehicleType, licensePlate) {
  const response = await fetch(`${API_BASE_URL}/park`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ vehicleType, licensePlate: licensePlate || null }),
  });
  return handleResponse(response);
}

export async function clearSpot(spotId) {
  const response = await fetch(`${API_BASE_URL}/spots/${encodeURIComponent(spotId)}/clear`, {
    method: 'POST',
  });
  return handleResponse(response);
}

export async function parkConcurrently(motorcycleCount, carCount, truckCount) {
  const response = await fetch(`${API_BASE_URL}/park-concurrent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ motorcycleCount, carCount, truckCount }),
  });
  return handleResponse(response);
}

export async function resetLot() {
  const response = await fetch(`${API_BASE_URL}/reset`, {
    method: 'POST',
  });
  return handleResponse(response);
}
