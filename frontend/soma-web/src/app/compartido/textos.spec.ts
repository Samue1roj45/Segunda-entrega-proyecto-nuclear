import { fechaHora, numero, porcentaje } from './textos';

describe('textos', () => {
  it('formatea porcentajes y números en es-CO', () => {
    expect(porcentaje(0.857)).toBe('86 %');
    expect(numero(4.5)).toBe('4,5');
  });
  it('une fecha y hora', () => { expect(fechaHora('2026-11-18T14:30:00')).toMatch(/2026/); });
});
