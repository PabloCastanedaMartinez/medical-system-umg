import { Navigate, Route, Routes } from 'react-router';
import { RequireAuth, RequireRole } from './auth/RequireAuth';
import { MainLayout } from './layout/MainLayout';
import { LoginPage } from './pages/LoginPage';
import { useAuth } from './auth/AuthContext';
import { PatientsPage } from './pages/PatientsPage';
import { MedicalRecordIndex, MedicalRecordPage } from './pages/MedicalRecordPage';
import { AppointmentsPage } from './pages/AppointmentsPage';
import { ReceptionPage } from './pages/ReceptionPage';
import { SettingsPage } from './pages/SettingsPage';

/** Rutas: sustituyen a NavigationService + ViewType de JavaFX. */
export function App() {
  const { user } = useAuth();
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
        <Route index element={<Navigate to={user?.role === 'ADMIN' ? '/configuracion' : '/pacientes'} replace />} />
        <Route path="pacientes" element={<RequireRole roles={['MEDICO', 'RECEPCION']}><PatientsPage /></RequireRole>} />
        <Route
          path="expediente"
          element={
            <RequireRole roles={['MEDICO']}>
              <MedicalRecordIndex />
            </RequireRole>
          }
        />
        <Route
          path="expediente/:patientId"
          element={
            <RequireRole roles={['MEDICO']}>
              <MedicalRecordPage />
            </RequireRole>
          }
        />
        <Route path="agenda" element={<RequireRole roles={['MEDICO', 'RECEPCION']}><AppointmentsPage /></RequireRole>} />
        <Route path="recepcion" element={<RequireRole roles={['MEDICO', 'RECEPCION']}><ReceptionPage /></RequireRole>} />
        <Route path="configuracion" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
