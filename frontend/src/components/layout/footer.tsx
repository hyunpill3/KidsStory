export function Footer() {
  return (
    <footer className="border-t mt-auto">
      <div className="container mx-auto px-4 py-6 text-sm text-muted-foreground text-center">
        © {new Date().getFullYear()} KidsStory. AI-powered story video service for children.
      </div>
    </footer>
  );
}
