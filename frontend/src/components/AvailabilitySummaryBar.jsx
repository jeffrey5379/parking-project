const SIZE_ORDER = ['SMALL', 'MEDIUM', 'LARGE'];
const SIZE_SHORT_LABEL = { SMALL: 'Small', MEDIUM: 'Medium', LARGE: 'Large' };

/**
 * Top summary strip: total free/occupied counts plus a breakdown per
 * spot size. Reads directly from the /availability endpoint response.
 */
export default function AvailabilitySummaryBar({ summary }) {
  if (!summary) return null;

  return (
    <section className="summary-bar">
      <div className="summary-bar__total">
        <span className="summary-bar__total-number">{summary.totalFree}</span>
        <span className="summary-bar__total-label">
          of {summary.totalSpots} spots free
        </span>
      </div>

      <div className="summary-bar__breakdown">
        {SIZE_ORDER.map((size) => {
          const data = summary.bySize[size];
          if (!data) return null;
          return (
            <div className="summary-chip" key={size} data-size={size}>
              <span className="summary-chip__label">{SIZE_SHORT_LABEL[size]}</span>
              <span className="summary-chip__count">
                {data.free}/{data.total}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
