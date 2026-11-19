import { claseEstado, desdeInputFecha, nombreEstado, paraInputFecha, relojDe } from './formato';

describe('formato', () => {
  it('traduce estados conocidos y tolera los desconocidos', () => {
    expect(nombreEstado('SIN_INICIAR')).toBe('Sin iniciar');
    expect(nombreEstado('ALGO_NUEVO')).toBe('Algo nuevo');
    expect(nombreEstado(null)).toBe('—');
    expect(claseEstado('EN_CURSO')).toBe('en-curso');
  });

  it('convierte fechas entre el backend y el input datetime-local', () => {
    expect(paraInputFecha('2026-11-20T08:30:00')).toBe('2026-11-20T08:30');
    expect(desdeInputFecha('2026-11-20T08:30')).toBe('2026-11-20T08:30:00');
    expect(desdeInputFecha('2026-11-20T08:30:15')).toBe('2026-11-20T08:30:15');
  });

  it('muestra el cronómetro con y sin horas, sin valores negativos', () => {
    expect(relojDe(3725)).toBe('1:02:05');
    expect(relojDe(125)).toBe('2:05');
    expect(relojDe(-5)).toBe('0:00');
  });
});
