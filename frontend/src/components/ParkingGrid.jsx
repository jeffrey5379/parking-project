import ParkingSpotCell from './ParkingSpotCell';

const SIZE_LABELS = {
  SMALL: 'Small — Motorcycles',
  MEDIUM: 'Medium — Cars',
  LARGE: 'Large — Trucks',
};

export default function ParkingGrid({ spots, onClear, isBusy }) {
  const bySize = {
    SMALL: spots.filter((spot) => spot.size === 'SMALL'),
    MEDIUM: spots.filter((spot) => spot.size === 'MEDIUM'),
    LARGE: spots.filter((spot) => spot.size === 'LARGE'),
  };

  return (
    <section className="panel grid-panel">
      <h2 className="panel-title">Lot layout</h2>

      <div className="lot-groups">
        {Object.entries(bySize).map(([size, spotsOfSize]) => (
          <div className="lane" key={size}>
            <div className="lane__label">{SIZE_LABELS[size]}</div>
            <div className="lane__spots">
              {spotsOfSize.map((spot) => (
                <ParkingSpotCell
                  key={spot.id}
                  spot={spot}
                  onClear={onClear}
                  isBusy={isBusy}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
