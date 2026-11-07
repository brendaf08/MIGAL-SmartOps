import type * as React from 'react';

export type Size = 'sm' | 'md' | 'lg';
export type Tone = 'neutral' | 'brand' | 'success' | 'warning' | 'danger' | 'info' | 'accent';
export type IconName = string;

export interface IconProps { name: IconName; size?: number; title?: string; strokeWidth?: number; className?: string }
export declare function Icon(props: IconProps): React.ReactElement | null;

export interface ButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  variant?: 'primary' | 'secondary' | 'outline' | 'subtle' | 'danger' | 'accent' | 'inverse';
  size?: Size; leftIcon?: IconName; rightIcon?: IconName; loading?: boolean; fullWidth?: boolean; children?: React.ReactNode;
}
export declare function Button(props: ButtonProps): React.ReactElement;

export interface IconButtonProps extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: IconName; label: string; variant?: 'subtle' | 'outline' | 'secondary' | 'primary' | 'danger' | 'inverse'; size?: Size; loading?: boolean;
}
export declare function IconButton(props: IconButtonProps): React.ReactElement;

export interface BadgeProps { tone?: Tone; icon?: IconName; variant?: 'sello'; className?: string; children?: React.ReactNode }
export declare function Badge(props: BadgeProps): React.ReactElement;

export type StatusEntity = 'cotizacion' | 'venta' | 'guia' | 'compra' | 'recepcion' | 'documento' | 'reserva' | 'stock' | 'registro';
export interface StatusBadgeProps { entity: StatusEntity; value: string; label?: string; variant?: 'sello'; className?: string }
export declare function StatusBadge(props: StatusBadgeProps): React.ReactElement;

export interface MovementBadgeProps { tipo: 'Entrada' | 'Salida'; cantidad?: number; className?: string }
export declare function MovementBadge(props: MovementBadgeProps): React.ReactElement;

export interface FieldProps {
  label?: string; description?: string; error?: string; required?: boolean; optional?: boolean; size?: Size; className?: string; id?: string;
}
export interface TextInputProps extends FieldProps, Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> { leftIcon?: IconName; rightSection?: React.ReactNode }
export declare function TextInput(props: TextInputProps): React.ReactElement;

export interface NumberInputProps extends FieldProps {
  value?: number | string; defaultValue?: number; onChange?: (value: number) => void; min?: number; max?: number; step?: number; unit?: string; disabled?: boolean; 'aria-label'?: string;
}
export declare function NumberInput(props: NumberInputProps): React.ReactElement;

export interface SelectOption { value: string; label: string; disabled?: boolean }
export interface SelectProps extends FieldProps, Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'size' | 'onChange'> {
  options: SelectOption[]; placeholder?: string; onChange?: (value: string, event: React.ChangeEvent<HTMLSelectElement>) => void;
}
export declare function Select(props: SelectProps): React.ReactElement;

export interface TextareaProps extends FieldProps, Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'size'> { rows?: number }
export declare function Textarea(props: TextareaProps): React.ReactElement;

export interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> { label: string; description?: string; indeterminate?: boolean }
export declare function Checkbox(props: CheckboxProps): React.ReactElement;

export interface SwitchProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> { label: string; description?: string }
export declare function Switch(props: SwitchProps): React.ReactElement;

export interface SearchInputProps { value?: string; onChange?: (value: string) => void; onSubmit?: (value: string) => void; placeholder?: string; label?: string; size?: Size; className?: string; 'aria-label'?: string }
export declare function SearchInput(props: SearchInputProps): React.ReactElement;

export interface CardProps { title?: string; titleId?: string; level?: 2 | 3 | 4; description?: string; actions?: React.ReactNode; footer?: React.ReactNode; flat?: boolean; padded?: boolean; as?: string; className?: string; children?: React.ReactNode }
export declare function Card(props: CardProps): React.ReactElement;

export interface StackProps { gap?: 1 | 2 | 3 | 4 | 5 | 6 | 8 | 10 | 12; direction?: 'column' | 'row'; align?: string; justify?: string; wrap?: boolean; as?: string; className?: string; style?: React.CSSProperties; children?: React.ReactNode }
export declare function Stack(props: StackProps): React.ReactElement;

export interface AlertProps { tone?: 'info' | 'success' | 'warning' | 'danger'; title?: string; icon?: IconName; action?: React.ReactNode; onClose?: () => void; className?: string; children?: React.ReactNode }
export declare function Alert(props: AlertProps): React.ReactElement;

export interface NotificationProps { tone?: 'info' | 'success' | 'warning' | 'danger'; title?: string; icon?: IconName; loading?: boolean; onClose?: () => void; className?: string; children?: React.ReactNode }
export declare function Notification(props: NotificationProps): React.ReactElement;

export interface EmptyStateProps { icon?: IconName; title: string; description?: string; action?: React.ReactNode; className?: string }
export declare function EmptyState(props: EmptyStateProps): React.ReactElement;

export interface SpinnerProps { size?: number; label?: string; className?: string }
export declare function Spinner(props: SpinnerProps): React.ReactElement;

export interface SkeletonProps { width?: number | string; height?: number | string; radius?: number | string; className?: string }
export declare function Skeleton(props: SkeletonProps): React.ReactElement;

export interface TabItem { value: string; label: string; count?: number; icon?: IconName; disabled?: boolean }
export interface TabsProps { items: TabItem[]; value: string; onChange?: (value: string) => void; idPrefix?: string; 'aria-label'?: string; className?: string }
export declare function Tabs(props: TabsProps): React.ReactElement;

