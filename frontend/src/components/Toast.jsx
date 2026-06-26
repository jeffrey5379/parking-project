import { useEffect } from "react";

export default function Toast({ message, kind, onDismiss }) {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onDismiss, 8000);
    return () => clearTimeout(timer);
  }, [message, onDismiss]);

  if (!message) {
    return <div className="toast toast--empty" aria-hidden="true" />;
  }

  return (
    <div className={`toast toast--${kind}`} role="status">
      <span className="toast__message">{message}</span>
      <button
        type="button"
        className="toast__dismiss"
        onClick={onDismiss}
        aria-label="Dismiss"
      >
        ×
      </button>
    </div>
  );
}
