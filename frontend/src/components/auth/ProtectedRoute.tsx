import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { loginPathWithNext } from "@/lib/redirect";

/** Sends anonymous visitors to login and brings them back to this path afterward. */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={loginPathWithNext(`${location.pathname}${location.search}`)} replace />;
  }

  return children;
}
