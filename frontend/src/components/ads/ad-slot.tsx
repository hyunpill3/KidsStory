interface AdSlotProps {
  label?: string;
  className?: string;
}

/**
 * Placeholder for a real ad network (e.g. AdSense/AdMob) slot. Swap the
 * inner content for the network's script/unit once one is wired up.
 */
export function AdSlot({ label = "Advertisement", className = "" }: AdSlotProps) {
  return (
    <div
      className={`flex items-center justify-center rounded-md border border-dashed bg-muted/40 text-xs text-muted-foreground h-24 ${className}`}
      role="complementary"
      aria-label={label}
    >
      {label}
    </div>
  );
}
