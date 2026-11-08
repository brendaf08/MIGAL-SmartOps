import { Button, Group, SegmentedControl, Stack } from '@mantine/core';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { PageHeader } from '@/components/layout/PageHeader/PageHeader';
import { Alert } from '@/components/ui/Alert/Alert';
import { Card } from '@/components/ui/Card/Card';
import { Dropzone } from '@/components/ui/Dropzone/Dropzone';
import { FileItem } from '@/components/ui/Dropzone/FileItem';
import { Icon } from '@/components/ui/Icon/Icon';
import { Stepper } from '@/components/ui/Stepper/Stepper';
import { procesarDocumento, TIPOS_CARGA, type TipoDocumento } from './api';
import { leerDestino, rutaDestino } from './destino';

const PASOS = [
  { label: 'Cargar', description: 'Foto o PDF' },
  { label: 'Leer', description: 'Extraer líneas con OCR' },
  { label: 'Revisar', description: 'Confirmar productos y cantidades' },
];

/** CU-08 y CU-13 (alternativa 1): carga de un documento; el OCR solo extrae datos para su revisión. */
export function ProcesarDocumentoPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const destino = leerDestino(params.get('destino'));
  const [tipo, setTipo] = useState<TipoDocumento>('Guía de proveedor');
  const [archivo, setArchivo] = useState<File | null>(null);

  const procesar = useMutation({
    mutationFn: ({ file, tipo: tipoDocumento }: { file: File; tipo: TipoDocumento }) =>
      procesarDocumento(file, tipoDocumento),
    onSuccess: (documento) => {
      const query = destino ? `?destino=${destino}` : '';
      void navigate(`/documentos/${documento.id}/revision${query}`);
    },
  });

  const cargar = (files: File[]) => {
    const [file] = files;
    if (!file) return;
    setArchivo(file);
    procesar.mutate({ file, tipo });
  };

  const cargarOtra = () => {
    setArchivo(null);
    procesar.reset();
  };

  const estado = procesar.isPending ? 'processing' : procesar.isError ? 'error' : 'processed';

  return (
    <>
      <PageHeader
        title="Procesar documento"
        description="Cargar una foto o un PDF para extraer sus líneas con OCR."
        breadcrumbs={[
          { label: 'Documentos OCR', href: '/documentos' },
          { label: 'Procesar documento' },
        ]}
      />
      <Stack gap="lg">
        <Alert title="El procesamiento no modifica el inventario">
          El documento no genera ventas, reservas ni movimientos de Kardex. Se revisa antes de usar
          sus datos.
        </Alert>
        <Card title="Documento" description="Seleccionar el tipo y cargar la imagen o el PDF.">
          <Stack>
            <SegmentedControl
              aria-label="Tipo de documento"
              fullWidth
              data={TIPOS_CARGA}
              value={tipo}
              disabled={procesar.isPending}
              onChange={(valor) => setTipo(valor as TipoDocumento)}
            />
            <Dropzone camera disabled={procesar.isPending} onDrop={cargar} />
            {archivo && (
              <>
                <Stepper
                  aria-label="Progreso del documento"
                  steps={PASOS}
                  current={procesar.isPending ? 1 : 0}
                />
                <FileItem name={archivo.name} size={archivo.size} status={estado} />
              </>
            )}
            {procesar.isError && (
              <Alert tone="danger" title="Documento ilegible">
                <Stack gap="sm" align="flex-start">
                  <span>Cargar una foto más nítida o un PDF, o registrar los datos a mano.</span>
                  <Group gap="sm">
                    <Button
                      variant="default"
                      leftSection={<Icon name="upload" />}
                      onClick={cargarOtra}
                    >
                      Cargar otra foto
                    </Button>
                    <Button
                      variant="default"
                      leftSection={<Icon name="edit" />}
                      onClick={() => void navigate(rutaDestino(destino))}
                    >
                      Registrar manualmente
                    </Button>
                  </Group>
                </Stack>
              </Alert>
            )}
          </Stack>
        </Card>
      </Stack>
    </>
  );
}
