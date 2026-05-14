import React from "react";
import { createRoot } from "react-dom/client";
import { API_BASE_PATH } from "@lims4wuxi/shared";

function PortalApp() {
  return (
    <main>
      <h1>LIMS4WUXI Portal</h1>
      <p>API base path: {API_BASE_PATH}</p>
    </main>
  );
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <PortalApp />
  </React.StrictMode>,
);
