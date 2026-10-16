import { Route, Routes } from 'react-router';
import { Construction } from 'lucide-react';
import { RequireAuth } from './auth/RequireAuth';
import { MainLayout } from './layout/MainLayout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { PatientsPage } from './pages/PatientsPage';
import { AppointmentsPage } from './pages/AppointmentsPage';
import { ReceptionPage } from './pages/ReceptionPage';
import { EmptyState } from './components/Common';

/** Rutas (entrega 2 de 3): panel, pacientes, agenda y recepción. Expediente y configuración llegan en la entrega 3. */
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
        <Route index element={<DashboardPage />} />
        <Route path="pacientes" element={<PatientsPage />} />
        <Route path="agenda" element={<AppointmentsPage />} />
        <Route path="recepcion" element={<ReceptionPage />} />
        <Route path="*" element={<EnConstruccion />} />
      </Route>
    </Routes>
  );
}
