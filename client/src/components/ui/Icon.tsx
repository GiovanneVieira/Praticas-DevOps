// Icones SVG inline: unica fonte dos paths, tipados por nome (as const evita
// anotacao circular Record<IconName, ...> com keyof typeof PATHS).
const PATHS = {
  home: 'M3 10.5 12 3l9 7.5M5.5 9v10h13V9M9 19v-5h6v5',
  book: 'M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5v-16ZM4 17.5A2.5 2.5 0 0 1 6.5 15H20',
  ticket: 'm20.5 12-2-2a2 2 0 0 0-2.8-2.8l-2-2a2 2 0 0 0-2.8 0l-2 2A2 2 0 0 0 6.1 10l-2 2a2 2 0 0 0 0 2.8l2 2A2 2 0 0 0 8.9 19l2 2a2 2 0 0 0 2.8 0l2-2a2 2 0 0 0 2.8-2.8l2-2a2 2 0 0 0 .2-2.2Z',
  user: 'M20 21a8 8 0 0 0-16 0M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z',
  coin: 'M12 22c5 0 9-2.2 9-5V7c0 2.8-4 5-9 5S3 9.8 3 7v10c0 2.8 4 5 9 5ZM21 7c0 2.8-4 5-9 5S3 9.8 3 7s4-5 9-5 9 2.2 9 5Z',
  arrow: 'M5 12h14M13 6l6 6-6 6',
  check: 'm5 12 4 4L19 6',
  lock: 'M6 10V7a6 6 0 0 1 12 0v3M5 10h14v11H5z',
  logout: 'M10 17l5-5-5-5M15 12H3M21 19V5a2 2 0 0 0-2-2h-4',
  spark: 'm12 3 1.6 5.4L19 10l-5.4 1.6L12 17l-1.6-5.4L5 10l5.4-1.6L12 3Z',
  close: 'M6 6l12 12M18 6 6 18',
  alert: 'M12 4 2.8 20h18.4L12 4Zm0 5.5v4m0 3.1v.01',
  trophy: 'M8 21h8M12 17v4M7 4h10v4a5 5 0 0 1-10 0V4ZM7 6H4a3 3 0 0 0 3 4M17 6h3a3 3 0 0 1-3 4',
} as const

export type IconName = keyof typeof PATHS

export function Icon({ name }: { name: IconName }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true">
      <path d={PATHS[name]} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
