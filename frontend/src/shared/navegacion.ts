// Orden, grupos e iconos del menú lateral. Solo lo cambia el responsable del repositorio.
import type { LucideIcon } from 'lucide-react';
import {
  Bot, FileText, Receipt, ShoppingCart, Users, ScanText, Package, Boxes, TriangleAlert,
  ArrowLeftRight, History, Truck, PackageCheck, ClipboardList, Building2, UserCog,
} from 'lucide-react';
import type { CodigoPantalla } from './tipos';

export const grupos: { nombre: string; codigos: CodigoPantalla[] }[] = [
  { nombre: 'General', codigos: ['P05'] },
  { nombre: 'Comercial', codigos: ['P20', 'P19', 'P17', 'P22', 'P23'] },
  { nombre: 'Catálogo', codigos: ['P08', 'P11'] },
  { nombre: 'Inventario', codigos: ['P12', 'P13', 'P14', 'P15', 'P27'] },
  { nombre: 'Compras', codigos: ['P25', 'P06'] },
  { nombre: 'Administración', codigos: ['P02'] },
];

export const iconos: Partial<Record<CodigoPantalla, LucideIcon>> = {
  P05: Bot, P20: ShoppingCart, P19: Receipt, P17: FileText, P22: Users, P23: ScanText,
  P08: Package, P11: Boxes, P12: TriangleAlert, P13: ArrowLeftRight, P14: History,
  P15: Truck, P27: PackageCheck, P25: ClipboardList, P06: Building2, P02: UserCog,
};

export const nombreRol: Record<string, string> = {
  administrador: 'Administrador', vendedor: 'Vendedor', almacenero: 'Almacenero', reponedor: 'Reponedor',
};
