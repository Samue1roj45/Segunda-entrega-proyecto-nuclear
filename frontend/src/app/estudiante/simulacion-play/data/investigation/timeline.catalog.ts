import { TimelineEventDef } from './investigation.types';

export const TIMELINE_CATALOG: TimelineEventDef[] = [
  {
    id: 'tl-discusion',
    timeLabel: '21:30',
    label: 'Discusión inicial en el hogar',
    unlockClueIds: ['h-madre-t2'],
    order: 1,
  },
  {
    id: 'tl-escalada',
    timeLabel: '22:15',
    label: 'Escalada del conflicto y amenazas',
    unlockClueIds: ['h-hermano-t2', 'h-guardia-t2'],
    order: 2,
  },
  {
    id: 'tl-lesion',
    timeLabel: '23:00',
    label: 'Lesión y emergencia',
    unlockClueIds: ['h-hermano-t3', 'c-reportes-t1'],
    order: 3,
  },
  {
    id: 'tl-urgencias',
    timeLabel: '23:20',
    label: 'Ingreso de la víctima a urgencias',
    unlockClueIds: ['h-medico-t1', 'h-historia-t1'],
    order: 4,
  },
  {
    id: 'tl-intervencion',
    timeLabel: '00:10',
    label: 'Intervención institucional en curso',
    unlockClueIds: ['c-victima-t2', 'c-secretaria-t2'],
    order: 5,
  },
];
