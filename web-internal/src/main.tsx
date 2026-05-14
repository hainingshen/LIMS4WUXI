import React from "react";
import { createRoot } from "react-dom/client";
import { API_BASE_PATH } from "@lims4wuxi/shared";

function InternalApp() {
  return (
    <main>
      <h1>LIMS4WUXI Internal</h1>
      <p>API base path: {API_BASE_PATH}</p>
    </main>
  );
}

createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <InternalApp />
  </React.StrictMode>,
);
