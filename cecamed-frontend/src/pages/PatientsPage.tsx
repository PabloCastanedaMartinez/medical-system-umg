import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { ClipboardList, Pencil, RefreshCw, Search, UserCheck, UserPlus, Users, UserX, X } from 'lucide-react';
import { patientsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { Patient, PatientStatusFilter } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { useConfirm } from '../components/ConfirmDialog';
import { Badge } from '../components/StatusBadge';
import { EmptyState, ErrorBanner, PageHeader, Spinner } from '../components/Common';
import { useApi, useDebounced } from '../utils/useApi';
import { formatDate } from '../utils/format';
import { BLOOD_TYPE_SHORT, GENDER_SHORT } from '../utils/labels';
import { PatientAccessModal } from './PatientAccessModal';
import { PatientFormModal } from './PatientFormModal';

const FILTERS: { value: PatientStatusFilter; label: string }[] = [
  { value: 'ACTIVE', label: 'Solo activos' },
  { value: 'ALL', label: 'Todos los pacientes' },
  { value: 'INACTIVE', label: 'Inactivos' },
];

/** Equivalente de patient-list.fxml / PatientListController. */
export function PatientsPage() {
  const { isRecepcion, isMedico } = useAuth();
  const [accessPatient, setAccessPatient] = useState<Patient | null>(null);
  const toast = useToast();
  const { confirm } = useConfirm();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<PatientStatusFilter>('ACTIVE');
  const [page, setPage] = useState(0);
  useEffect(() => setPage(0), [search, status]);
  const debouncedSearch = useDebounced(search, 350);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Patient | null>(null);

  const { data, loading, error, reload } = useApi(
    (signal) => patientsApi.page(debouncedSearch.trim(), status, page, signal),
    [debouncedSearch, status, page],
  );
  const patients = data?.content ?? [];

  // Acceso rápido desde el dashboard: /pacientes?nuevo=1
  useEffect(() => {
    if (params.get('nuevo') === '1') {
      openCreate();
      params.delete('nuevo');
      setParams(params, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function openCreate() {
    setEditing(null);
    setFormOpen(true);
  }

  const openEdit = (p: Patient) => {
    setEditing(p);
    setFormOpen(true);
  };

  const openRecord = (p: Patient) => {
    if (isRecepcion) {
      toast.warning('Acceso restringido', 'El perfil de Recepción no tiene permisos para consultar el expediente clínico detallado');
      return;
    }
    navigate(`/expediente/${p.id}`);
  };

  const toggleStatus = async (p: Patient) => {
    const active = p.active === true;
    const ok = await confirm({
      title: 'Confirmar',
      message: (
        <>
          <p>
            <strong>¿Desea {active ? 'desactivar' : 'reactivar'} al paciente?</strong>
          </p>
          <p className="muted">
            Paciente: {p.fullName} (Exp.: {p.medicalRecordNumber ?? '-'})
          </p>
        </>
      ),
      confirmLabel: active ? 'Desactivar' : 'Reactivar',
      tone: active ? 'danger' : 'primary',
    });
    if (!ok) return;
    try {
      if (active) await patientsApi.deactivate(p.id, p.version);
      else await patientsApi.activate(p.id, p.version);
      toast.success('Estado actualizado', `El paciente ${p.fullName} fue ${active ? 'desactivado' : 'reactivado'} con éxito`);
      reload();
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No fue posible cambiar el estado del paciente'));
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Pacientes"
        actions={
          <button className="btn btn-primary" onClick={openCreate}>
            <UserPlus size={16} aria-hidden /> Registrar paciente
          </button>
        }
      />

      <div className="card">
        <div className="toolbar">
          <div className="search-box">
            <Search size={16} aria-hidden />
            <input
              type="search"
              placeholder="Buscar por nombre, apellido o teléfono..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Buscar pacientes"
            />
            {search && (
              <button className="icon-button" onClick={() => setSearch('')} aria-label="Limpiar búsqueda">
                <X size={14} />
              </button>
            )}
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value as PatientStatusFilter)} aria-label="Filtrar por estado">
            {FILTERS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </select>
          <div className="toolbar-spacer" />
          {loading && <Spinner size={16} />}
          <span className="muted">
            Mostrando {patients.length} paciente{patients.length === 1 ? '' : 's'}
          </span>
          <button className="btn btn-ghost btn-sm" onClick={reload} aria-label="Actualizar">
            <RefreshCw size={14} aria-hidden />
          </button>
        </div>

        {error && <ErrorBanner message={error} onRetry={reload} />}

        {patients.length === 0 && !loading ? (
          <EmptyState icon={Users} message="No se encontraron pacientes registrados o que coincidan con la búsqueda" />
        ) : (
          <div className="table-wrap">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Edad</th>
                  <th>Sexo</th>
                  <th>Grupo</th>
                  <th>Teléfono</th>
                  <th>Correo</th>
                  <th>Expediente</th>
                  <th>Estado</th>
                  <th className="col-actions">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {patients.map((p) => (
                  <tr key={p.id} onDoubleClick={() => openRecord(p)}>
                    <td className="strong">{p.fullName}</td>
                    <td className="nowrap">
                      {p.age != null ? `${p.age} años` : '--'}
                      {p.birthDate && <span className="muted"> ({formatDate(p.birthDate)})</span>}
                    </td>
                    <td>{p.gender ? GENDER_SHORT[p.gender] : '-'}</td>
                    <td>{p.bloodType ? <Badge tone="info">{BLOOD_TYPE_SHORT[p.bloodType]}</Badge> : '-'}</td>
                    <td className="nowrap">{p.phone || '-'}</td>
                    <td>{p.email || '-'}</td>
                    <td className="mono nowrap">{p.medicalRecordNumber ?? '-'}</td>
                    <td>
                      <Badge tone={p.active ? 'success' : 'neutral'}>{p.active ? 'Activo' : 'Inactivo'}</Badge>
                    </td>
                    <td className="col-actions">
                      <div className="row-actions">
                        <button className="btn btn-ghost btn-xs" onClick={() => navigate(`/pacientes/${p.id}/citas`)}>Citas</button>
                        {!isMedico && <button className="btn btn-ghost btn-xs" onClick={() => setAccessPatient(p)}>Crear acceso</button>}

                        {isMedico && (
                          <button className="icon-button" title="Ver expediente clínico" aria-label="Ver expediente clínico" onClick={() => openRecord(p)}>
                            <ClipboardList size={16} />
                          </button>
                        )}
                        <button className="icon-button" title="Editar datos del paciente" aria-label="Editar datos del paciente" onClick={() => openEdit(p)}>
                          <Pencil size={16} />
                        </button>
                        <button
                          className={`icon-button ${p.active ? 'text-danger' : 'text-success'}`}
                          title={p.active ? 'Desactivar paciente' : 'Reactivar paciente'}
                          aria-label={p.active ? 'Desactivar paciente' : 'Reactivar paciente'}
                          onClick={() => toggleStatus(p)}
                        >
                          {p.active ? <UserX size={16} /> : <UserCheck size={16} />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="toolbar" aria-label="Paginación de pacientes">
        <button className="btn btn-ghost" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Anterior</button>
        <span>Página {page + 1} · {data?.totalElements ?? 0} pacientes</span>
        <button className="btn btn-ghost" disabled={loading || page + 1 >= (data?.totalPages ?? 0)} onClick={() => setPage(page + 1)}>Siguiente</button>
      </div>
      <PatientAccessModal patient={accessPatient} onClose={() => setAccessPatient(null)} />
      <PatientFormModal
        open={formOpen}
        patient={editing}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          reload();
        }}
      />
    </div>
  );
}
