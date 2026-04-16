const ICONS = {
  home: (
    <>
      <path d="M3.75 9.5 12 3l8.25 6.5" />
      <path d="M5.25 8.8V20h13.5V8.8" />
      <path d="M9.5 20v-5.5h5V20" />
    </>
  ),
  steering: (
    <>
      <path d="M4 18.5h16" />
      <path d="M6 15.5V10" />
      <path d="M12 15.5V6" />
      <path d="M18 15.5V8.5" />
    </>
  ),
  execution: (
    <>
      <path d="M5 5.5h9l-2.5 4.5L14 14.5H5z" />
      <path d="M14 5.5h5v5" />
      <path d="M14.2 9.8 19 5" />
    </>
  ),
  data: (
    <>
      <ellipse cx="12" cy="6" rx="6.5" ry="2.75" />
      <path d="M5.5 6v5c0 1.5 2.9 2.75 6.5 2.75s6.5-1.25 6.5-2.75V6" />
      <path d="M5.5 11v5c0 1.5 2.9 2.75 6.5 2.75s6.5-1.25 6.5-2.75v-5" />
    </>
  ),
  admin: (
    <>
      <circle cx="12" cy="8" r="3.25" />
      <path d="M5.5 19c1.4-2.75 4.05-4.25 6.5-4.25S17.1 16.25 18.5 19" />
      <path d="M19 7.5h2" />
      <path d="M20 6.5v2" />
    </>
  ),
};

export default function NavIcon({ name, className = "", title }) {
  const glyph = ICONS[name] ?? ICONS.home;

  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden={title ? undefined : true}
      role={title ? "img" : undefined}
      className={className}
    >
      {title ? <title>{title}</title> : null}
      {glyph}
    </svg>
  );
}
