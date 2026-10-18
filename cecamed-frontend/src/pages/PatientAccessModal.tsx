import { useEffect, useState, type FormEvent } from 'react';
import { portalApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { Patient } from '../api/types';
import { Modal } from '../components/Modal';
import { Field, Spinner } from '../components/Common';
import { useToast } from '../components/Toast';
export function PatientAccessModal({ patient, onClose }: { patient: Patient | null; onClose: () => void }) {
  const [username, setUsername] = useState(''); const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false); const [error, setError] = useState<string | null>(null);
  const toast = useToast();
  useEffect(() => { setUsername(''); setPassword(''); setError(null); }, [patient]);
  const save = async (e: FormEvent) => {
    e.preventDefault(); if (!patient) return; setSaving(true); setError(null);
    try { await portalApi.grantAccess(patient.id, username, password); toast.success('Acceso creado', 'El paciente puede iniciar sesión para solicitar citas y consultar su historial.'); onClose(); }
    catch (err) { setError(errorMessage(err)); } finally { setSaving(false); }
  };
  return <Modal open={patient !== null} onClose={onClose} locked={saving} title="Acceso del paciente" subtitle={patient?.fullName}>
    <form onSubmit={save}><fieldset className="form-section" disabled={saving}>
      <Field label="Usuario" htmlFor="portal-user" required><input id="portal-user" required minLength={3} maxLength={80} value={username} onChange={(e) => setUsername(e.target.value)} /></Field>
      <Field label="Contraseña inicial" htmlFor="portal-password" required><input id="portal-password" type="password" autoComplete="new-password" required minLength={12} maxLength={100} value={password} onChange={(e) => setPassword(e.target.value)} /></Field>
      <p className="muted">Entregue las credenciales al paciente por el procedimiento del consultorio.</p>
      <button className="btn btn-primary" type="submit">{saving && <Spinner size={16} />} Crear acceso</button>
    </fieldset>{error && <div className="alert alert-danger" role="alert">{error}</div>}</form>
  </Modal>;
}
