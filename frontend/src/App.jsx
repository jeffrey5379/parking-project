import { useEffect, useState } from "react";
import {
  EVENTS_URL,
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

export default function App() {
  const [spots, setSpots] = useState([]);
  const [summary, setSummary] = useState(null);
  const [isBusy, setIsBusy] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState({ id: 0, message: "", kind: "error" });

  function showToast(message, kind) {
    setToast({ id: Date.now(), message, kind });
  }

  useEffect(() => {
    const es = new EventSource(EVENTS_URL);

    es.addEventListener("parking-update", (e) => {
      const { spots: updatedSpots, summary: updatedSummary, notification } = JSON.parse(e.data);
      setSpots(updatedSpots);
      setSummary(updatedSummary);
      if (notification) {
        showToast(notification.message, notification.kind);
      }
      setIsLoading(false);
    });

    es.onerror = () => {
      // EventSource reconnects automatically; nothing to do here.
    };

    return () => es.close();
  }, []);

  async function handlePark(vehicleType, licensePlate) {
    setIsBusy(true);
    try {
      const spot = await parkVehicle(vehicleType, licensePlate);
      showToast(`Parked in spot ${spot.id}.`, "success");
    } catch (error) {
      showToast(error.message, "error");
    } finally {
      setIsBusy(false);
    }
  }

  async function handleClear(spotId) {
    setIsBusy(true);
    try {
      await clearSpot(spotId);
      showToast(`Spot ${spotId} cleared.`, "success");
    } catch (error) {
      showToast(error.message, "error");
    } finally {
      setIsBusy(false);
    }
  }

  async function handleParkConcurrently(motorcycleCount, carCount, truckCount) {
    setIsBusy(true);
    try {
      const result = await parkConcurrently(motorcycleCount, carCount, truckCount);
      const msg = `Parked ${result.parked}${result.failed > 0 ? `, ${result.failed} failed` : ''}.`;
      showToast(msg, result.failed === 0 ? "success" : "error");
    } catch (error) {
      showToast(error.message, "error");
    } finally {
      setIsBusy(false);
    }
  }

  async function handleReset() {
    setIsBusy(true);
    try {
      await resetLot();
      showToast("Lot reset. All spots are free.", "success");
    } catch (error) {
      showToast(error.message, "error");
    } finally {
      setIsBusy(false);
    }
  }

  function dismissToast() {
    setToast({ id: 0, message: "", kind: "error" });
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
        key={toast.id}
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
