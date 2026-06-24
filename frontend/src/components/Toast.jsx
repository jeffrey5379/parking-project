/**
 * Small dismissible banner for surfacing the last error or success
 * message from an API call (e.g. "No available spot for TRUCK").
 */
export default function Toast({ message, kind, onDismiss }) {
  if (!message) return null;

  return (
    <div className={`toast toast--${kind}`} role="status">
      <span className="toast__message">{message}</span>
      <button type="button" className="toast__dismiss" onClick={onDismiss} aria-label="Dismiss">
        ×
      </button>
    </div>
  );
}
