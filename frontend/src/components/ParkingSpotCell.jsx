const VEHICLE_ICON = {
  MOTORCYCLE: '🏍',
  CAR: '🚗',
  TRUCK: '🚚',
};

export default function ParkingSpotCell({ spot, onClear, isBusy }) {
  const isFree = spot.status === 'FREE';
  const isRelocating = spot.status === 'RELOCATING';

  return (
    <div
      className={`spot ${isFree ? 'spot--free' : isRelocating ? 'spot--relocating' : 'spot--occupied'}`}
      data-size={spot.size}
    >
      <div className="spot__id">{spot.id}</div>

      {isFree ? (
        <div className="spot__free-label">Free</div>
      ) : isRelocating ? (
        <div className="spot__occupant">
          <div className="spot__icon" aria-hidden="true">
            {VEHICLE_ICON[spot.occupiedBy] || '🚙'}
          </div>
          <div className="spot__plate">{spot.licensePlate}</div>
          <div className="spot__relocating-label">Moving…</div>
        </div>
      ) : (
        <div className="spot__occupant">
          <div className="spot__icon" aria-hidden="true">
            {VEHICLE_ICON[spot.occupiedBy] || '🚙'}
          </div>
          <div className="spot__plate">{spot.licensePlate}</div>
          <button
            type="button"
            className="spot__clear-button"
            onClick={() => onClear(spot.id)}
            disabled={isBusy}
          >
            Clear
          </button>
        </div>
      )}
    </div>
  );
}
