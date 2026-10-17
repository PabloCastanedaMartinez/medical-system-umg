import { useRef, useState, type FormEvent } from 'react';
import { Download, Eye, FileText, Image as ImageIcon, Trash2, Upload } from 'lucide-react';
import { documentsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { DocumentType, PatientDocument } from '../api/types';
import { useToast } from '../components/Toast';
import { useConfirm } from '../components/ConfirmDialog';
import { EmptyState, ErrorBanner, Field, Spinner } from '../components/Common';
import { useApi } from '../utils/useApi';
import { formatDateTime, formatFileSize } from '../utils/format';
import { DOCUMENT_TYPE_LABEL } from '../utils/labels';

const MAX_BYTES = 15 * 1024 * 1024;
const ACCEPT = '.pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg';

/** Documentos adjuntos del paciente (PatientDocumentService: PDF, PNG y JPEG, máx. 15 MB). */
export function DocumentsTab({ patientId }: { patientId: number }) {
  const toast = useToast();
  const { confirm } = useConfirm();
  const fileRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [docType, setDocType] = useState<DocumentType>('ESTUDIO_LABORATORIO');
  const [description, setDescription] = useState('');
  const [uploading, setUploading] = useState(false);

  const { data, loading, error, reload } = useApi(() => documentsApi.list(patientId), [patientId]);

  const onUpload = async (e: FormEvent) => {
    e.preventDefault();
    if (!file) {
      toast.warning('Falta el archivo', 'Seleccione un archivo PDF, PNG o JPEG');
      return;
    }
    if (file.size > MAX_BYTES) {
      toast.warning('Archivo demasiado grande', 'El tamaño máximo permitido es 15 MB');
      return;
    }
    setUploading(true);
    try {
      await documentsApi.upload(patientId, file, docType, description);
      toast.success('Documento adjuntado', `${file.name} se guardó en el expediente`);
      setFile(null);
      setDescription('');
      if (fileRef.current) fileRef.current.value = '';
      reload();
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No fue posible adjuntar el documento'));
    } finally {
      setUploading(false);
    }
  };

  const onDelete = async (doc: PatientDocument) => {
    const ok = await confirm({
      title: 'Eliminar documento',
      message: <p>¿Desea eliminar el documento <strong>{doc.originalFileName ?? doc.fileName}</strong>?</p>,
      confirmLabel: 'Eliminar',
      tone: 'danger',
    });
    if (!ok) return;
    try {
      await documentsApi.remove(doc.id);
      toast.success('Documento eliminado', 'El documento fue eliminado del expediente');
      reload();
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No fue posible eliminar el documento'));
    }
  };

  const openFile = async (doc: PatientDocument, download: boolean) => {
    // Se descarga con fetch para enviar el token; luego se abre o guarda como blob local.
    const win = download ? null : window.open('', '_blank');
    try {
      const blob = await documentsApi.fetchFile(doc.id);
      const url = URL.createObjectURL(blob);
      if (download) {
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.originalFileName ?? doc.fileName;
        a.click();
      } else if (win) {
        win.location.href = url;
      }
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (err) {
      win?.close();
      toast.error('Error', errorMessage(err, 'No fue posible abrir el documento'));
    }
  };

  const docs = data ?? [];

  return (
    <div className="documents">
      <form className="upload-box" onSubmit={onUpload}>
        <div className="form-grid form-grid-3">
          <Field label="Archivo (PDF, PNG o JPEG, máx. 15 MB)" htmlFor="doc-file" required>
            <input id="doc-file" ref={fileRef} type="file" accept={ACCEPT} onChange={(e) => setFile(e.target.files?.[0] ?? null)} disabled={uploading} />
          </Field>
          <Field label="Tipo de documento" htmlFor="doc-type" required>
            <select id="doc-type" value={docType} onChange={(e) => setDocType(e.target.value as DocumentType)} disabled={uploading}>
              {(Object.keys(DOCUMENT_TYPE_LABEL) as DocumentType[]).map((t) => (
                <option key={t} value={t}>
                  {DOCUMENT_TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Descripción" htmlFor="doc-desc">
            <input id="doc-desc" value={description} onChange={(e) => setDescription(e.target.value)} disabled={uploading} placeholder="ej. Hemograma completo" />
          </Field>
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn-accent" disabled={uploading || !file}>
            {uploading ? <Spinner size={16} /> : <Upload size={16} aria-hidden />} Adjuntar documento
          </button>
        </div>
      </form>

      {error && <ErrorBanner message={error} onRetry={reload} />}

      {loading && !data ? (
        <div className="center-pad">
          <Spinner />
        </div>
      ) : docs.length === 0 ? (
        <EmptyState icon={FileText} message="No hay documentos adjuntos en el expediente" />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Documento</th>
                <th>Tipo</th>
                <th>Descripción</th>
                <th>Tamaño</th>
                <th>Fecha</th>
                <th className="col-actions">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {docs.map((d) => {
                const isImage = (d.fileType ?? '').startsWith('image/');
                return (
                  <tr key={d.id}>
                    <td className="strong">
                      <span className="doc-name">
                        {isImage ? <ImageIcon size={15} aria-hidden /> : <FileText size={15} aria-hidden />}
                        {d.originalFileName ?? d.fileName}
                      </span>
                    </td>
                    <td>{DOCUMENT_TYPE_LABEL[d.documentType] ?? d.documentType}</td>
                    <td>{d.description || '-'}</td>
                    <td className="nowrap">{formatFileSize(d.fileSizeBytes)}</td>
                    <td className="nowrap">{formatDateTime(d.createdAt)}</td>
                    <td className="col-actions">
                      <div className="row-actions">
                        <button className="icon-button" title="Ver" aria-label="Ver documento" onClick={() => openFile(d, false)}>
                          <Eye size={16} />
                        </button>
                        <button className="icon-button" title="Descargar" aria-label="Descargar documento" onClick={() => openFile(d, true)}>
                          <Download size={16} />
                        </button>
                        <button className="icon-button text-danger" title="Eliminar" aria-label="Eliminar documento" onClick={() => onDelete(d)}>
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
