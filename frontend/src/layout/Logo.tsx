/** Знак: лінія часу, що закінчується точкою. */
export function LogoMark({ size = 22 }: { size?: number }) {
  return (
    <svg
      className="logo-mark"
      width={size * 1.25}
      height={size}
      viewBox="0 0 30 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2.6}
      strokeLinecap="round"
      aria-hidden="true"
    >
      <path d="M2 12h14" />
      <circle cx="22" cy="12" r="5" />
    </svg>
  )
}

export default function Logo({ size = 22 }: { size?: number }) {
  return (
    <span className="logo" style={{ fontSize: size * 0.95 }}>
      <LogoMark size={size} />
      <span className="logo-text">Pastline</span>
    </span>
  )
}
