import { useCallback, useEffect, useState } from "react";
import {
  fetchSpots,
  fetchAvailability,
  parkVehicle,
  parkConcurrently,
  clearSpot,
  resetLot,
} from "./api/parkingApi";
import AvailabilitySummaryBar from "./components/AvailabilitySummaryBar";
import VehicleControls from "./components/VehicleControls";
import ParkingGrid from "./components/ParkingGrid";
import Toast from "./components/Toast";
import "./App.css";

const POLL_INTERVAL_MS = 5000;

export default function App() {
  const [spots, setSpots] = useState([]);
  const [summary, setSummary] = useState(null);
  const [isBusy, setIsBusy] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState({ message: "", kind: "error" });

  const loadState = useCallback(async () => {
    try {
      const [spotsData, availabilityData] = await Promise.all([
        fetchSpots(),
        fetchAvailability(),
      ]);
      setSpots(spotsData);
      setSummary(availabilityData);
    } catch (error) {
      setToast({
        message: `Could not reach the parking service: ${error.message}`,
        kind: "error",
      });
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // Wrapping in a named async function (rather than calling loadState
    // directly in the effect body) keeps the effect itself synchronous,
    // which is what the react-hooks/set-state-in-effect rule expects —
    // state updates happen inside the async callback, not the effect body.
    async function pollOnce() {
      await loadState();
    }

    pollOnce();
    const intervalId = setInterval(pollOnce, POLL_INTERVAL_MS);
    return () => clearInterval(intervalId);
  }, [loadState]);

  async function handlePark(vehicleType, licensePlate) {
    setIsBusy(true);
    try {
      const spot = await parkVehicle(vehicleType, licensePlate);
      setToast({ message: `Parked in spot ${spot.id}.`, kind: "success" });
      await loadState();
    } catch (error) {
      setToast({ message: error.message, kind: "error" });
    } finally {
      setIsBusy(false);
    }
  }

  async function handleClear(spotId) {
    setIsBusy(true);
    try {
      await clearSpot(spotId);
      setToast({ message: `Spot ${spotId} cleared.`, kind: "success" });
      await loadState();
    } catch (error) {
      setToast({ message: error.message, kind: "error" });
    } finally {
      setIsBusy(false);
    }
  }

  async function handleParkConcurrently(motorcycleCount, carCount, truckCount) {
    setIsBusy(true);
    try {
      const result = await parkConcurrently(motorcycleCount, carCount, truckCount);
      const msg = `Parked ${result.parked}${result.failed > 0 ? `, ${result.failed} failed` : ''}.`;
      setToast({ message: msg, kind: result.failed === 0 ? "success" : "error" });
      await loadState();
    } catch (error) {
      setToast({ message: error.message, kind: "error" });
    } finally {
      setIsBusy(false);
    }
  }

  async function handleReset() {
    setIsBusy(true);
    try {
      await resetLot();
      setToast({ message: "Lot reset. All spots are free.", kind: "success" });
      await loadState();
    } catch (error) {
      setToast({ message: error.message, kind: "error" });
    } finally {
      setIsBusy(false);
    }
  }

  function dismissToast() {
    setToast({ message: "", kind: "error" });
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__title-block">
          <span className="app-header__eyebrow">Lot Control</span>
          <h1 className="app-header__title">Parking Lot Dashboard</h1>
        </div>
        <AvailabilitySummaryBar summary={summary} />
        <button
          type="button"
          className="reset-button"
          onClick={handleReset}
          disabled={isBusy}
        >
          Reset lot
        </button>
      </header>

      <Toast
        message={toast.message}
        kind={toast.kind}
        onDismiss={dismissToast}
      />

      {isLoading ? (
        <p className="loading-text">Loading parking lot state…</p>
      ) : (
        <>
          <main className="app-main">
            <VehicleControls onPark={handlePark} onParkConcurrently={handleParkConcurrently} isBusy={isBusy} />
            <ParkingGrid spots={spots} onClear={handleClear} isBusy={isBusy} />
          </main>
        </>
      )}
    </div>
  );
}
