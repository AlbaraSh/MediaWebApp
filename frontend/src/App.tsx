import { useEffect } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { AppShell } from "@/components/layout/AppShell";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { Toaster } from "@/components/ui/Toaster";
import { bindAuthRedirects } from "@/lib/api/client";
import { DiscoverPage } from "@/pages/DiscoverPage";
import { ForYouPage } from "@/pages/ForYouPage";
import { LandingPage } from "@/pages/LandingPage";
import { LibraryPage } from "@/pages/LibraryPage";
import { LoginPage } from "@/pages/LoginPage";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { SearchPage } from "@/pages/SearchPage";
import { TitlePage } from "@/pages/TitlePage";

export default function App() {
  const navigate = useNavigate();

  useEffect(() => {
    bindAuthRedirects({
      onSessionExpired: (loginPath) => {
        navigate(loginPath, { replace: true });
      },
      onUnauthorized: (loginPath) => {
        navigate(loginPath, { replace: true });
      },
    });
  }, [navigate]);

  return (
    <AppShell>
      <Toaster />
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/discover" element={<DiscoverPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/media/:id" element={<TitlePage />} />
        <Route
          path="/shelf"
          element={
            <ProtectedRoute>
              <LibraryPage />
            </ProtectedRoute>
          }
        />
        <Route path="/library" element={<Navigate to="/shelf" replace />} />
        <Route
          path="/for-you"
          element={
            <ProtectedRoute>
              <ForYouPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppShell>
  );
}
