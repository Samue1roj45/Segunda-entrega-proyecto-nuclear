import { expect, test } from '@playwright/test';
import { Estado, instalarServidorSimulado } from './servidor-simulado';

/** Recorrido completo: el docente programa e inicia una sesión, la estudiante participa y el docente califica y publica. */
test('recorrido completo docente → estudiante → docente', async ({ page }) => {
  const estado = new Estado();
  await instalarServidorSimulado(page, estado);
  await page.addInitScript(() => sessionStorage.setItem('soma.saludado', '1'));

  // 1) El docente entra
  await page.goto('/login');
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await expect(page.getByRole('heading', { name: /Hola, Carlos|Buenos días, Carlos|Buenas tardes, Carlos|Buenas noches, Carlos/ })).toBeVisible();

  // 2) Programa una sesión eligiendo a la estudiante en el selector con búsqueda
  await page.goto('/sesiones/nueva');
  await page.selectOption('select[name=caso]', { label: 'Conformidad en el grupo de trabajo' });
  await page.fill('input[name=esp]', 'Psicología Social I');
  await page.fill('input[name=fi]', '2026-11-20T14:00');
  await page.fill('input[name=ff]', '2026-11-20T16:00');
  await page.fill('input[name=buscar]', 'ana');
  await expect(page.locator('.fila-est')).toHaveCount(1);
  await page.locator('.fila-est').first().click();
  await page.getByRole('button', { name: 'Programar sesión' }).click();
  await expect(page).toHaveURL(/\/sesiones\/1$/);

  // 3) La inicia (se ve la línea de tiempo y el botón cambia)
  await expect(page.locator('.linea-tiempo li')).toHaveCount(5);
  await page.getByRole('button', { name: 'Iniciar', exact: true }).click();
  await expect(page.locator('.linea-tiempo li.hecho')).not.toHaveCount(1);

  // 4) Sale y entra la estudiante con su código
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.click('button.cambio');
  await page.fill('#correo-login', 'ana.perez@institucion.edu.co');
  await page.fill('#codigo-login', 'CODIGO_VALIDO_01');
  await page.click('button.ingresar');
  await page.goto('/mis-sesiones');
  await page.getByRole('button', { name: 'Comenzar' }).click();
  await expect(page).toHaveURL(/\/participar\/9$/);

  // 5) Responde (se ve «Respuesta guardada» y el progreso) y finaliza
  await page.getByLabel(/Mantiene su postura/).check();
  await expect(page.getByText('Respuesta guardada')).toBeVisible();
  await page.getByRole('button', { name: 'Finalizar intento' }).click();
  await expect(page.getByRole('alertdialog')).toContainText('Antes de finalizar');
  await page.getByRole('alertdialog').getByRole('button', { name: 'Finalizar intento' }).click();
  await expect(page).toHaveURL(/\/mis-resultados$/);
  await expect(page.locator('.destacado')).toContainText('5,0');

  // 6) El docente califica: nota oficial y publicación
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.locator('#correo-login')).toBeVisible();
  await page.waitForTimeout(400); // deja que el formulario termine de inicializarse
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await page.waitForURL(/\/inicio/);
  await page.goto('/resultados?practica=1');
  await expect(page.locator('.resumen-estadistico')).toContainText('5,0');
  await page.getByRole('button', { name: 'Detalle' }).click();
  await page.getByRole('button', { name: 'Elegir como nota oficial' }).click();
  await expect.poll(() => estado.oficial).toBe(true);
  await page.getByRole('button', { name: 'Publicar calificaciones' }).click();
  await page.getByRole('alertdialog').getByRole('button', { name: 'Publicar' }).click();
  await expect.poll(() => estado.publicada).toBe(true);
  await expect(page.locator('.toast').last()).toContainText('Calificaciones publicadas');
});

test('el docente con el permiso adicional elimina un caso desde la lista', async ({ page }) => {
  const estado = new Estado();
  await instalarServidorSimulado(page, estado);
  await page.addInitScript(() => sessionStorage.setItem('soma.saludado', '1'));
  await page.goto('/login');
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await page.waitForURL(/\/inicio/);
  await page.goto('/casos');
  await page.getByRole('button', { name: 'Eliminar' }).click();
  await expect(page.getByRole('alertdialog')).toContainText('no se puede deshacer');
  await page.getByRole('alertdialog').getByRole('button', { name: 'Eliminar' }).click();
  await expect.poll(() => estado.casoEliminado).toBe(true);
  await expect(page.locator('.vacio')).toBeVisible();
});

test('Ctrl+K abre el buscador y lleva a la sección elegida', async ({ page }) => {
  await instalarServidorSimulado(page, new Estado());
  await page.addInitScript(() => sessionStorage.setItem('soma.saludado', '1'));
  await page.goto('/login');
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await page.waitForURL(/\/inicio/);
  await expect(page.locator('soma-paleta button').first()).toBeVisible();
  await page.keyboard.press('Control+k');
  await expect(page.locator('input:focus')).toBeVisible();
  await page.keyboard.type('sesion');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/sesiones$/);
});

test('la paleta también ejecuta acciones (cambiar color)', async ({ page }) => {
  await instalarServidorSimulado(page, new Estado());
  await page.addInitScript(() => sessionStorage.setItem('soma.saludado', '1'));
  await page.goto('/login');
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await page.waitForURL(/\/inicio/);
  await expect(page.locator('soma-paleta button').first()).toBeVisible();
  await page.keyboard.press('Control+k');
  await expect(page.locator('input:focus')).toBeVisible();
  await page.keyboard.type('siguiente color');
  await page.keyboard.press('Enter');
  await expect(page.locator('html')).toHaveAttribute('data-acento', 'oceano');
});

test('modo noche se aplica solo dentro del panel y se recuerda', async ({ page }) => {
  await instalarServidorSimulado(page, new Estado());
  await page.addInitScript(() => sessionStorage.setItem('soma.saludado', '1'));
  await page.goto('/login');
  expect(await page.evaluate(() => document.documentElement.dataset['tema'])).toBeUndefined();
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await page.waitForURL(/\/inicio/);
  await page.getByRole('button', { name: 'Cambiar a modo noche' }).click();
  await expect.poll(() => page.evaluate(() => document.documentElement.dataset['tema'])).toBe('noche');
  await page.reload();
  await expect.poll(() => page.evaluate(() => document.documentElement.dataset['tema'])).toBe('noche');
});

test('una dirección que no existe muestra la página 404 dentro del panel', async ({ page }) => {
  await instalarServidorSimulado(page, new Estado());
  await page.addInitScript(() => sessionStorage.setItem('soma.saludado', '1'));
  await page.goto('/login');
  await page.fill('#correo-login', 'carlos.gomez@institucion.edu.co');
  await page.fill('#clave-login', 'Clave-Docente-01');
  await page.click('button.ingresar');
  await page.waitForURL(/\/inicio/);
  await page.goto('/ruta-que-no-existe');
  await expect(page.getByText('Esta página se perdió en la niebla')).toBeVisible();
});
