import { Navigate, Route, Routes } from 'react-router';
import { RequireAuth, RequireRole } from './auth/RequireAuth';
import { MainLayout } from './layout/MainLayout';
import { LoginPage } from './pages/LoginPage';
import { useAuth } from './auth/AuthContext';
import { PatientsPage } from './pages/PatientsPage';
import { MedicalRecordIndex, MedicalRecordPage } from './pages/MedicalRecordPage';
import { AppointmentsPage } from './pages/AppointmentsPage';
import { ReceptionPage } from './pages/ReceptionPage';
import { DoctorsPage } from './pages/DoctorsPage';
import { UsersPage } from './pages/UsersPage';
import { PatientAppointmentsPage } from './pages/PatientAppointmentsPage';
import { DashboardPage } from './pages/DashboardPage';
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
        <Route index element={user?.role === 'PACIENTE' ? <Navigate to='/mis-citas' replace /> : <DashboardPage />} />
        <Route path="pacientes" element={<RequireRole roles={['MEDICO', 'RECEPCION', 'ADMIN']}><PatientsPage /></RequireRole>} />
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
        <Route path="agenda" element={<RequireRole roles={['MEDICO', 'RECEPCION', 'ADMIN']}><AppointmentsPage /></RequireRole>} />
        <Route path="recepcion" element={<RequireRole roles={['RECEPCION', 'ADMIN']}><ReceptionPage /></RequireRole>} />
        <Route path="medicos" element={<RequireRole roles={['RECEPCION', 'ADMIN']}><DoctorsPage /></RequireRole>} />
        <Route path="usuarios" element={<RequireRole roles={['ADMIN']}><UsersPage /></RequireRole>} />
        <Route path="mis-citas" element={<RequireRole roles={['PACIENTE']}><PatientAppointmentsPage /></RequireRole>} />
        <Route path="pacientes/:patientId/citas" element={<RequireRole roles={['MEDICO', 'RECEPCION', 'ADMIN']}><PatientAppointmentsPage /></RequireRole>} />
        <Route path="configuracion" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
