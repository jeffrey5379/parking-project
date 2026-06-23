export default function SpinnerInput({ value, onChange, min = 0, max = 27 }) {
  return (
    <div className="spinner-input">
      <button
        type="button"
        className="spinner-input__btn"
        onClick={() => onChange(Math.max(min, value - 1))}
        disabled={value <= min}
        aria-label="Decrease"
      >
        −
      </button>
      <span className="spinner-input__value">{value}</span>
      <button
        type="button"
        className="spinner-input__btn"
        onClick={() => onChange(Math.min(max, value + 1))}
        disabled={value >= max}
        aria-label="Increase"
      >
        +
      </button>
    </div>
  );
}
