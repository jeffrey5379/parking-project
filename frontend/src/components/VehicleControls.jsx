import { useState } from "react";
import SpinnerInput from "./SpinnerInput";

const VEHICLE_OPTIONS = [
  { type: "MOTORCYCLE", label: "Park Motorcycle", hint: "Needs a Small spot" },
  { type: "CAR", label: "Park Car", hint: "Needs a Medium spot" },
  { type: "TRUCK", label: "Park Truck", hint: "Needs a Large spot" },
];

const CONCURRENT_ROWS = [
  { type: "MOTORCYCLE", label: "Motorcycles", max: 27 },
  { type: "CAR", label: "Cars", max: 18 },
  { type: "TRUCK", label: "Trucks", max: 9 },
];

export default function VehicleControls({
  onPark,
  onParkConcurrently,
  isBusy,
}) {
  const [counts, setCounts] = useState({ MOTORCYCLE: 1, CAR: 1, TRUCK: 1 });

  function handlePark(vehicleType) {
    onPark(vehicleType, "");
  }

  function setCount(type, value) {
    setCounts((prev) => ({ ...prev, [type]: value }));
  }

  const totalConcurrent = counts.MOTORCYCLE + counts.CAR + counts.TRUCK;

  return (
    <section className="panel controls-panel">
      <h2 className="panel-title">Park a vehicle</h2>

      <div className="vehicle-buttons">
        {VEHICLE_OPTIONS.map((vehicle) => (
          <button
            key={vehicle.type}
            type="button"
            className={`vehicle-button vehicle-button--${vehicle.type.toLowerCase()}`}
            onClick={() => handlePark(vehicle.type)}
            disabled={isBusy}
          >
            <span className="vehicle-button__label">{vehicle.label}</span>
            <span className="vehicle-button__hint">{vehicle.hint}</span>
          </button>
        ))}
      </div>

      <div className="concurrent-section">
        <h3 className="concurrent-title">Park Concurrently</h3>

        <div className="concurrent-rows">
          {CONCURRENT_ROWS.map(({ type, label, max }) => (
            <div key={type} className="concurrent-row">
              <span className="concurrent-label">{label}</span>
              <SpinnerInput
                value={counts[type]}
                onChange={(v) => setCount(type, v)}
                max={max}
              />
            </div>
          ))}
        </div>

        <div className="vehicle-buttons">
          <button
            type="button"
            className="vehicle-button"
            onClick={() =>
              onParkConcurrently(counts.MOTORCYCLE, counts.CAR, counts.TRUCK)
            }
            disabled={isBusy || totalConcurrent === 0}
          >
            <span className="vehicle-button__label">
              Park {totalConcurrent} vehicle{totalConcurrent !== 1 ? "s" : ""}
            </span>
          </button>
        </div>
      </div>
    </section>
  );
}
