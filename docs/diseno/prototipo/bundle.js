/* @ds-bundle: {"format":4,"namespace":"Migal","components":[{"name":"Icon"},{"name":"Button"},{"name":"IconButton"},{"name":"Badge"},{"name":"StatusBadge"},{"name":"MovementBadge"},{"name":"TextInput"},{"name":"NumberInput"},{"name":"Select"},{"name":"Textarea"},{"name":"Checkbox"},{"name":"Switch"},{"name":"SearchInput"},{"name":"Card"},{"name":"Stack"},{"name":"Columns"},{"name":"ActionBar"},{"name":"StockLocations"},{"name":"Constancia"},{"name":"Alert"},{"name":"Notification"},{"name":"EmptyState"},{"name":"Spinner"},{"name":"Skeleton"},{"name":"Tabs"},{"name":"Stepper"},{"name":"Breadcrumbs"},{"name":"PageHeader"},{"name":"Table"},{"name":"Pagination"},{"name":"Dropzone"},{"name":"Modal"},{"name":"ConfirmDialog"},{"name":"StockSummary"},{"name":"StockBar"},{"name":"MatchConfidence"},{"name":"Stat"},{"name":"ChatMessage"},{"name":"ChatComposer"},{"name":"AppShell"}]} */
(function () {
  'use strict';
  var React = window.React;
  var h = React.createElement;
  var useState = React.useState;
  var useRef = React.useRef;
  var useEffect = React.useEffect;

  /* ---------- utilidades ---------- */
  function cx() {
    var out = [];
    for (var i = 0; i < arguments.length; i++) { if (arguments[i]) out.push(arguments[i]); }
    return out.join(' ');
  }
  var counter = 0;
  function useUid(prefix) {
    var ref = useRef(null);
    if (ref.current === null) { counter += 1; ref.current = (prefix || 'mg') + '-' + counter; }
    return ref.current;
  }
  function pick(obj, drop) {
    var out = {};
    for (var k in obj) { if (Object.prototype.hasOwnProperty.call(obj, k) && drop.indexOf(k) === -1) out[k] = obj[k]; }
    return out;
  }
  function formatNumber(n, digits) {
    if (n === null || n === undefined || isNaN(n)) return '—';
    return Number(n).toLocaleString('es-PE', { minimumFractionDigits: digits || 0, maximumFractionDigits: digits || 0 });
  }
  function formatCurrency(n) {
    if (n === null || n === undefined || isNaN(n)) return '—';
    return 'S/\u00A0' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function formatBytes(b) {
    if (b < 1024) return b + '\u00A0B';
    if (b < 1024 * 1024) return (b / 1024).toFixed(0) + '\u00A0KB';
    var mb = b / 1024 / 1024;
    return (mb % 1 === 0 ? mb : mb.toFixed(1)) + '\u00A0MB';
  }

  /* ---------- Icon ---------- */
  var ICONS = {
    search: [['c', 11, 11, 6.5], 'M20 20l-4.2-4.2'],
    plus: ['M12 5v14M5 12h14'],
    minus: ['M5 12h14'],
    close: ['M6 6l12 12M18 6L6 18'],
    check: ['M5 12.5l4.5 4.5L19 7.5'],
    'chevron-down': ['M6 9l6 6 6-6'],
    'chevron-up': ['M6 15l6-6 6 6'],
    'chevron-left': ['M15 6l-6 6 6 6'],
    'chevron-right': ['M9 6l6 6-6 6'],
    'chevrons-up-down': ['M8 9l4-4 4 4M8 15l4 4 4-4'],
    'arrow-right': ['M5 12h14M13 6l6 6-6 6'],
    'arrow-left': ['M19 12H5M11 6l-6 6 6 6'],
    'arrow-up': ['M12 19V5M6 11l6-6 6 6'],
    'arrow-down': ['M12 5v14M6 13l6 6 6-6'],
    edit: ['M4 20v-4L14.5 5.5a2.1 2.1 0 0 1 3 0l1 1a2.1 2.1 0 0 1 0 3L8 20z', 'M13 7l4 4'],
    trash: ['M4 7h16M10 11v6M14 11v6M6 7l1 12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-12M9 7V4h6v3'],
    eye: ['M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z', ['c', 12, 12, 3]],
    download: ['M12 4v11M7 10l5 5 5-5M5 20h14'],
    upload: ['M12 16V5M7 10l5-5 5 5M5 20h14'],
    print: ['M7 9V4h10v5M7 17H5a1 1 0 0 1-1-1v-5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v5a1 1 0 0 1-1 1h-2M7 14h10v6H7z'],
    filter: ['M4 5h16l-6 7v6l-4 2v-8z'],
    calendar: [['r', 4, 5, 16, 15, 2], 'M4 10h16M9 3v4M15 3v4'],
    user: [['c', 12, 8, 4], 'M4 20a8 8 0 0 1 16 0'],
    users: [['c', 9, 8, 3.5], 'M3 19a6 6 0 0 1 12 0', ['c', 17, 9, 2.5], 'M17 14a5 5 0 0 1 4.5 5'],
    lock: [['r', 5, 11, 14, 9, 2], 'M8 11V8a4 4 0 0 1 8 0v3'],
    logout: ['M14 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-2M10 12h10M17 9l3 3-3 3'],
    menu: ['M4 7h16M4 12h16M4 17h16'],
    more: [['c', 5, 12, 1.2], ['c', 12, 12, 1.2], ['c', 19, 12, 1.2]],
    info: [['c', 12, 12, 9], 'M12 11v5M12 8h.01'],
    'alert-triangle': ['M12 4.5L3 19.5h18z', 'M12 10v4M12 17h.01'],
    'alert-circle': [['c', 12, 12, 9], 'M12 7.5v5M12 16h.01'],
    'check-circle': [['c', 12, 12, 9], 'M8 12.5l2.7 2.7L16 9.5'],
    'x-circle': [['c', 12, 12, 9], 'M9 9l6 6M15 9l-6 6'],
    'status-ok': [['r', 4, 4, 16, 16, 3], 'M8.5 12.5l2.5 2.5 4.5-5'],
    'status-danger': ['M8.5 3h7L21 8.5v7L15.5 21h-7L3 15.5v-7z', 'M12 8v5M12 16h.01'],
    refresh: ['M21 4v5h-5M20 13a8 8 0 1 1-2.4-6.6L21 9'],
    clock: [['c', 12, 12, 9], 'M12 7v5l3 2'],
    sliders: ['M4 7h9M17 7h3M4 17h3M11 17h9', ['c', 15, 7, 2], ['c', 9, 17, 2]],
    send: ['M20 4L10.5 13.5M20 4l-6 16-3.5-6.5L4 10z'],
    sun: [['c', 12, 12, 4], 'M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4'],
    moon: ['M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z'],
    camera: [['r', 3, 7, 18, 13, 2], 'M9 7l1.5-2h3L15 7', ['c', 12, 13.5, 3.5]],
    sparkles: ['M10 4l1.6 4.4L16 10l-4.4 1.6L10 16l-1.6-4.4L4 10l4.4-1.6z', 'M18 14l.8 2.2L21 17l-2.2.8L18 20l-.8-2.2L15 17l2.2-.8z'],
    bell: ['M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z', 'M10 20a2 2 0 0 0 4 0'],
    dashboard: [['r', 4, 4, 7, 7, 1.5], ['r', 13, 4, 7, 4, 1.5], ['r', 13, 11, 7, 9, 1.5], ['r', 4, 14, 7, 6, 1.5]],
    help: [['c', 12, 12, 9], 'M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .9-1 1.7M12 17h.01'],
    inbox: ['M4 13l2-8h12l2 8M4 13v6a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-6M4 13h5l1 2h4l1-2h5'],
    image: [['r', 4, 5, 16, 14, 2], ['c', 9, 10, 1.5], 'M20 16l-4.5-4.5L7 19'],
    'file-text': ['M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z', 'M14 3v5h5M9 13h6M9 17h6'],
    chat: ['M5 5h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H10l-5 4v-4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z'],
    producto: ['M12 3l8 4.5v9L12 21l-8-4.5v-9z', 'M4 7.5l8 4.5 8-4.5M12 12v9'],
    almacen: ['M3 9.5L12 4l9 5.5V20H3z', 'M8 20v-7h8v7M8 16.5h8'],
    tienda: ['M3.5 9.5L5 4h14l1.5 5.5M3.5 9.5a2.8 2.8 0 0 0 5.5 0 2.8 2.8 0 0 0 6 0 2.8 2.8 0 0 0 5.5 0M5 13v7h14v-7M10 20v-4h4v4'],
    kardex: ['M9 4h6a1 1 0 0 1 1 1v1H8V5a1 1 0 0 1 1-1z', 'M8 6H6a1 1 0 0 0-1 1v13a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-2M9 12h6M9 16h4'],
    movimiento: ['M8 5v14M4 15l4 4 4-4M16 19V5M12 9l4-4 4 4'],
    traslado: ['M4 8h14M14 4l4 4-4 4M20 16H6M10 12l-4 4 4 4'],
    despacho: ['M3 6h11v9.5H3z', 'M14 9.5h4l3 3.5v2.5h-7', ['c', 7.5, 17.5, 2], ['c', 17.5, 17.5, 2]],
    recepcion: ['M4 8l8-4.5L20 8v8.5L12 21 4 16.5z', 'M12 8v6M9.5 11.5L12 14l2.5-2.5'],
    'orden-compra': ['M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z', 'M14 3v5h5M12 12v5M9.5 14.5h5'],
    cotizacion: ['M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z', 'M14 3v5h5M9 15l2 2 4-4'],
    venta: ['M3.5 5h2.2l2.1 10h9.4l2-7.5H7', ['c', 9.5, 19, 1.3], ['c', 16.5, 19, 1.3]],
    proveedor: ['M3 20V10l5 3v-3l5 3V5h8v15z', 'M3 20h18M16.5 9h1M16.5 13h1'],
    scan: ['M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16'],
    reserva: ['M7 4h10a1 1 0 0 1 1 1v15l-6-4-6 4V5a1 1 0 0 1 1-1z'],
    rol: ['M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6z', 'M9 12l2 2 4-4'],
    sku: ['M4 6v12M7 6v12M10.5 6v12M14 6v12M17 6v12M20 6v12']
  };
  var ICON_NAMES = Object.keys(ICONS);

  function Icon(p) {
    var items = ICONS[p.name];
    var size = p.size || 20;
    if (!items) { return null; }
    var kids = items.map(function (it, i) {
      if (typeof it === 'string') return h('path', { key: i, d: it });
      if (it[0] === 'c') return h('circle', { key: i, cx: it[1], cy: it[2], r: it[3] });
      return h('rect', { key: i, x: it[1], y: it[2], width: it[3], height: it[4], rx: it[5] });
    });
    var a11y = p.title ? { role: 'img', 'aria-label': p.title } : { 'aria-hidden': 'true', focusable: 'false' };
    return h('svg', Object.assign({
      className: cx('mg-icon', p.className),
      width: size, height: size, viewBox: '0 0 24 24', fill: 'none',
      stroke: 'currentColor', strokeWidth: p.strokeWidth || 1.75, strokeLinecap: 'round', strokeLinejoin: 'round'
    }, a11y), kids);
  }

  /* ---------- Spinner / Skeleton ---------- */
  function Spinner(p) {
    var size = p.size || 20;
    return h('span', { className: cx('mg-spinner', p.className), role: 'status', 'aria-label': p.label || 'Cargando', style: { width: size, height: size } });
  }
  function Skeleton(p) {
    return h('span', {
      className: cx('mg-skeleton', p.className), 'aria-hidden': 'true',
      style: { width: p.width || '100%', height: p.height || 16, borderRadius: p.radius }
    });
  }

  /* ---------- Button / IconButton ---------- */
  function Button(p) {
    var variant = p.variant || 'primary';
    var size = p.size || 'md';
    var isz = size === 'lg' ? 24 : size === 'sm' ? 16 : 20;
    var rest = pick(p, ['variant', 'size', 'leftIcon', 'rightIcon', 'loading', 'fullWidth', 'children', 'className', 'type', 'disabled']);
    return h('button', Object.assign({
      type: p.type || 'button',
      className: cx('mg-btn', 'mg-btn-' + variant, 'mg-btn-' + size, 't-button-' + size, p.fullWidth && 'mg-btn-block', p.loading && 'is-loading', p.className),
      disabled: p.disabled,
      'aria-busy': p.loading ? 'true' : undefined,
      'aria-disabled': p.loading ? 'true' : undefined
    }, rest, p.loading ? { onClick: undefined } : null),
      p.loading ? h(Spinner, { size: isz, label: 'Procesando' }) : null,
      p.leftIcon ? h(Icon, { name: p.leftIcon, size: isz }) : null,
      p.children !== undefined ? h('span', { className: 'mg-btn-label' }, p.children) : null,
      p.rightIcon ? h(Icon, { name: p.rightIcon, size: isz }) : null);
  }
  function IconButton(p) {
    var variant = p.variant || 'subtle';
    var size = p.size || 'md';
    var isz = size === 'lg' ? 24 : size === 'sm' ? 16 : 20;
    var rest = pick(p, ['variant', 'size', 'icon', 'label', 'className', 'type', 'loading', 'disabled']);
    return h('button', Object.assign({
      type: p.type || 'button',
      className: cx('mg-btn', 'mg-iconbtn', 'mg-btn-' + variant, 'mg-btn-' + size, p.loading && 'is-loading', p.className),
      'aria-label': p.label, title: p.label,
      disabled: p.disabled,
      'aria-busy': p.loading ? 'true' : undefined,
      'aria-disabled': p.loading ? 'true' : undefined
    }, rest, p.loading ? { onClick: undefined } : null), p.loading ? h(Spinner, { size: isz, label: 'Procesando' }) : h(Icon, { name: p.icon, size: isz }));
  }

  /* ---------- Badge / StatusBadge / MovementBadge ---------- */
  function Badge(p) {
    return h('span', { className: cx('mg-badge', 'mg-badge-' + (p.tone || 'neutral'), p.variant === 'sello' ? 'mg-badge-sello t-overline' : 't-caption', p.className) },
      p.icon && p.variant !== 'sello' ? h(Icon, { name: p.icon, size: 14, strokeWidth: 2 }) : null,
      p.children);
  }
  var STATUS = {
    cotizacion: {
      Emitida: ['info', 'Emitida', 'info'],
      AceptadaCliente: ['brand', 'Aceptada por el cliente', 'check'],
      ValidadaMigal: ['success', 'Validada por MIGAL', 'status-ok'],
      Rechazada: ['danger', 'Rechazada', 'status-danger'],
      Vencida: ['neutral', 'Vencida', 'clock']
    },
    venta: {
      PendienteAbastecimiento: ['warning', 'Pendiente de abastecimiento', 'alert-triangle'],
      PendienteDespacho: ['info', 'Pendiente de despacho', 'info'],
      ListaDespacho: ['brand', 'Lista para despacho', 'check'],
      Despachada: ['success', 'Despachada', 'status-ok'],
      Cancelada: ['danger', 'Cancelada', 'status-danger']
    },
    guia: {
      Preparacion: ['neutral', 'En preparación', 'clock'],
      Emitida: ['info', 'Emitida', 'info'],
      SalidaConfirmada: ['brand', 'Salida confirmada', 'despacho'],
      Entregada: ['success', 'Entregada', 'status-ok']
    },
    compra: {
      Pendiente: ['warning', 'Pendiente', 'alert-triangle'],
      Parcial: ['info', 'Recepción parcial', 'info'],
      Completada: ['success', 'Completada', 'status-ok']
    },
    recepcion: {
      Pendiente: ['warning', 'Pendiente de validación', 'alert-triangle'],
      Validada: ['success', 'Validada', 'status-ok']
    },
    documento: {
      Pendiente: ['neutral', 'Pendiente', 'clock'],
      Procesado_OCR: ['info', 'Procesado por OCR', 'info'],
      Validado: ['success', 'Validado', 'status-ok']
    },
    reserva: {
      Activa: ['brand', 'Reserva activa', 'reserva'],
      Liberada: ['neutral', 'Liberada', 'arrow-up'],
      Consumida: ['success', 'Consumida', 'status-ok']
    },
    stock: {
      Normal: ['success', 'Stock suficiente', 'status-ok'],
      Minimo: ['warning', 'En el mínimo', 'alert-triangle'],
      Critico: ['danger', 'Stock crítico', 'status-danger']
    },
    registro: {
      Activo: ['success', 'Activo', 'status-ok'],
      Inactivo: ['neutral', 'Inactivo', 'x-circle']
    }
  };
  function StatusBadge(p) {
    var group = STATUS[p.entity] || {};
    var def = group[p.value] || ['neutral', String(p.value), 'info'];
    return h(Badge, { tone: def[0], icon: def[2], variant: p.variant, className: p.className }, p.label || def[1]);
  }
  function MovementBadge(p) {
    var entrada = p.tipo === 'Entrada';
    return h('span', { className: cx('mg-move', entrada ? 'is-in' : 'is-out', p.className) },
      h(Badge, { tone: entrada ? 'success' : 'brand', icon: entrada ? 'arrow-down' : 'arrow-up' }, p.tipo),
      p.cantidad !== undefined ? h('span', { className: 'mg-move-qty t-data' }, (entrada ? '+' : '−') + formatNumber(Math.abs(p.cantidad))) : null);
  }

  /* ---------- Formularios ---------- */
  function Field(p) {
    return h('div', { className: cx('mg-field', p.className) },
      p.label ? h('label', { className: 'mg-label t-label', htmlFor: p.id },
        p.label,
        p.required ? h('span', { className: 'mg-req', 'aria-hidden': 'true' }, ' *') : null,
        p.optional ? h('span', { className: 'mg-opt t-body-sm' }, ' (opcional)') : null) : null,
      p.description ? h('div', { id: p.id + '-d', className: 'mg-desc t-body-sm' }, p.description) : null,
      p.children,
      p.error ? h('div', { id: p.id + '-e', className: 'mg-error t-body-sm', role: 'alert' }, h(Icon, { name: 'status-danger', size: 16 }), h('span', null, p.error)) : null);
  }
  function describedBy(id, p) {
    var out = [];
    if (p.description) out.push(id + '-d');
    if (p.error) out.push(id + '-e');
    return out.length ? out.join(' ') : undefined;
  }
  var FIELD_KEYS = ['label', 'description', 'error', 'required', 'optional', 'leftIcon', 'rightSection', 'size', 'className', 'id', 'unit'];
  function TextInput(p) {
    var uid = useUid('in');
    var id = p.id || uid;
    var size = p.size || 'md';
    var rest = pick(p, FIELD_KEYS);
    return h(Field, { id: id, label: p.label, description: p.description, error: p.error, required: p.required, optional: p.optional, className: p.className },
      h('div', { className: cx('mg-input', 'mg-input-' + size, p.error && 'is-error', p.disabled && 'is-disabled') },
        p.leftIcon ? h(Icon, { name: p.leftIcon, size: size === 'lg' ? 24 : 20, className: 'mg-input-icon' }) : null,
        h('input', Object.assign({
          id: id, type: 'text', className: size === 'lg' ? 't-body-lg' : 't-body',
          'aria-invalid': p.error ? 'true' : undefined, 'aria-required': p.required ? 'true' : undefined,
          'aria-describedby': describedBy(id, p)
        }, rest)),
        p.rightSection ? h('span', { className: 'mg-input-right' }, p.rightSection) : null));
  }
  function NumberInput(p) {
    var uid = useUid('num');
    var id = p.id || uid;
    var size = p.size || 'md';
    var step = p.step || 1;
    var ctrl = p.value !== undefined;
    var st = useState(p.defaultValue !== undefined ? p.defaultValue : 0);
    var value = ctrl ? p.value : st[0];
    function set(v) {
      if (p.min !== undefined && v < p.min) v = p.min;
      if (p.max !== undefined && v > p.max) v = p.max;
      if (!ctrl) st[1](v);
      if (p.onChange) p.onChange(v);
    }
    var num = Number(value) || 0;
    return h(Field, { id: id, label: p.label, description: p.description, error: p.error, required: p.required, optional: p.optional, className: p.className },
      h('div', { className: cx('mg-input', 'mg-number', 'mg-input-' + size, p.error && 'is-error', p.disabled && 'is-disabled') },
        h('button', { type: 'button', className: 'mg-step', 'aria-label': 'Disminuir', disabled: p.disabled || (p.min !== undefined && num <= p.min), onClick: function () { set(num - step); }, tabIndex: -1 }, h(Icon, { name: 'minus', size: 18 })),
        h('input', {
          id: id, type: 'text', inputMode: 'decimal', value: value, disabled: p.disabled, className: cx('mg-num-field', size === 'lg' ? 't-body-lg' : 't-body'), 'aria-label': p['aria-label'],
          'aria-invalid': p.error ? 'true' : undefined, 'aria-describedby': describedBy(id, p),
          onChange: function (e) { var v = e.target.value.replace(',', '.'); if (v === '' || v === '-') { if (!ctrl) st[1](v); return; } if (!isNaN(Number(v))) set(Number(v)); },
          onKeyDown: function (e) { if (e.key === 'ArrowUp') { e.preventDefault(); set(num + step); } if (e.key === 'ArrowDown') { e.preventDefault(); set(num - step); } }
        }),
        p.unit ? h('span', { className: 'mg-unit t-body-sm' }, p.unit) : null,
        h('button', { type: 'button', className: 'mg-step', 'aria-label': 'Aumentar', disabled: p.disabled || (p.max !== undefined && num >= p.max), onClick: function () { set(num + step); }, tabIndex: -1 }, h(Icon, { name: 'plus', size: 18 }))));
  }
  function Select(p) {
    var uid = useUid('sel');
    var id = p.id || uid;
    var size = p.size || 'md';
    var rest = pick(p, FIELD_KEYS.concat(['options', 'onChange', 'placeholder']));
    return h(Field, { id: id, label: p.label, description: p.description, error: p.error, required: p.required, optional: p.optional, className: p.className },
      h('div', { className: cx('mg-input', 'mg-select', 'mg-input-' + size, p.error && 'is-error', p.disabled && 'is-disabled') },
        h('select', Object.assign({
          id: id, className: size === 'lg' ? 't-body-lg' : 't-body',
          'aria-invalid': p.error ? 'true' : undefined, 'aria-describedby': describedBy(id, p),
          onChange: function (e) { if (p.onChange) p.onChange(e.target.value, e); }
        }, rest),
          p.placeholder ? h('option', { value: '' }, p.placeholder) : null,
          (p.options || []).map(function (o) { return h('option', { key: o.value, value: o.value, disabled: o.disabled }, o.label); })),
        h(Icon, { name: 'chevron-down', size: 20, className: 'mg-select-icon' })));
  }
  function Textarea(p) {
    var uid = useUid('ta');
    var id = p.id || uid;
    var rest = pick(p, FIELD_KEYS);
    return h(Field, { id: id, label: p.label, description: p.description, error: p.error, required: p.required, optional: p.optional, className: p.className },
      h('div', { className: cx('mg-input', 'mg-textarea', p.error && 'is-error', p.disabled && 'is-disabled') },
        h('textarea', Object.assign({ id: id, rows: p.rows || 3, className: 't-body', 'aria-invalid': p.error ? 'true' : undefined, 'aria-describedby': describedBy(id, p) }, rest))));
  }
  function Checkbox(p) {
    var uid = useUid('cb');
    var id = p.id || uid;
    var rest = pick(p, ['label', 'description', 'className', 'id', 'indeterminate']);
    return h('label', { className: cx('mg-check', p.indeterminate && 'is-indet', p.disabled && 'is-disabled', p.className), htmlFor: id },
      h('input', Object.assign({ id: id, type: 'checkbox' }, rest)),
      h('span', { className: 'mg-check-box', 'aria-hidden': 'true' }, h(Icon, { name: p.indeterminate ? 'minus' : 'check', size: 14, strokeWidth: 3 })),
      h('span', { className: 'mg-check-text' }, h('span', { className: 't-body' }, p.label), p.description ? h('span', { className: 't-body-sm mg-desc' }, p.description) : null));
  }
  function Switch(p) {
    var uid = useUid('sw');
    var id = p.id || uid;
    var rest = pick(p, ['label', 'description', 'className', 'id']);
    return h('label', { className: cx('mg-switch', p.disabled && 'is-disabled', p.className), htmlFor: id },
      h('input', Object.assign({ id: id, type: 'checkbox', role: 'switch' }, rest)),
      h('span', { className: 'mg-switch-track', 'aria-hidden': 'true' }, h('span', { className: 'mg-switch-thumb' })),
      h('span', { className: 'mg-check-text' }, h('span', { className: 't-body' }, p.label), p.description ? h('span', { className: 't-body-sm mg-desc' }, p.description) : null));
  }
  function SearchInput(p) {
    var size = p.size || 'md';
    var hasValue = p.value !== undefined && p.value !== '';
    return h(TextInput, {
      label: p.label, size: size, className: p.className, type: 'search', value: p.value, placeholder: p.placeholder || 'Buscar por nombre o SKU',
      leftIcon: 'search', 'aria-label': p.label ? undefined : (p['aria-label'] || 'Buscar'),
      onChange: function (e) { if (p.onChange) p.onChange(e.target.value, e); },
      onKeyDown: function (e) { if (e.key === 'Enter' && p.onSubmit) p.onSubmit(p.value); },
      rightSection: hasValue ? h(IconButton, { icon: 'close', label: 'Limpiar búsqueda', size: 'sm', onClick: function () { if (p.onChange) p.onChange(''); } }) : null
    });
  }

  /* ---------- Layout ---------- */
  function Stack(p) {
    var dir = p.direction || 'column';
    return h(p.as || 'div', {
      className: cx('mg-stack', p.className),
      style: Object.assign({ flexDirection: dir, gap: 'var(--space-' + (p.gap || 4) + ')', alignItems: p.align, justifyContent: p.justify, flexWrap: p.wrap ? 'wrap' : undefined }, p.style)
    }, p.children);
  }
  function Columns(p) {
    return h(p.as || 'div', { className: cx('mg-cols', 'mg-cols-' + (p.layout || 'two'), p.className), style: p.gap ? { gap: 'var(--space-' + p.gap + ')' } : undefined }, p.children);
  }
  function Card(p) {
    var head = p.title || p.actions;
    return h(p.as || 'section', { className: cx('mg-card', p.flat && 'is-flat', p.className), 'aria-labelledby': p.title ? p.titleId : undefined },
      head ? h('header', { className: 'mg-card-head' },
        h('div', { className: 'mg-card-titles' }, p.title ? h('h' + (p.level || 2), { id: p.titleId, className: 't-h3' }, p.title) : null, p.description ? h('p', { className: 't-body-sm mg-desc' }, p.description) : null),
        p.actions ? h('div', { className: 'mg-card-actions' }, p.actions) : null) : null,
      h('div', { className: cx('mg-card-body', p.padded === false && 'is-flush') }, p.children),
      p.footer ? h('footer', { className: 'mg-card-foot' }, p.footer) : null);
  }

  /* ---------- Feedback ---------- */
  var TONE_ICON = { info: 'info', success: 'status-ok', warning: 'alert-triangle', danger: 'status-danger', neutral: 'info' };
  function Alert(p) {
    var tone = p.tone || 'info';
    return h('div', { className: cx('mg-alert', 'mg-alert-' + tone, p.className), role: tone === 'danger' || tone === 'warning' ? 'alert' : 'status' },
      h(Icon, { name: p.icon || TONE_ICON[tone], size: 22, className: 'mg-alert-icon' }),
      h('div', { className: 'mg-alert-body' }, p.title ? h('div', { className: 't-label' }, p.title) : null, p.children ? h('div', { className: 't-body' }, p.children) : null, p.action ? h('div', { className: 'mg-alert-action' }, p.action) : null),
      p.onClose ? h(IconButton, { icon: 'close', label: 'Cerrar aviso', size: 'sm', onClick: p.onClose }) : null);
  }
  function Notification(p) {
    var tone = p.tone || 'info';
    return h('div', { className: cx('mg-toast', 'mg-toast-' + tone, p.className), role: 'status', 'aria-live': 'polite' },
      p.loading ? h(Spinner, { size: 22 }) : h(Icon, { name: p.icon || TONE_ICON[tone], size: 22, className: 'mg-toast-icon' }),
      h('div', { className: 'mg-alert-body' }, p.title ? h('div', { className: 't-label' }, p.title) : null, p.children ? h('div', { className: 't-body-sm mg-desc' }, p.children) : null),
      p.onClose ? h(IconButton, { icon: 'close', label: 'Cerrar notificación', size: 'sm', onClick: p.onClose }) : null);
  }
  function EmptyState(p) {
    return h('div', { className: cx('mg-empty', p.className) },
      h('span', { className: 'mg-empty-icon' }, h(Icon, { name: p.icon || 'inbox', size: 28 })),
      h('div', { className: 't-h4' }, p.title),
      p.description ? h('p', { className: 't-body mg-desc' }, p.description) : null,
      p.action ? h('div', { className: 'mg-empty-action' }, p.action) : null);
  }

  /* ---------- Navegación ---------- */
  function Tabs(p) {
    var items = p.items || [];
    var ref = useRef(null);
    function onKey(e, i) {
      var n = items.length, next = null;
      if (e.key === 'ArrowRight') next = (i + 1) % n;
      if (e.key === 'ArrowLeft') next = (i - 1 + n) % n;
      if (e.key === 'Home') next = 0;
      if (e.key === 'End') next = n - 1;
      if (next !== null) {
        e.preventDefault();
        var btn = ref.current.querySelectorAll('[role=tab]')[next];
        if (btn) btn.focus();
        if (p.onChange && !items[next].disabled) p.onChange(items[next].value);
      }
    }
    return h('div', { ref: ref, className: cx('mg-tabs', p.className), role: 'tablist', 'aria-label': p['aria-label'] || 'Secciones' },
      items.map(function (it, i) {
        var active = it.value === p.value;
        return h('button', {
          key: it.value, type: 'button', role: 'tab', 'aria-selected': active ? 'true' : 'false', tabIndex: active ? 0 : -1, disabled: it.disabled,
          id: p.idPrefix ? p.idPrefix + '-' + it.value : undefined, 'aria-controls': p.idPrefix ? p.idPrefix + '-panel-' + it.value : undefined,
          className: cx('mg-tab', 't-label', active && 'is-active'),
          onClick: function () { if (p.onChange) p.onChange(it.value); },
          onKeyDown: function (e) { onKey(e, i); }
        }, it.icon ? h(Icon, { name: it.icon, size: 18 }) : null, it.label, it.count !== undefined ? h('span', { className: cx('mg-tab-count t-caption', it.tone === 'warning' && 'is-warning') }, it.count) : null);
      }));
  }
  function Stepper(p) {
    var steps = p.steps || [];
    return h('ol', { className: cx('mg-stepper', p.className), 'aria-label': p['aria-label'] || 'Progreso' },
      steps.map(function (s, i) {
        var state = i < p.current ? 'done' : i === p.current ? 'current' : 'todo';
        return h('li', { key: i, className: cx('mg-step-item', 'is-' + state), 'aria-current': state === 'current' ? 'step' : undefined },
          h('span', { className: 'mg-step-dot t-caption' }, state === 'done' ? h(Icon, { name: 'check', size: 14, strokeWidth: 3 }) : i + 1),
          h('span', { className: 'mg-step-text' }, h('span', { className: 't-label' }, s.label), s.description ? h('span', { className: 't-body-sm mg-desc' }, s.description) : null),
          state === 'done' ? h('span', { className: 'mg-sr' }, ' (completado)') : null);
      }));
  }
  function Breadcrumbs(p) {
    var items = p.items || [];
    return h('nav', { 'aria-label': 'Ruta de navegación', className: cx('mg-crumbs', p.className) },
      h('ol', null, items.map(function (it, i) {
        var last = i === items.length - 1;
        return h('li', { key: i },
          last ? h('span', { 'aria-current': 'page', className: 't-body-sm' }, it.label)
            : h('a', { href: it.href || '#', className: 't-body-sm', onClick: it.onClick }, it.label),
          last ? null : h(Icon, { name: 'chevron-right', size: 14 }));
      })));
  }
  function PageHeader(p) {
    return h('header', { className: cx('mg-pagehead', p.className) },
      p.breadcrumbs ? h(Breadcrumbs, { items: p.breadcrumbs }) : null,
      h('div', { className: 'mg-pagehead-row' },
        h('div', { className: 'mg-pagehead-titles' },
          h('div', { className: 'mg-pagehead-line' }, h('h1', { className: 't-h1' }, p.title), p.status || null),
          p.description ? h('p', { className: 't-body mg-desc' }, p.description) : null),
        p.actions ? h('div', { className: 'mg-pagehead-actions' }, p.actions) : null));
  }

  /* ---------- Tabla y paginación ---------- */
  function Table(p) {
    var cols = p.columns || [];
    var rows = p.rows || [];
    var sort = p.sort || {};
    function key(r, i) { return typeof p.rowKey === 'function' ? p.rowKey(r) : p.rowKey ? r[p.rowKey] : i; }
    var body;
    if (p.loading) {
      body = [0, 1, 2, 3, 4].map(function (i) {
        return h('tr', { key: 'sk' + i }, cols.map(function (c) { return h('td', { key: c.key, 'data-label': c.header, className: cx(c.align === 'right' || c.numeric ? 'is-right' : undefined, c.phone === false && 'mg-hide-phone', c.phoneOnly && 'mg-only-phone', c.primary && 'is-primary') }, h(Skeleton, { width: c.numeric ? 56 : (i % 2 ? '70%' : '90%'), height: 14 })); }));
      });
    } else if (!rows.length) {
      body = h('tr', null, h('td', { colSpan: cols.length, className: 'mg-table-empty' }, p.empty || h(EmptyState, { title: 'Sin resultados', description: 'No hay registros que coincidan con los filtros aplicados.' })));
    } else {
      body = rows.map(function (r, i) {
        var k = key(r, i);
        var clickable = !!p.onRowClick;
        return h('tr', {
          key: k, className: cx(clickable && 'is-clickable', p.selectedKey !== undefined && p.selectedKey === k && 'is-selected', p.changedKey !== undefined && p.changedKey === k && 'is-changed'),
          onClick: clickable ? function () { p.onRowClick(r); } : undefined,
          tabIndex: clickable ? 0 : undefined,
          onKeyDown: clickable ? function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); p.onRowClick(r); } } : undefined,
          'aria-selected': p.selectedKey !== undefined ? (p.selectedKey === k ? 'true' : 'false') : undefined
        }, cols.map(function (c) {
          var right = c.align === 'right' || c.numeric;
          return h('td', { key: c.key, 'data-label': c.header, className: cx(right && 'is-right', c.numeric && 'mg-num', c.mono && 'mg-mono t-sku', c.primary && 'is-primary', c.block && 'is-block', c.phone === false && 'mg-hide-phone', c.phoneOnly && 'mg-only-phone') }, c.render ? c.render(r) : r[c.key]);
        }));
      });
    }
    return h('div', { className: cx('mg-table-wrap', p.className) },
      h('table', { className: cx('mg-table', p.density === 'sm' && 'is-dense', p.stickyHeader && 'is-sticky', p.stack !== false && 'is-stack'), 'aria-busy': p.loading ? 'true' : undefined },
        p.caption ? h('caption', { className: 'mg-sr' }, p.caption) : null,
        h('thead', null, h('tr', null, cols.map(function (c) {
          var right = c.align === 'right' || c.numeric;
          var active = sort.key === c.key;
          var ariaSort = c.sortable ? (active ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none') : undefined;
          return h('th', { key: c.key, scope: 'col', 'aria-sort': ariaSort, style: c.width ? { width: c.width } : undefined, className: cx(right && 'is-right', c.phone === false && 'mg-hide-phone', c.phoneOnly && 'mg-only-phone') },
            c.sortable ? h('button', { type: 'button', className: cx('mg-sort t-th', active && 'is-active'), onClick: function () { if (p.onSort) p.onSort(c.key, active && sort.dir === 'asc' ? 'desc' : 'asc'); } },
              c.header, h(Icon, { name: active ? (sort.dir === 'asc' ? 'chevron-up' : 'chevron-down') : 'chevrons-up-down', size: 16 }))
              : h('span', { className: 't-th' }, c.header));
        }))),
        h('tbody', null, body),
        p.footer && rows.length ? h('tfoot', null, (Array.isArray(p.footer) ? p.footer : [p.footer]).map(function (f, ri) {
          return h('tr', { key: ri }, cols.map(function (c) {
            var right = c.align === 'right' || c.numeric;
            return h('td', { key: c.key, className: cx(right && 'is-right', c.numeric && 'mg-num', c.phone === false && 'mg-hide-phone', c.phoneOnly && 'mg-only-phone') }, f[c.key] !== undefined ? f[c.key] : null);
          }));
        })) : null));
  }
  function Pagination(p) {
    var page = p.page, pages = p.pages;
    var list = [];
    for (var i = 1; i <= pages; i++) { if (i === 1 || i === pages || Math.abs(i - page) <= 1) list.push(i); else if (list[list.length - 1] !== '…') list.push('…'); }
    var from = p.total ? (page - 1) * p.pageSize + 1 : 0;
    var to = p.total ? Math.min(page * p.pageSize, p.total) : 0;
    return h('nav', { className: cx('mg-pager', p.className), 'aria-label': 'Paginación' },
      p.total ? h('span', { className: 't-body-sm mg-desc' }, 'Mostrando ', h('b', null, from + '–' + to), ' de ', h('b', null, formatNumber(p.total))) : h('span'),
      h('div', { className: 'mg-pager-pages' },
        h(IconButton, { icon: 'chevron-left', label: 'Página anterior', size: 'sm', variant: 'outline', disabled: page <= 1, onClick: function () { p.onChange(page - 1); } }),
        h('span', { className: 'mg-pager-compact t-label' }, 'Página ' + page + ' de ' + pages),
        list.map(function (n, i) {
          return n === '…' ? h('span', { key: 'e' + i, className: 'mg-pager-gap' }, n)
            : h('button', { key: n, type: 'button', className: cx('mg-pager-btn t-button-sm', n === page && 'is-active'), 'aria-current': n === page ? 'page' : undefined, 'aria-label': 'Página ' + n, onClick: function () { p.onChange(n); } }, n);
        }),
        h(IconButton, { icon: 'chevron-right', label: 'Página siguiente', size: 'sm', variant: 'outline', disabled: page >= pages, onClick: function () { p.onChange(page + 1); } })));
  }

  /* ---------- Carga de archivos ---------- */
  var DEFAULT_ACCEPT = ['image/jpeg', 'image/png', 'application/pdf'];
  function typeLabel(accept) {
    var m = { 'image/jpeg': 'JPG', 'image/png': 'PNG', 'application/pdf': 'PDF' };
    var n = accept.map(function (a) { return m[a] || a; });
    return n.length > 1 ? n.slice(0, -1).join(', ') + ' o ' + n[n.length - 1] : n[0];
  }
  function Dropzone(p) {
    var accept = p.accept || DEFAULT_ACCEPT;
    var maxSize = p.maxSize || 10 * 1024 * 1024;
    var st = useState(false);
    var er = useState(null);
    var over = p.state === 'dragover' || st[0];
    var error = p.error || er[0] || (p.state === 'error' ? 'No fue posible cargar el archivo.' : null);
    var busy = p.state === 'processing';
    function handle(list) {
      var files = Array.prototype.slice.call(list || []);
      var ok = [], bad = [];
      files.forEach(function (f) {
        if (accept.indexOf(f.type) === -1) bad.push({ file: f, reason: 'type' });
        else if (f.size > maxSize) bad.push({ file: f, reason: 'size' });
        else ok.push(f);
      });
      if (bad.length) er[1](bad[0].reason === 'type' ? 'Formato no admitido. Cargar archivos ' + typeLabel(accept) + '.' : 'El archivo supera el tamaño máximo de ' + formatBytes(maxSize) + '.');
      else er[1](null);
      if (bad.length && p.onReject) p.onReject(bad);
      if (ok.length && p.onDrop) p.onDrop(p.multiple ? ok : ok.slice(0, 1));
    }
    var icon = busy ? null : error ? 'status-danger' : 'upload';
    return h('div', { className: cx('mg-dz-wrap', p.className) },
      h('label', {
        className: cx('mg-dz', over && 'is-over', error && 'is-error', busy && 'is-busy', p.disabled && 'is-disabled'),
        onDragOver: function (e) { e.preventDefault(); if (!p.disabled) st[1](true); },
        onDragLeave: function () { st[1](false); },
        onDrop: function (e) { e.preventDefault(); st[1](false); if (!p.disabled && !busy) handle(e.dataTransfer.files); }
      },
        h('input', { type: 'file', className: 'mg-sr-input', accept: accept.join(','), multiple: !!p.multiple, disabled: p.disabled || busy, onChange: function (e) { handle(e.target.files); e.target.value = ''; } }),
        busy ? h(Spinner, { size: 32, label: 'Procesando documento' }) : h('span', { className: 'mg-dz-icon' }, h(Icon, { name: icon, size: 28 })),
        h('span', { className: 't-h4' }, busy ? (p.busyTitle || 'Procesando documento') : over ? 'Soltar para cargar' : (p.title || 'Arrastrar un archivo aquí')),
        h('span', { className: 't-body mg-desc' }, busy ? (p.busyHint || 'Extrayendo el texto del documento. Puede tardar unos segundos.') : h('span', null, 'o ', h('span', { className: 'mg-link' }, 'seleccionar desde el equipo'))),
        h('span', { className: 't-caption mg-desc' }, typeLabel(accept) + '. Máximo ' + formatBytes(maxSize) + '.')),
      p.camera ? h('label', { className: cx('mg-btn mg-btn-outline mg-btn-md t-button-md mg-dz-camera', (p.disabled || busy) && 'is-disabled') },
        h(Icon, { name: 'camera', size: 20 }), h('span', { className: 'mg-btn-label' }, 'Tomar foto'),
        h('input', { type: 'file', accept: 'image/*', capture: 'environment', className: 'mg-sr-input', disabled: p.disabled || busy, onChange: function (e) { handle(e.target.files); e.target.value = ''; } })) : null,
      error ? h('div', { className: 'mg-error t-body-sm', role: 'alert' }, h(Icon, { name: 'status-danger', size: 16 }), h('span', null, error)) : null);
  }
  function FileItem(p) {
    var status = p.status || 'done';
    var isImg = /\.(png|jpe?g)$/i.test(p.name || '');
    return h('li', { className: cx('mg-file', 'is-' + status) },
      h('span', { className: 'mg-file-icon' }, h(Icon, { name: isImg ? 'image' : 'file-text', size: 22 })),
      h('span', { className: 'mg-file-main' },
        h('span', { className: 'mg-file-name t-label' }, p.name),
        h('span', { className: 't-body-sm mg-desc' }, p.size !== undefined ? formatBytes(p.size) : '', p.error ? ': ' + p.error : ''),
        status === 'uploading' ? h('span', { className: 'mg-progress', role: 'progressbar', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': p.progress || 0, 'aria-label': 'Carga de ' + p.name }, h('span', { style: { width: (p.progress || 0) + '%' } })) : null),
      status === 'done' ? h(StatusBadge, { entity: 'documento', value: p.processed ? 'Procesado_OCR' : 'Pendiente', label: p.processed ? 'Procesado por OCR' : 'Cargado' }) : null,
      status === 'error' ? h(Badge, { tone: 'danger', icon: 'status-danger' }, 'Error') : null,
      status === 'uploading' ? h(Badge, { tone: 'info', icon: 'upload' }, 'Cargando ' + (p.progress || 0) + ' %') : null,
      p.onRemove ? h(IconButton, { icon: 'close', label: 'Quitar ' + p.name, size: 'sm', onClick: p.onRemove }) : null);
  }

  /* ---------- Modal ---------- */
  var FOCUSABLE = 'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])';
  function Modal(p) {
    var ref = useRef(null);
    var uid = useUid('dlg');
    useEffect(function () {
      if (!p.open || p.inline) return undefined;
      var prev = document.activeElement;
      var el = ref.current;
      if (el) { var first = el.querySelector('[data-autofocus]') || el.querySelector(FOCUSABLE) || el; first.focus(); }
      function onKey(e) {
        if (e.key === 'Escape' && p.onClose) { p.onClose(); }
        if (e.key === 'Tab' && el) {
          var f = el.querySelectorAll(FOCUSABLE);
          if (!f.length) { e.preventDefault(); return; }
          var a = f[0], z = f[f.length - 1];
          if (e.shiftKey && document.activeElement === a) { e.preventDefault(); z.focus(); }
          else if (!e.shiftKey && document.activeElement === z) { e.preventDefault(); a.focus(); }
        }
      }
      document.addEventListener('keydown', onKey);
      return function () { document.removeEventListener('keydown', onKey); if (prev && prev.focus) prev.focus(); };
    }, [p.open, p.inline]);
    if (!p.open) return null;
    var dialog = h('div', { ref: ref, className: cx('mg-modal', 'mg-modal-' + (p.size || 'md'), p.inline && 'is-inline', p.className), role: p.alert ? 'alertdialog' : 'dialog', 'aria-modal': p.inline ? undefined : 'true', 'aria-labelledby': uid + '-t', 'aria-describedby': p.description ? uid + '-d' : undefined, tabIndex: -1 },
      h('header', { className: 'mg-modal-head' },
        h('div', null, h('h2', { id: uid + '-t', className: 't-h3' }, p.title), p.description ? h('p', { id: uid + '-d', className: 't-body mg-desc' }, p.description) : null),
        p.onClose ? h(IconButton, { icon: 'close', label: 'Cerrar', size: 'sm', onClick: p.onClose }) : null),
      p.children ? h('div', { className: 'mg-modal-body' }, p.children) : null,
      p.footer ? h('footer', { className: 'mg-modal-foot' }, p.footer) : null);
    if (p.inline) return h('div', { className: 'mg-modal-stage' }, dialog);
    var tree = h('div', { className: 'mg-overlay', onMouseDown: function (e) { if (e.target === e.currentTarget && p.onClose && !p.alert) p.onClose(); } }, dialog);
    return window.ReactDOM && window.ReactDOM.createPortal ? window.ReactDOM.createPortal(tree, document.body) : tree;
  }
  function ConfirmDialog(p) {
    var danger = (p.tone || 'danger') === 'danger';
    return h(Modal, {
      open: p.open, inline: p.inline, alert: true, size: 'sm', title: p.title, description: p.description, onClose: p.onCancel,
      footer: h(React.Fragment, null,
        h(Button, { variant: 'outline', onClick: p.onCancel, disabled: p.loading, 'data-autofocus': true }, p.cancelLabel || 'Cancelar'),
        h(Button, { variant: danger ? 'danger' : 'primary', onClick: p.onConfirm, loading: p.loading, leftIcon: p.confirmIcon }, p.confirmLabel || 'Confirmar'))
    }, (p.consequences && p.consequences.length) || p.children ? h('div', { className: 'mg-confirm' },
      h('span', { className: cx('mg-confirm-icon', danger ? 'is-danger' : 'is-warning') }, h(Icon, { name: danger ? 'status-danger' : 'alert-triangle', size: 22 })),
      h('div', null, p.children, p.consequences ? h('ul', { className: 'mg-consequences t-body' }, p.consequences.map(function (c, i) { return h('li', { key: i }, c); })) : null)) : null);
  }

  /* ---------- Dominio: inventario ---------- */
  function stockLevel(libre, minimo) {
    if (libre <= 0) return 'Critico';
    if (minimo !== undefined && minimo !== null && libre <= minimo) return 'Minimo';
    return 'Normal';
  }
  function StockSummary(p) {
    var reservado = p.reservado || 0;
    var libre = p.libre !== undefined ? p.libre : p.fisico - reservado;
    var nivel = stockLevel(libre, p.minimo);
    var u = p.unidad ? ' ' + p.unidad : '';
    return h('div', { className: cx('mg-stock', p.compact && 'is-compact', p.className) },
      h('dl', { className: 'mg-stock-grid' },
        h('div', { className: 'is-free' }, h('dt', { className: 't-caption' }, 'Libre'), h('dd', { className: 't-data mg-num' }, formatNumber(libre), u ? h('span', { className: 'mg-unit t-caption' }, u) : null)),
        h('div', null, h('dt', { className: 't-caption' }, 'Físico'), h('dd', { className: 't-data mg-num' }, formatNumber(p.fisico))),
        h('div', null, h('dt', { className: 't-caption' }, 'Reservado'), h('dd', { className: 't-data mg-num' }, formatNumber(reservado)))),
      p.minimo !== undefined && !p.hideLevel ? h('div', { className: 'mg-stock-level' }, h(StatusBadge, { entity: 'stock', value: nivel }), h('span', { className: 't-caption mg-desc' }, 'Mínimo: ' + formatNumber(p.minimo))) : null);
  }
  function StockBar(p) {
    var libre = p.libre || 0, reservado = p.reservado || 0, pendiente = p.pendiente || 0;
    var total = libre + reservado + pendiente || 1;
    var seg = [
      { k: 'libre', v: libre, label: 'Libre' },
      { k: 'reservado', v: reservado, label: 'Reservado' },
      { k: 'pendiente', v: pendiente, label: 'Pendiente de abastecimiento' }
    ];
    var shown = seg.filter(function (s) { return s.v > 0 || s.k !== 'pendiente'; });
    var marks = [];
    if (p.minimo !== undefined) marks.push({ k: 'min', v: p.minimo, label: 'Mínimo' });
    if (p.reposicion !== undefined) marks.push({ k: 'rep', v: p.reposicion, label: 'Reposición' });
    var summary = shown.map(function (s) { return s.label + ' ' + formatNumber(s.v); }).join(', ') + marks.map(function (m) { return ', ' + m.label + ' ' + formatNumber(m.v); }).join('');
    var paso = p.empaque && p.empaque.paso ? p.empaque.paso : total / 10;
    var ticks = [];
    for (var t = paso; t < total && ticks.length < 24; t += paso) ticks.push(t);
    var unit = p.unidad ? ' ' + p.unidad : '';
    return h('div', { className: cx('mg-bar', p.compact && 'is-compact', p.className) },
      h('div', { className: 'mg-bar-frame' },
        h('div', { className: 'mg-bar-track', role: 'img', 'aria-label': (p.label || 'Distribución de stock') + ': ' + summary },
          seg.map(function (s) { return s.v > 0 ? h('span', { key: s.k, className: 'mg-bar-seg is-' + s.k, style: { flexGrow: s.v } }) : null; })),
        p.compact ? null : h('div', { className: 'mg-bar-ruler', 'aria-hidden': 'true' },
          ticks.map(function (v, i) { return h('i', { key: i, style: { left: (v / total * 100) + '%' } }); })),
        p.compact ? null : marks.map(function (m) {
          return h('span', { key: m.k, className: 'mg-bar-mark is-' + m.k, 'aria-hidden': 'true', style: { left: Math.min(100, m.v / total * 100) + '%' } });
        })),
      p.compact ? null : h('div', { className: 'mg-bar-scale t-caption', 'aria-hidden': 'true' }, h('span', null, '0'), h('span', null, formatNumber(total) + unit + (p.empaque && p.empaque.unidad ? ' (marcas cada ' + formatNumber(p.empaque.paso) + ' ' + p.empaque.unidad + ')' : ''))),
      p.legend === false ? null : h('ul', { className: 'mg-bar-legend t-caption' },
        shown.map(function (s) {
          return h('li', { key: s.k }, h('span', { className: 'mg-swatch is-' + s.k, 'aria-hidden': 'true' }), s.label, ' ', h('b', { className: 'mg-num' }, formatNumber(s.v)), unit);
        }),
        marks.map(function (m) {
          return h('li', { key: m.k }, h('span', { className: 'mg-swatch is-mark', 'aria-hidden': 'true' }), m.label, ' ', h('b', { className: 'mg-num' }, formatNumber(m.v)), unit);
        })));
  }
  function StockLocations(p) {
    var items = p.ubicaciones || [];
    var unit = p.unidad ? ' ' + p.unidad : '';
    return h('ul', { className: cx('mg-locs', p.compact && 'is-compact', p.className), 'aria-label': p.label || 'Existencias por ubicación' },
      items.map(function (u) {
        var libre = (u.cantidad || 0) - (u.reservado || 0);
        var empty = !u.cantidad;
        return h('li', { key: u.nombre, className: cx('mg-loc', empty && 'is-empty'), style: { flexGrow: empty ? 0.6 : Math.max(1, Math.min(3, u.cantidad / (p.escala || 400))) } },
          h('span', { className: 't-caption mg-loc-name' }, u.nombre),
          h('span', { className: 't-data mg-num mg-loc-qty' }, empty ? 'Sin stock' : formatNumber(libre)),
          empty ? null : h('span', { className: 'mg-loc-bar', 'aria-hidden': 'true' }, h('span', { style: { width: Math.round(libre / u.cantidad * 100) + '%' } })),
          empty || p.compact ? null : h('span', { className: 't-caption mg-loc-sub' }, 'Reservado ' + formatNumber(u.reservado || 0) + unit));
      }));
  }
  function MatchConfidence(p) {
    var th = p.thresholds || { high: 0.85, low: 0.6 };
    var s = p.score;
    var level = s >= th.high ? 'high' : s >= th.low ? 'mid' : 'low';
    var tone = level === 'high' ? 'success' : level === 'mid' ? 'warning' : 'danger';
    var icon = level === 'high' ? 'status-ok' : level === 'mid' ? 'alert-triangle' : 'status-danger';
    var text = level === 'high' ? 'Coincidencia alta' : level === 'mid' ? 'Revisar coincidencia' : 'Sin coincidencia';
    return h('div', { className: cx('mg-match', 'is-' + level, p.compact && 'is-compact', p.className) },
      h('div', { className: 'mg-match-top' }, h(Badge, { tone: tone, icon: icon }, text), h('span', { className: 't-data mg-num' }, Math.round(s * 100) + ' %')),
      h('div', { className: 'mg-progress', role: 'meter', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': Math.round(s * 100), 'aria-label': 'Similitud con el catálogo' }, h('span', { style: { width: Math.round(s * 100) + '%' } })),
      level === 'low' && !p.compact ? h('div', { className: 't-body-sm mg-desc' }, 'Se requiere validación manual antes de continuar.') : null);
  }
  function ActionBar(p) {
    return h('div', { className: cx('mg-actionbar', p.className), role: 'region', 'aria-label': p['aria-label'] || 'Resumen y acciones' },
      h('div', { className: 'mg-actionbar-info' },
        p.summary ? h('div', { className: 'mg-actionbar-summary' }, p.summary) : null,
        p.reason ? h('div', { className: 'mg-actionbar-reason t-body-sm', role: 'status' }, h(Icon, { name: 'alert-triangle', size: 16 }), h('span', null, p.reason)) : null),
      h('div', { className: 'mg-actionbar-actions' }, p.children));
  }
  function Constancia(p) {
    useEffect(function () {
      var still = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (p.vibrate !== false && !still && navigator.vibrate) { try { navigator.vibrate(30); } catch (e) { /* sin vibración */ } }
    }, []);
    return h('section', { className: cx('mg-receipt', p.className), 'aria-labelledby': p.titleId || 'mg-receipt-t', role: 'status' },
      h('div', { className: 'mg-receipt-seal' }, p.sello),
      h('h2', { id: p.titleId || 'mg-receipt-t', className: 't-h2' }, p.title),
      p.description ? h('p', { className: 't-body mg-desc' }, p.description) : null,
      h('table', { className: 'mg-receipt-table' },
        h('caption', { className: 'mg-sr' }, 'Cambios de existencias'),
        h('thead', null, h('tr', null, h('th', { scope: 'col', className: 't-th' }, 'Producto'), h('th', { scope: 'col', className: 't-th is-right' }, 'Antes'), h('th', { scope: 'col', className: 't-th is-right' }, 'Después'))),
        h('tbody', null, (p.cambios || []).map(function (c, i) {
          return h('tr', { key: i }, h('td', null, h('span', { className: 't-label' }, c.producto), c.ubicacion ? h('span', { className: 't-caption mg-desc mg-receipt-where' }, c.ubicacion) : null),
            h('td', { className: 'is-right mg-num t-data' }, formatNumber(c.antes)),
            h('td', { className: 'is-right mg-num t-data mg-receipt-after' }, h(Icon, { name: 'arrow-right', size: 14, className: 'mg-receipt-arrow' }), formatNumber(c.despues), c.unidad ? h('span', { className: 't-caption mg-desc' }, ' ' + c.unidad) : null));
        }))),
      p.nota ? h('p', { className: 't-body-sm mg-desc' }, p.nota) : null,
      h('div', { className: 'mg-receipt-actions' }, p.actions));
  }
  function Stat(p) {
    var flag = { warning: 'alert-triangle', danger: 'status-danger', success: 'status-ok' }[p.tone];
    return h('div', { className: cx('mg-stat', p.tone && 'is-' + p.tone, p.className) },
      h('div', { className: 'mg-stat-top t-label' }, flag ? h(Icon, { name: flag, size: 16, className: 'mg-stat-flag' }) : null, h('span', null, p.label)),
      h('div', { className: 't-stat mg-num' }, p.value),
      p.hint ? h('div', { className: 't-body-sm mg-desc' }, p.hint) : null);
  }

  /* ---------- Agente de IA ---------- */
  function ChatMessage(p) {
    var agent = p.role === 'agent';
    return h('div', { className: cx('mg-msg', agent ? 'is-agent' : 'is-user', p.className) },
      agent ? h('span', { className: 'mg-avatar is-agent', 'aria-hidden': 'true' }, h(Icon, { name: 'sparkles', size: 18 })) : null,
      h('div', { className: 'mg-msg-main' },
        h('div', { className: 'mg-msg-meta t-caption' }, agent ? 'Agente MIGAL' : (p.name || 'Usted'), p.time ? ', ' + p.time : ''),
        p.tools && p.tools.length ? h('ul', { className: 'mg-tools' }, p.tools.map(function (t, i) {
          return h('li', { key: i, className: cx('mg-tool t-caption', t.status === 'running' && 'is-running') }, t.status === 'running' ? h(Spinner, { size: 14 }) : h(Icon, { name: 'check', size: 14, strokeWidth: 2.5 }), t.label);
        })) : null,
        h('div', { className: cx('mg-bubble t-body', p.uncertain && 'is-uncertain') }, p.children),
        p.uncertain ? h('div', { className: 'mg-msg-note t-caption' }, h(Icon, { name: 'info', size: 14 }), 'Respuesta sin datos suficientes en el sistema.') : null));
  }
  function ChatComposer(p) {
    var st = useState('');
    var ctrl = p.value !== undefined;
    var value = ctrl ? p.value : st[0];
    function setV(v) { if (!ctrl) st[1](v); if (p.onChange) p.onChange(v); }
    function send() { var v = String(value).trim(); if (!v || p.disabled) return; if (p.onSend) p.onSend(v); setV(''); }
    return h('div', { className: cx('mg-composer', p.className) },
      p.suggestions && p.suggestions.length ? h('div', { className: 'mg-suggest' }, p.suggestions.map(function (s) { return h('button', { key: s, type: 'button', className: 'mg-chip t-body-sm', onClick: function () { if (p.onSend) p.onSend(s); } }, s); })) : null,
      h('div', { className: cx('mg-input', 'mg-composer-box', p.disabled && 'is-disabled') },
        h('textarea', { rows: 1, className: 't-body', value: value, disabled: p.disabled, placeholder: p.placeholder || 'Consultar stock, ubicación o productos críticos', 'aria-label': 'Mensaje para el agente',
          onChange: function (e) { setV(e.target.value); },
          onKeyDown: function (e) { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } } }),
        h(IconButton, { icon: 'send', label: 'Enviar consulta', variant: 'primary', disabled: p.disabled || !String(value).trim(), onClick: send })),
      h('div', { className: 't-caption mg-desc' }, 'El agente solo consulta datos autorizados para el rol actual y no modifica registros.'));
  }

  /* ---------- Estructura de aplicación ---------- */
  function initials(name) { return String(name || '?').split(' ').slice(0, 2).map(function (w) { return w.charAt(0); }).join('').toUpperCase(); }
  function AppShell(p) {
    var navState = useState(false);
    var navOpen = navState[0];
    useEffect(function () {
      if (!navOpen) return undefined;
      function onKey(e) { if (e.key === 'Escape') navState[1](false); }
      document.addEventListener('keydown', onKey);
      return function () { document.removeEventListener('keydown', onKey); };
    }, [navOpen]);
    var role = p.role;
    var groups = p.nav || [];
    var user = p.user;
    return h('div', { className: cx('mg-shell', p.collapsed && 'is-collapsed', navOpen && 'is-nav-open', p.warehouse && 'is-warehouse', p.className) },
      h('a', { className: 'mg-skip t-label', href: '#contenido' }, 'Saltar al contenido'),
      h('div', { className: 'mg-scrim', 'aria-hidden': 'true', onClick: function () { navState[1](false); } }),
      h('aside', { className: 'mg-sidebar', id: 'mg-nav' },
        h('div', { className: 'mg-sidebar-brand' }, p.logo || null, h('div', { className: 'mg-sidebar-name' }, h('span', { className: 't-h4' }, p.name || 'MIGAL SmartOps'), h('span', { className: 't-caption mg-sidebar-sub' }, p.subtitle || 'Ferretería Progresol Migal')),
          h(IconButton, { icon: 'close', label: 'Cerrar menú de navegación', variant: 'inverse', className: 'mg-drawer-close', onClick: function () { navState[1](false); } })),
        h('nav', { 'aria-label': 'Navegación principal', className: 'mg-sidebar-nav' },
          groups.map(function (g, gi) {
            var items = (g.items || []).filter(function (it) { return !it.roles || !role || it.roles.indexOf(role) !== -1; });
            if (!items.length) return null;
            return h('div', { key: gi, className: 'mg-nav-group' },
              g.label ? h('div', { className: 't-overline mg-nav-label' }, g.label) : null,
              h('ul', null, items.map(function (it) {
                var active = it.key === p.current;
                return h('li', { key: it.key }, h('a', {
                  href: it.href || '#' + it.key, className: cx('mg-nav-item', 't-label', active && 'is-active'), 'aria-current': active ? 'page' : undefined, title: it.label,
                  onClick: function (e) { navState[1](false); if (p.onNavigate) { e.preventDefault(); p.onNavigate(it.key); } }
                }, h(Icon, { name: it.icon, size: 20 }), h('span', { className: 'mg-nav-text' }, it.label), it.badge ? h('span', { className: 'mg-nav-badge t-caption' }, it.badge) : null));
              })));
          }))),
      h('div', { className: 'mg-shell-main' },
        h('header', { className: 'mg-topbar' },
          h(IconButton, { icon: 'menu', label: 'Abrir menú de navegación', className: 'mg-menu-btn', 'aria-expanded': navOpen ? 'true' : 'false', 'aria-controls': 'mg-nav', onClick: function () { navState[1](!navOpen); } }),
          h('div', { className: 'mg-topbar-brand t-h4' }, p.name || 'MIGAL SmartOps'),
          h('div', { className: 'mg-topbar-slot' }, p.header || null),
          p.onToggleWarehouse ? h(IconButton, { icon: 'almacen', label: p.warehouse ? 'Desactivar modo almacén' : 'Activar modo almacén', 'aria-pressed': p.warehouse ? 'true' : 'false', variant: p.warehouse ? 'secondary' : 'subtle', onClick: p.onToggleWarehouse }) : null,
          p.onToggleTheme ? h(IconButton, { icon: p.theme === 'dark' ? 'sun' : 'moon', label: p.theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro', onClick: p.onToggleTheme }) : null,
          user ? h('div', { className: 'mg-user' },
            h('span', { className: 'mg-avatar', 'aria-hidden': 'true' }, initials(user.name)),
            h('span', { className: 'mg-user-text' }, h('span', { className: 't-label' }, user.name), h('span', { className: 't-caption' }, user.role)),
            p.onLogout ? h(IconButton, { icon: 'logout', label: 'Cerrar sesión', variant: 'subtle', onClick: p.onLogout }) : null) : null),
        h('main', { id: 'contenido', className: 'mg-content', tabIndex: -1 }, p.children)));
  }

  var api = {
    Icon: Icon, Button: Button, IconButton: IconButton, Badge: Badge, StatusBadge: StatusBadge, MovementBadge: MovementBadge,
    Field: Field, TextInput: TextInput, NumberInput: NumberInput, Select: Select, Textarea: Textarea, Checkbox: Checkbox, Switch: Switch, SearchInput: SearchInput,
    Card: Card, Stack: Stack, Columns: Columns, ActionBar: ActionBar, StockLocations: StockLocations, Constancia: Constancia, Alert: Alert, Notification: Notification, EmptyState: EmptyState, Spinner: Spinner, Skeleton: Skeleton,
    Tabs: Tabs, Stepper: Stepper, Breadcrumbs: Breadcrumbs, PageHeader: PageHeader, Table: Table, Pagination: Pagination,
    Dropzone: Dropzone, FileItem: FileItem, Modal: Modal, ConfirmDialog: ConfirmDialog,
    StockSummary: StockSummary, StockBar: StockBar, MatchConfidence: MatchConfidence, Stat: Stat,
    ChatMessage: ChatMessage, ChatComposer: ChatComposer, AppShell: AppShell,
    ICON_NAMES: ICON_NAMES, STATUS: STATUS, stockLevel: stockLevel, formatNumber: formatNumber, formatCurrency: formatCurrency
  };
  window.Migal = window.Migal || {};
  Object.assign(window.Migal, api);
})();
