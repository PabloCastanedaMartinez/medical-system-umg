import { Route, Routes } from 'react-router';
import { Construction } from 'lucide-react';
import { RequireAuth } from './auth/RequireAuth';
import { MainLayout } from './layout/MainLayout';
import { LoginPage } from './pages/LoginPage';
import { EmptyState } from './components/Common';

/** Rutas (entrega 1 de 3): acceso y estructura principal. Las pantallas se agregan en las siguientes entregas. */
function EnConstruccion() {
  return <EmptyState icon={Construction} message="Este módulo se incorporará en una próxima entrega." />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route index element={<EnConstruccion />} />
        <Route path="*" element={<EnConstruccion />} />
      </Route>
    </Routes>
  );
}
