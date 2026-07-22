/**
 * Inline SVG icons — no external files, so they render under the strict CSP
 * and inherit `currentColor` from whatever button holds them.
 */
type IconProps = { size?: number }

function svg(size: number, children: React.ReactNode): React.JSX.Element {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

export const IconPlay = ({ size = 22 }: IconProps): React.JSX.Element =>
  svg(size, <polygon points="6 4 20 12 6 20 6 4" fill="currentColor" stroke="none" />)

export const IconSkin = ({ size = 22 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <path d="M8 3l4 2 4-2 4 3-2 4-2-1v11H8V9L6 10 4 6l4-3z" />
  )

export const IconVersions = ({ size = 22 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <>
      <polygon points="12 3 21 8 12 13 3 8 12 3" />
      <polyline points="3 13 12 18 21 13" />
    </>
  )

export const IconSettings = ({ size = 22 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </>
  )

export const IconLogout = ({ size = 18 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </>
  )

export const IconCaret = ({ size = 18 }: IconProps): React.JSX.Element =>
  svg(size, <polyline points="6 9 12 15 18 9" />)

export const IconRefresh = ({ size = 16 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <>
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
    </>
  )

export const IconFolder = ({ size = 16 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
  )

export const IconWrench = ({ size = 16 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
  )

export const IconSpark = ({ size = 16 }: IconProps): React.JSX.Element =>
  svg(
    size,
    <path d="M12 2l1.9 5.8L20 9.7l-4.9 3.6L16.8 20 12 16.3 7.2 20l1.7-6.7L4 9.7l6.1-1.9L12 2z" />
  )
