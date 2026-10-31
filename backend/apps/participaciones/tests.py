from datetime import timedelta

from django.core import mail
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework import status

from apps.participaciones.models import Participacion
from apps.practicas.models import Practica
from apps.resultados.test_fixtures import (
    autorizar,
    cliente,
    crear_caso_con_rubrica,
    crear_docente,
    crear_estudiante,
    crear_practica,
    token_estudiante,
    token_usuario,
)


@override_settings(EMAIL_BACKEND='django.core.mail.backends.locmem.EmailBackend')
class ParticipacionRF12Tests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.estudiante = crear_estudiante(self.docente)
        self.caso = crear_caso_con_rubrica(self.docente)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth = autorizar(self.practica, self.estudiante)
        self.client = cliente(token_estudiante(self.estudiante, self.auth))

    def test_rf12_iniciar_y_finalizar_guarda_tiempos(self):
        inicio = self.client.post('/api/participaciones/iniciar/')
        self.assertEqual(inicio.status_code, status.HTTP_200_OK)
        pid = inicio.data['id']
        self.assertEqual(inicio.data['estado'], Participacion.Estado.EN_CURSO)
        self.assertEqual(inicio.data['numero_intento'], 1)
        self.assertIsNotNone(inicio.data['inicio'])
        self.assertNotIn('es_correcta', str(inicio.data.get('caso', {})))

        fin = self.client.post(f'/api/participaciones/{pid}/finalizar/')
        self.assertEqual(fin.status_code, status.HTTP_200_OK)
        self.assertIn('resultado_id', fin.data)
        part = Participacion.objects.get(pk=pid)
        self.assertEqual(part.estado, Participacion.Estado.FINALIZADA)
        self.assertIsNotNone(part.fin)
        self.assertGreaterEqual(part.tiempo_usado_seg, 0)

    def test_rf12_no_finalizar_si_nunca_inicio(self):
        part = Participacion.objects.create(
            practica=self.practica,
            estudiante=self.estudiante,
            autorizacion=self.auth,
            numero_intento=1,
            estado=Participacion.Estado.NO_INICIADA,
        )
        resp = self.client.post(f'/api/participaciones/{part.id}/finalizar/')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        part.refresh_from_db()
        self.assertEqual(part.estado, Participacion.Estado.NO_INICIADA)

    def test_intentos_independientes_conservan_historial(self):
        r1 = self.client.post('/api/participaciones/iniciar/')
        pid1 = r1.data['id']
        self.client.post(
            f'/api/participaciones/{pid1}/responder/',
            {'pregunta_id': self.caso['p1'].id, 'respuesta_id': self.caso['r1_ok'].id},
            format='json',
        )
        self.client.post(f'/api/participaciones/{pid1}/finalizar/')
        self.auth.reintento_autorizado = True
        self.auth.save(update_fields=['reintento_autorizado'])

        r2 = self.client.post('/api/participaciones/iniciar/')
        pid2 = r2.data['id']
        self.assertNotEqual(pid1, pid2)
        self.assertEqual(r2.data['numero_intento'], 2)
        self.assertEqual(Participacion.objects.filter(autorizacion=self.auth).count(), 2)
        self.assertTrue(
            Participacion.objects.get(pk=pid1).respuestas_seleccionadas.filter(pregunta=self.caso['p1']).exists()
        )
        self.assertEqual(Participacion.objects.get(pk=pid2).respuestas_seleccionadas.count(), 0)

    def test_rn17_bloquea_inicio_tras_punto_medio(self):
        ahora = timezone.now()
        self.practica.fecha_inicio = ahora - timedelta(hours=4)
        self.practica.fecha_fin = ahora + timedelta(minutes=10)
        self.practica.save()
        resp = self.client.post('/api/participaciones/iniciar/')
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('mitad', str(resp.data).lower())

    def test_rf15_docente_lista_solo_sus_practicas(self):
        self.client.post('/api/participaciones/iniciar/')
        otro = crear_docente('docente2', 'docente2@uni.edu')
        cli_propio = cliente(token_usuario(self.docente))
        cli_ajeno = cliente(token_usuario(otro))
        propio = cli_propio.get('/api/participaciones/')
        ajeno = cli_ajeno.get('/api/participaciones/')
        self.assertEqual(propio.status_code, status.HTTP_200_OK)
        self.assertGreaterEqual(propio.data['count'], 1)
        self.assertEqual(ajeno.data['count'], 0)


class AccesoCruzadoParticipacionesTests(TestCase):
    def setUp(self):
        mail.outbox.clear()
        self.docente = crear_docente()
        self.est1 = crear_estudiante(self.docente, 'a@uni.edu', 'A', 'Uno', 'estA')
        self.est2 = crear_estudiante(self.docente, 'b@uni.edu', 'B', 'Dos', 'estB')
        self.caso = crear_caso_con_rubrica(self.docente)
        self.practica = crear_practica(self.docente, self.caso['caso'])
        self.auth1 = autorizar(self.practica, self.est1)
        self.auth2 = autorizar(self.practica, self.est2)

    def test_estudiante_no_consulta_participacion_ajena(self):
        c1 = cliente(token_estudiante(self.est1, self.auth1))
        pid = c1.post('/api/participaciones/iniciar/').data['id']
        c2 = cliente(token_estudiante(self.est2, self.auth2))
        resp = c2.get(f'/api/participaciones/{pid}/')
        self.assertEqual(resp.status_code, status.HTTP_404_NOT_FOUND)