export interface StepperProps { steps: { label: string; description?: string }[]; current: number; 'aria-label'?: string; className?: string }
export declare function Stepper(props: StepperProps): React.ReactElement;

export interface BreadcrumbsProps { items: { label: string; href?: string; onClick?: (e: React.MouseEvent) => void }[]; className?: string }
export declare function Breadcrumbs(props: BreadcrumbsProps): React.ReactElement;

export interface PageHeaderProps { title: string; description?: string; breadcrumbs?: BreadcrumbsProps['items']; status?: React.ReactNode; actions?: React.ReactNode; className?: string }
export declare function PageHeader(props: PageHeaderProps): React.ReactElement;

export interface TableColumn<T = any> { key: string; header: string; align?: 'left' | 'right' | 'center'; width?: number | string; numeric?: boolean; mono?: boolean; sortable?: boolean; render?: (row: T) => React.ReactNode }
export interface TableProps<T = any> {
  columns: TableColumn<T>[]; rows: T[]; rowKey?: string | ((row: T) => string | number);
  sort?: { key: string; dir: 'asc' | 'desc' }; onSort?: (key: string, dir: 'asc' | 'desc') => void;
  loading?: boolean; empty?: React.ReactNode; onRowClick?: (row: T) => void; selectedKey?: string | number; footer?: Record<string, React.ReactNode> | Record<string, React.ReactNode>[];
  density?: 'md' | 'sm'; stickyHeader?: boolean; caption?: string; className?: string;
}
export declare function Table<T = any>(props: TableProps<T>): React.ReactElement;

export interface PaginationProps { page: number; pages: number; total?: number; pageSize?: number; onChange: (page: number) => void; className?: string }
export declare function Pagination(props: PaginationProps): React.ReactElement;

export interface DropzoneProps {
  accept?: string[]; maxSize?: number; multiple?: boolean; onDrop?: (files: File[]) => void; onReject?: (errors: { file: File; reason: 'type' | 'size' }[]) => void;
  state?: 'idle' | 'dragover' | 'processing' | 'error'; error?: string; title?: string; busyTitle?: string; busyHint?: string; disabled?: boolean; className?: string;
}
export declare function Dropzone(props: DropzoneProps): React.ReactElement;

export interface FileItemProps { name: string; size?: number; status?: 'uploading' | 'done' | 'error'; progress?: number; processed?: boolean; error?: string; onRemove?: () => void }
export declare function FileItem(props: FileItemProps): React.ReactElement;

export interface ModalProps { open: boolean; title: string; description?: string; onClose?: () => void; size?: 'sm' | 'md' | 'lg'; footer?: React.ReactNode; inline?: boolean; alert?: boolean; className?: string; children?: React.ReactNode }
export declare function Modal(props: ModalProps): React.ReactElement | null;

export interface ConfirmDialogProps {
  open: boolean; title: string; description?: string; consequences?: string[]; tone?: 'danger' | 'primary'; confirmLabel?: string; confirmIcon?: IconName; cancelLabel?: string;
  loading?: boolean; inline?: boolean; onConfirm?: () => void; onCancel?: () => void; children?: React.ReactNode;
}
export declare function ConfirmDialog(props: ConfirmDialogProps): React.ReactElement | null;

export interface StockSummaryProps { fisico: number; reservado?: number; libre?: number; minimo?: number; unidad?: string; compact?: boolean; hideLevel?: boolean; className?: string }
export declare function StockSummary(props: StockSummaryProps): React.ReactElement;

export interface StockBarProps { libre?: number; reservado?: number; pendiente?: number; unidad?: string; label?: string; legend?: boolean; className?: string }
export declare function StockBar(props: StockBarProps): React.ReactElement;

export interface MatchConfidenceProps { score: number; thresholds?: { high: number; low: number }; className?: string }
export declare function MatchConfidence(props: MatchConfidenceProps): React.ReactElement;

export interface StatProps { label: string; value: React.ReactNode; hint?: string; tone?: 'warning' | 'danger' | 'success'; className?: string }
export declare function Stat(props: StatProps): React.ReactElement;

export interface ChatMessageProps { role: 'user' | 'agent'; name?: string; time?: string; tools?: { label: string; status: 'running' | 'done' }[]; uncertain?: boolean; className?: string; children?: React.ReactNode }
export declare function ChatMessage(props: ChatMessageProps): React.ReactElement;

export interface ChatComposerProps { value?: string; onChange?: (value: string) => void; onSend?: (text: string) => void; suggestions?: string[]; disabled?: boolean; placeholder?: string; className?: string }
export declare function ChatComposer(props: ChatComposerProps): React.ReactElement;

export interface NavItem { key: string; label: string; icon: IconName; roles?: string[]; badge?: string | number; href?: string }
export interface AppShellProps {
  nav: { label?: string; items: NavItem[] }[]; role?: string; current?: string; onNavigate?: (key: string) => void;
  user?: { name: string; role: string }; onLogout?: () => void; logo?: React.ReactNode; name?: string; subtitle?: string; header?: React.ReactNode; collapsed?: boolean; theme?: 'light' | 'dark'; onToggleTheme?: () => void; className?: string; children?: React.ReactNode;
}
export declare function AppShell(props: AppShellProps): React.ReactElement;

export declare const ICON_NAMES: string[];
export declare const STATUS: Record<StatusEntity, Record<string, [Tone, string, IconName]>>;
export declare function stockLevel(libre: number, minimo?: number): 'Normal' | 'Minimo' | 'Critico';
export declare function formatNumber(n: number, digits?: number): string;
export declare function formatCurrency(n: number): string;

declare global { interface Window { Migal: Record<string, unknown> } }
